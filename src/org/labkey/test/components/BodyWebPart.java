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
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BodyWebPart extends WebPart
{
    public BodyWebPart(WebDriver driver, WebElement webPartElement)
    {
        super(driver, webPartElement);
    }

    public BodyWebPart(WebDriver test, String title, int index)
    {
        this(test, PortalHelper.Locators.webPart(title).index(index).waitForElement(test, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
        _title = title;
    }

    public BodyWebPart(WebDriver test, String title)
    {
        this(test, title, 0);
    }

    public BodyWebPart(WebDriver test, int index)
    {
        this(test, PortalHelper.Locators.webPart.index(index).waitForElement(test, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
    }

    @Deprecated
    public BodyWebPart(BaseWebDriverTest test, String title, int index)
    {
        super(test, PortalHelper.Locators.webPart(title).index(index).waitForElement(test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT));
        _title = title;
    }

    @Deprecated
    public BodyWebPart(BaseWebDriverTest test, String title)
    {
        this(test, title, 0);
    }

    @Override
    protected void waitForReady() {}

    @Override
    public String getTitle()
    {
        if (_title == null)
            _title = elements().webPartTitle.getAttribute("title");
        return _title;
    }

    @Override
    public void delete()
    {
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.removeWebPart(getTitle());
    }

    @Override
    public void moveUp()
    {
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.UP);
    }

    @Override
    public void moveDown()
    {
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.DOWN);
    }

    public static Locator locator()
    {
        return Locator.css(".labkey-side-panel > table[name=webpart]");
    }
}
