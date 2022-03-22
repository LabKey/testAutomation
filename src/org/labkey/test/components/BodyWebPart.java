/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BodyWebPart<EC extends BodyWebPart.ElementCache> extends WebPart<EC>
{
    public BodyWebPart(WebDriver driver, WebElement webPartElement)
    {
        super(driver, webPartElement);
        waitForReady();
    }

    public BodyWebPart(WebDriver driver, String title, int index)
    {
        this(driver, find(driver, title, index));
        _title = title;
    }

    public BodyWebPart(WebDriver driver, String title)
    {
        this(driver, title, 0);
    }

    public BodyWebPart(WebDriver driver, int index)
    {
        this(driver, webPartLoc().index(index).waitForElement(driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
    }

    static public WebElement find(WebDriver driver, String title, int index)
    {
        return webPartLoc(title).index(index).waitForElement(driver, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    public class ElementCache extends WebPart.ElementCache {}
}
