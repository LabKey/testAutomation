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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class WebPart extends Component
{
    protected final WebDriverWrapper _test;

    protected final WebElement _componentElement;
    protected String _title;

    public WebPart(WebDriver driver, WebElement componentElement)
    {
        this(new WebDriverWrapperImpl(driver), componentElement);
    }

    public WebPart(WebDriverWrapper test, WebElement componentElement)
    {
        _componentElement = componentElement;
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public WebDriver getDriver()
    {
        return _test.getDriver();
    }

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

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement webPartTitle = new LazyWebElement(Locator.xpath("tbody/tr/th"), this);
    }
}
