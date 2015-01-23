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
package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

public abstract class WebPart
{
    protected BaseWebDriverTest _test;
    protected WebElement _componentElement;
    protected String _title;
    protected String _id;

    protected abstract void waitForReady();

    private void clearCachedTitle()
    {
        _title = null;
    }

    public String getCurrentTitle()
    {
        clearCachedTitle();
        return getTitle();
    }

    public abstract String getTitle();

    protected Elements elements()
    {
        return new Elements();
    }

    public abstract void delete();

    public abstract void moveUp();

    public abstract void moveDown();

    public void goToPermissions()
    {
        clickMenuItem("Permissions");
    }

    public void clickMenuItem(String... items)
    {
        clickMenuItem(true, items);
    }

    public void clickMenuItem(boolean wait, String... items)
    {
        _test._extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + _title.toLowerCase() + "']"), items);
    }

    protected class Elements
    {
        public Locator.XPathLocator webPart = Locator.tagWithId("table", _id);
        public Locator.XPathLocator webPartTitle = webPart.append(Locator.xpath("/tbody/tr/th"));
    }
}
