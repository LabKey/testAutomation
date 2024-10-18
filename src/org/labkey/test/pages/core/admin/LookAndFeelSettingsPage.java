/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelSettingsPage extends BaseSettingsPage
{
    public LookAndFeelSettingsPage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    public static LookAndFeelSettingsPage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", "lookAndFeelSettings"));
        return new LookAndFeelSettingsPage(wrapper.getDriver());
    }

    public boolean isUsDateParsingModeChecked()
    {
        return new RadioButton(elementCache().usDateParsingRadio).isChecked();
    }

    public boolean isNonUsDateParsingModeChecked()
    {
        return new RadioButton(elementCache().nonUSDateParsingRadio).isChecked();
    }

    public void setDateParsingMode(boolean useUSDateFormat)
    {
        RadioButton button;

        if (useUSDateFormat)
            button = new RadioButton(elementCache().usDateParsingRadio);
        else
            button = new RadioButton(elementCache().nonUSDateParsingRadio);

        button.check();
    }

    public String getAltWelcomePage()
    {
        return getFormElement(elementCache().altWelcomePageTxt);
    }

    public void setAltWelcomePage(String welcomePage)
    {
        setFormElement(elementCache().altWelcomePageTxt, welcomePage);
    }

    @Override
    public void reset()
    {
        doAndWaitForPageToLoad(()->
        {
            elementCache().resetBtn.click();
            acceptAlert();
        });

    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseSettingsPage.ElementCache
    {
        WebElement usDateParsingRadio = Locator.xpath("//input[@name='dateParsingMode' and @value='US']").findWhenNeeded(this);
        WebElement nonUSDateParsingRadio = Locator.xpath("//input[@name='dateParsingMode' and @value='NON_US']").findWhenNeeded(this);
        WebElement altWelcomePageTxt = Locator.inputById("customWelcome").findWhenNeeded(this);
        WebElement resetBtn = Locator.lkButton("Reset to Defaults").findWhenNeeded(this);

    }

}
