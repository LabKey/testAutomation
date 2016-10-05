/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.WebPart.Locators.*;

public class BodyWebPart<EC extends WebPart.ElementCache> extends WebPart<EC>
{
    public BodyWebPart(WebDriver driver, WebElement webPartElement)
    {
        super(driver, webPartElement);
        waitForReady();
    }

    public BodyWebPart(WebDriver test, String title, int index)
    {
        this(test, locator().withDescendant(leftTitle.withAttribute("title", title)).index(index).waitForElement(test, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
        _title = title;
    }

    public BodyWebPart(WebDriver test, String title)
    {
        this(test, title, 0);
    }

    public BodyWebPart(WebDriver test, int index)
    {
        this(test, locator().index(index).waitForElement(test, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
    }

    @Deprecated
    public BodyWebPart(BaseWebDriverTest test, String title, int index)
    {
        this(test.getDriver(), title, index);
    }

    @Deprecated
    public BodyWebPart(BaseWebDriverTest test, String title)
    {
        this(test.getDriver(), title, 0);
    }

    @Override
    protected void waitForReady() {}

    private static Locator.XPathLocator locator()
    {
        return Locator.id("bodypanel").append(webPart);
    }
}
