/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.pages.test;

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
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
        configurationException("org.labkey.api.util.ConfigurationException"),
        illegalState("java.lang.IllegalStateException"),
        multiException(null),
        notFound(HttpStatus.SC_NOT_FOUND),
        npe("java.lang.NullPointerException"),
        npeother("java.lang.NullPointerException"),
        unauthorized(HttpStatus.SC_FORBIDDEN);

        final String exceptionClass;
        final int response;

        ExceptionActions(String exceptionClass)
        {
            this.response = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            this.exceptionClass = exceptionClass;
        }

        ExceptionActions(int response)
        {
            this.response = response;
            this.exceptionClass = null;
        }

        public void triggerException(String message)
        {
            SimpleHttpResponse httpResponse = WebTestHelper.getHttpResponse(getUrl(message));
            assertEquals(httpResponse.getResponseMessage(), response, httpResponse.getResponseCode());
        }

        public void beginAt(WebDriverWrapper driverWrapper)
        {
            driverWrapper.beginAt(getUrl(null));
        }

        public String getExceptionClass()
        {
            return exceptionClass;
        }

        @NotNull
        private String getUrl(String message)
        {
            return WebTestHelper.buildURL("test", toString(), message == null ? Collections.emptyMap() : Maps.of("message", message));
        }

        public void triggerException()
        {
            triggerException(null);
        }
    }
}