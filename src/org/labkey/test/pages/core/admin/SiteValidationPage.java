/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SiteValidationPage extends BaseSettingsPage
{
    public SiteValidationPage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    @Override
    protected void waitForPage()
    {
        Locator.waitForAnyElement(shortWait(), Locator.lkButton("Validate"));
    }

    public static SiteValidationPage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", "configureSiteValidation"));
        return new SiteValidationPage(wrapper.getDriver());
    }


    public void setWholeSite(boolean wholeSite)
    {
        RadioButton button;

        if (wholeSite)
            button = new RadioButton(elementCache().wholeSiteRadio);
        else
            button = new RadioButton(elementCache().projectsOnlyRadio);

        button.check();
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

    public void setAllValidators(boolean enabled)
    {
        WebElement formEl = Locator.id("form").findElement(getDriver());
        // Enable all validators
        Locator.tagWithAttribute("input", "name", "provider")
                .findElements(formEl).forEach(x -> setCheckbox(x, enabled));
    }

    public void setBackground(boolean background)
    {
        // Run in background
        checkCheckbox(Locator.id("background"));
        new Checkbox(elementCache().backgroundCheckbox).set(background);
    }

    public PipelineStatusDetailsPage clickValidateInBackground()
    {
        setBackground(true);
        clickAndWait(elementCache().validateButton);
        return new PipelineStatusDetailsPage(getDriver());
    }

    public void setWikiValidator(boolean checked)
    {
        new Checkbox(elementCache().wikiCheckbox).set(checked);
    }

    protected class ElementCache extends BaseSettingsPage.ElementCache
    {
        WebElement displayFormatCheckbox = Locator.xpath("//input[@name='providers' and @value='Display Format Validator']").findWhenNeeded(this);
        WebElement permissionsCheckbox = Locator.xpath("//input[@name='providers' and @value='Permissions Validator']").findWhenNeeded(this);
        WebElement pipelineCheckbox = Locator.xpath("//input[@name='providers' and @value='Pipeline Validator']").findWhenNeeded(this);
        WebElement fileRootSizeCheckbox = Locator.xpath("//input[@name='providers' and @value='File Root Size']").findWhenNeeded(this);
        WebElement wikiCheckbox = Locator.xpath("//input[@name='providers' and @value='Wiki Validator']").findWhenNeeded(this);

        WebElement wholeSiteRadio = Locator.xpath("//input[@name='includeSubfolders' and @value='true']").findWhenNeeded(this);
        WebElement projectsOnlyRadio = Locator.xpath("//input[@name='includeSubfolders' and @value='false']").findWhenNeeded(this);

        WebElement backgroundCheckbox = Locator.xpath("//input[@name='background']").findWhenNeeded(this);

        WebElement validateButton = Locator.lkButton("Validate").findWhenNeeded(this);
    }

}
