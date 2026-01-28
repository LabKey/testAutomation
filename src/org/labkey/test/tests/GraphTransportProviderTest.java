/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.util.GraphTransportProvider;
import org.mockserver.integration.ClientAndServer;

import java.util.Properties;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Tests for {@link GraphTransportProvider} using MockServer to simulate
 * Azure AD token endpoint and Microsoft Graph sendMail endpoint.
 *
 * Mocked endpoints:
 *   POST /{tenantId}/oauth2/v2.0/token — OAuth2 client credentials token acquisition
 *   POST /v1.0/users/{fromAddress}/sendMail — Graph API email send
 */
public class GraphTransportProviderTest extends Assert
{
    private ClientAndServer mockServer;

    // Test-generated fake values — MockServer doesn't validate these
    private static final String TEST_TENANT_ID = "test-tenant-id";
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_FROM_ADDRESS = "sender@example.com";
    private static final String TEST_TO_ADDRESS = "recipient@example.com";
    private static final String TEST_ACCESS_TOKEN = "mock-access-token-12345";

    private static final String TOKEN_PATH = "/" + TEST_TENANT_ID + "/oauth2/v2.0/token";
    private static final String SEND_MAIL_PATH = "/v1.0/users/" + TEST_FROM_ADDRESS + "/sendMail";

    private static final String TOKEN_RESPONSE_BODY = """
            {
                "token_type": "Bearer",
                "expires_in": 3600,
                "access_token": "%s"
            }
            """.formatted(TEST_ACCESS_TOKEN);

    @Before
    public void setUp()
    {
        mockServer = ClientAndServer.startClientAndServer(0); // dynamic port
    }

    @After
    public void tearDown()
    {
        if (mockServer != null && mockServer.isRunning())
        {
            mockServer.stop();
        }
    }

    @Test
    public void testSuccessfulEmailSend() throws MessagingException
    {
        // Stub token endpoint
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH)
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TOKEN_RESPONSE_BODY)
        );

        // Stub sendMail endpoint — Graph API returns 202 Accepted
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(SEND_MAIL_PATH)
                .withHeader("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
        ).respond(
            response()
                .withStatusCode(202)
        );

        GraphTransportProvider provider = createTestProvider();
        provider.send(createTestMessage());

        // Verify both endpoints were called
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH)
        );
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath(SEND_MAIL_PATH)
                .withHeader("Authorization", "Bearer " + TEST_ACCESS_TOKEN)
        );
    }

    @Test
    public void testTokenAcquisitionFailure()
    {
        // Stub token endpoint returning 401
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH)
        ).respond(
            response()
                .withStatusCode(401)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "invalid_client",
                        "error_description": "Invalid client credentials"
                    }
                    """)
        );

        GraphTransportProvider provider = createTestProvider();

        MessagingException e = assertThrows(MessagingException.class,
            () -> provider.send(createTestMessage()));
        assertTrue("Exception should indicate token failure",
            e.getMessage().contains("Failed sending mail via Microsoft Graph"));
    }

    @Test
    public void testSendMailFailure() throws MessagingException
    {
        // Stub successful token acquisition
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH)
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TOKEN_RESPONSE_BODY)
        );

        // Stub sendMail endpoint returning 400
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(SEND_MAIL_PATH)
        ).respond(
            response()
                .withStatusCode(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": {
                            "code": "BadRequest",
                            "message": "Invalid recipient address"
                        }
                    }
                    """)
        );

        GraphTransportProvider provider = createTestProvider();

        MessagingException e = assertThrows(MessagingException.class,
            () -> provider.send(createTestMessage()));
        assertTrue("Exception should indicate Graph sendMail failure",
            e.getMessage().contains("Failed sending mail via Microsoft Graph"));
    }

    @Test
    public void testTokenCaching() throws MessagingException
    {
        // Stub token endpoint
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH)
        ).respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TOKEN_RESPONSE_BODY)
        );

        // Stub sendMail endpoint
        mockServer.when(
            request()
                .withMethod("POST")
                .withPath(SEND_MAIL_PATH)
        ).respond(
            response()
                .withStatusCode(202)
        );

        GraphTransportProvider provider = createTestProvider();

        // Send two emails with the same provider instance
        provider.send(createTestMessage());
        provider.send(createTestMessage());

        // Token endpoint should be called only once due to caching
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath(TOKEN_PATH),
            org.mockserver.verify.VerificationTimes.exactly(1)
        );

        // sendMail should be called twice
        mockServer.verify(
            request()
                .withMethod("POST")
                .withPath(SEND_MAIL_PATH),
            org.mockserver.verify.VerificationTimes.exactly(2)
        );
    }

    /**
     * Creates a GraphTransportProvider with base URLs pointing to MockServer
     * and test credentials pre-configured.
     */
    private GraphTransportProvider createTestProvider()
    {
        GraphTransportProvider provider = new GraphTransportProvider()
        {
            @Override
            protected String getOAuth2LoginBaseUrl()
            {
                return getMockBaseUrl();
            }

            @Override
            protected String getGraphBaseUrl()
            {
                return getMockBaseUrl();
            }
        };

        // Configure test credentials
        Properties props = provider.getProperties();
        props.put("mail.graph.tenantId", TEST_TENANT_ID);
        props.put("mail.graph.clientId", TEST_CLIENT_ID);
        props.put("mail.graph.clientSecret", TEST_CLIENT_SECRET);
        props.put("mail.graph.fromAddress", TEST_FROM_ADDRESS);

        return provider;
    }

    /**
     * Creates a simple test MimeMessage.
     */
    private MimeMessage createTestMessage() throws MessagingException
    {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(TEST_FROM_ADDRESS));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TEST_TO_ADDRESS));
        message.setSubject("Test email from GraphTransportProviderTest");
        message.setText("This is a test email body.");
        return message;
    }

    private String getMockBaseUrl()
    {
        return "http://localhost:" + mockServer.getLocalPort();
    }
}
