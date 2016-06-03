package org.labkey.test.pages.test;

import org.apache.http.HttpStatus;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.labkey.test.util.SimpleHttpResponse;
import org.openqa.selenium.WebDriver;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestActions extends LabKeyPage
{
    public TestActions(WebDriver driver)
    {
        super(driver);
    }

    public static TestActions beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, null);
    }

    public static TestActions beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(Actions.begin.buildURL(containerPath));
        return new TestActions(driver.getDriver());
    }

    public LabKeyPage clickAction(Actions action)
    {
        clickAndWait(Locator.linkWithText(action.toString()));
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage clickAction(ExceptionActions action)
    {
        clickAndWait(Locator.linkWithText(action.toString()));
        return new LabKeyPage(getDriver());
    }

    /**
     * TestController
     */
    public enum Actions
    {
        begin,
        button,
        clearLeaks,
        complexForm,
        htmlView,
        leak,
        multipartForm,
        permAdmin,
        permDelete,
        permInsert,
        permNone,
        permRead,
        permUpdate,
        simpleForm,
        tags,
        test;

        public String buildURL()
        {
            return WebTestHelper.buildURL("test", toString());
        }

        public String buildURL(String container)
        {
            return WebTestHelper.buildURL("test", container, toString());
        }
    }

    /**
     * TestController, actions that trigger exceptions
     */
    public enum ExceptionActions
    {
        configurationException,
        illegalState,
        multiException,
        notFound(HttpStatus.SC_NOT_FOUND),
        npe,
        npeother,
        unauthorized(HttpStatus.SC_FORBIDDEN);

        final int response;

        ExceptionActions()
        {
            response = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        }

        ExceptionActions(int response)
        {
            this.response = response;
        }

        public void triggerException(String message)
        {
            SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(WebTestHelper.buildURL("test", toString(), message == null ? Collections.emptyMap() : Maps.of("message", message)));
            assertEquals(httpResponse.getResponseMessage(), response, httpResponse.getResponseCode());
        }

        public void triggerException()
        {
            triggerException(null);
        }
    }
}