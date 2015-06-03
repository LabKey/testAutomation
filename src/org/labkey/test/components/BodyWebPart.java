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
import org.openqa.selenium.WebElement;

import java.util.List;

public class BodyWebPart extends WebPart
{
    public BodyWebPart(BaseWebDriverTest test, WebElement webPartElement)
    {
        _test = test;
        _componentElement = webPartElement;
        _id = webPartElement.getAttribute("id");
        waitForReady();
    }

    public BodyWebPart(BaseWebDriverTest test, String title, int index)
    {
        _test = test;
        _title = title;
        List<WebElement> webparts = PortalHelper.Locators.webPart(title).findElements(test.getDriver());
        _componentElement = webparts.get(index);
        _id = _componentElement.getAttribute("id");
        waitForReady();
    }

    public BodyWebPart(BaseWebDriverTest test, String title)
    {
        this(test, title, 0);
    }

    public BodyWebPart(BaseWebDriverTest test, int index)
    {
        _test = test;
        List<WebElement> webparts = PortalHelper.Locators.webPart.findElements(test.getDriver());
        _id = webparts.get(index).getAttribute("id");
        waitForReady();
    }

    @Override
    protected void waitForReady() {}

    @Override
    public String getTitle()
    {
        if (_title == null)
            _title = elements().webPartTitle.findElement(_test.getDriver()).getAttribute("title");
        return _title;
    }

    @Override
    public void delete()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.removeWebPart(getTitle());
    }

    @Override
    public void moveUp()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.UP);
    }

    @Override
    public void moveDown()
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.moveWebPart(getTitle(), PortalHelper.Direction.DOWN);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BodyWebPart webPart = (BodyWebPart) o;

        if (!_id.equals(webPart._id)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return _id.hashCode();
    }

    public static Locator locator()
    {
        return Locator.css(".labkey-side-panel > table[name=webpart]");
    }
}
