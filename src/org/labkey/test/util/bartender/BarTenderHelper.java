package org.labkey.test.util.bartender;

import org.labkey.test.util.TestLogger;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpClassCallback;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.function.Consumer;

import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;

public class BarTenderHelper
{
    protected static ClientAndServer mockServer = null;

    private static final String MOCKSERVER_CALL_MATCHER_CLASS = "org.labkey.test.bartender.BarTenderMockPostCallback";

    public static final String SUCCESS_LABEL = "success";
    public static final String LABEL_NOT_FOUND = "labelnotfound";
    public static final String DEFAULT_LABEL = "\u2603new Default Label"; // Include tricky (snowman) character

    public static final String DOSENT_EXIST_PATH = "doesnotexist";
    public static final String MOCK_SERVER_CONTEXT_PATH = "mockBarTender";
    public static final String ALTERNATE_SUCCESS_PATH = "alternate";

    public void initMockserver()
    {
        if((null == mockServer) || (!mockServer.isRunning()))
            mockServer = ClientAndServer.startClientAndServer();

        int count = 1;

        while(!mockServer.isRunning() & count < 10)
        {
            TestLogger.log("Waiting for mockServer to start.");
            count++;
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        mockServer.reset();

        if(mockServer.isRunning()) {
            //BarTender path is fairly arbitrary relative to the implementation. So we will use it for returning different mock BT responses
            addRequestMatcher(mockServer, String.join("/", MOCK_SERVER_CONTEXT_PATH, DOSENT_EXIST_PATH), TestLogger::log,"POST", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", MOCK_SERVER_CONTEXT_PATH), TestLogger::log,"POST", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", MOCK_SERVER_CONTEXT_PATH, ALTERNATE_SUCCESS_PATH), TestLogger::log,"POST", MOCKSERVER_CALL_MATCHER_CLASS);
            addRequestMatcher(mockServer, String.join("/", MOCK_SERVER_CONTEXT_PATH, ALTERNATE_SUCCESS_PATH), TestLogger::log,"OPTIONS", MOCKSERVER_CALL_MATCHER_CLASS);
        }
        else {
            fail("Mockserver is not running and could not be initialized.");
        }
    }

    /**
     * Adds a Request matcher to the mockserver
     * @param mockServer to add matcher to
     * @param requestPath to add matcher for
     * @param log logging method
     * @param method HTTP request type, e.g., GET, POST, etc.
     * @param matcher Fully qualified class name String to request handler that implements ExpectationResponseCallback
     */
    public static void addRequestMatcher(ClientAndServer mockServer, String requestPath, Consumer<String> log, String method, String matcher )
    {
        log.accept(String.format("Adding a response for %1$s requests.", requestPath));
        mockServer.when(
                request()
                        .withMethod(method)
                        .withPath("/" + requestPath)
        ).respond(HttpClassCallback.callback(matcher));
    }

    public String getMockServerHost()
    {
        return "http://localhost:" + mockServer.getLocalPort() + "/" + MOCK_SERVER_CONTEXT_PATH;
    }

    public static void cleanup()
    {
        if(null != mockServer)
        {
            TestLogger.log("Stopping the mockserver.");
            mockServer.stop();

            TestLogger.log("Waiting for the mockserver to stop.");
            new FluentWait<>(mockServer).withMessage("waiting for the mockserver to stop.").until(mockServer -> !mockServer.isRunning());
            TestLogger.log("The mockserver is stopped.");
        }
    }
}
