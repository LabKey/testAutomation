/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * This class should, eventually, contain most basic page interaction functionality
 * {@link org.labkey.test.LabKeySiteWrapper} currently does this
 */
public class LabKeyPage extends WebDriverWrapper
{
    @Deprecated
    protected BaseWebDriverTest _test;
    private WebDriver _driver;

    /**
     * @deprecated Use {@link LabKeyPage(WebDriver)}
     */
    @Deprecated
    public LabKeyPage(WebDriverWrapper test)
    {
        if (test instanceof BaseWebDriverTest)
            _test = (BaseWebDriverTest)test;
        _driver = test.getDriver();
        waitForPage();
    }

    public LabKeyPage(WebDriver driver)
    {
        _driver = driver;
        waitForPage();
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driver;
    }

    protected void waitForPage() {}

    public static class Locators extends org.labkey.test.Locators
    {
        public static Locator.XPathLocator bodyPanel = Locator.id("bodypanel");
    }
}
