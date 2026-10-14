/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.tests.devtools;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.GetCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DRT;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.core.webdav.WebDavUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * We expect {@code response.sendError(statusCode, message)} to send the provided message in the response, but this
 * wasn't happening by default on embedded Tomcat distributions. This simple test verifies the fix. See Issue 51110.
 */
@Category({DRT.class, Daily.class})
public class ErrorResponseTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("DeveloperTools");
    }

    @Test
    public void testSendError() throws IOException
    {
        SendErrorCommand sendErrorCommand = new SendErrorCommand();
        final CommandResponse response;
        try
        {
            response = sendErrorCommand.execute(WebTestHelper.getRemoteApiConnection(), "/");
            fail("Expected execute() to throw a CommandException but succeeded with: " + response.getText());
        }
        catch (CommandException e)
        {
            checker().verifyThat("Response status code", e.getStatusCode(), equalTo(400));
            checker().verifyThat("Response text", e.getResponseText(), containsString("This is a very bad request!"));
        }
    }

    private static class SendErrorCommand extends GetCommand<CommandResponse>
    {
        protected SendErrorCommand()
        {
            super("test", "sendError");
        }
    }

    @Test
    public void testUnparseableParameters() throws Exception
    {
        String beginUrl = WebTestHelper.buildURL("project", getProjectName(), "begin");
        assertGetResponse(HttpStatus.SC_OK, beginUrl);
        assertGetResponse(HttpStatus.SC_BAD_REQUEST, beginUrl + "?x=%C3%28");
        assertGetResponse(HttpStatus.SC_BAD_REQUEST, beginUrl + "?x=%uf");
        assertGetResponse(HttpStatus.SC_BAD_REQUEST, beginUrl + "?=foo");
        // The 404 page reads the request parameters
        assertGetResponse(HttpStatus.SC_BAD_REQUEST, WebTestHelper.buildURL("project", getProjectName(), "noSuchAction") + "?x=%uf");
    }

    @Test
    public void testMalformedMultipart() throws Exception
    {
        String beginUrl = WebTestHelper.buildURL("project", getProjectName(), "begin");
        String truncated = "--XYZ\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nvalue";

        assertPostResponse(HttpStatus.SC_BAD_REQUEST, beginUrl, "multipart/form-data; boundary=XYZ", truncated);
        assertPostResponse(HttpStatus.SC_BAD_REQUEST, beginUrl, "multipart/form-data", "junk");
        assertPostResponse(HttpStatus.SC_BAD_REQUEST, WebDavUtils.buildBaseWebDavUrl(getProjectName(), "@files/"), "multipart/form-data; boundary=XYZ", truncated);
    }

    private void assertGetResponse(int expectedStatus, String url) throws IOException
    {
        SimpleHttpResponse response = new SimpleHttpRequest(url).getResponse();
        assertEquals("Wrong response for " + url, expectedStatus, response.getResponseCode());
    }

    private void assertPostResponse(int expectedStatus, String url, String contentType, String body) throws IOException
    {
        HttpPost post = new HttpPost(url);
        post.setEntity(new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8), ContentType.parse(contentType)));
        try (CloseableHttpClient client = WebTestHelper.getHttpClient())
        {
            int status = client.execute(post, ClassicHttpResponse::getCode);
            assertEquals("Wrong response for POST of " + contentType + " to " + url, expectedStatus, status);
        }
    }
}
