/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.pages.wiki;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.wiki.WikiFilePicker;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Collections;

import static org.labkey.test.components.html.EnumSelect.EnumSelect;
import static org.labkey.test.util.WikiHelper.WikiRendererType;

/**
 * TODO: Very incomplete
 */
public class EditPage extends LabKeyPage<EditPage.ElementCache>
{
    public EditPage(WebDriver driver)
    {
        super(driver);
    }

    public static EditPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static EditPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("wiki", containerPath, "edit"));
        return new EditPage(driver.getDriver());
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     */
    public EditPage convertWikiFormat(WikiRendererType format)
    {
        if (getWikiFormat() == format)
            return this;

        elementCache().convertButton.click();

        ChangeFormatWindow changeFormatWindow = new ChangeFormatWindow();
        changeFormatWindow.format().set(format);
        changeFormatWindow.clickConvert();
        WebElement status = waitForElement(Locator.id("status").containing("Converted."));
        shortWait().until(ExpectedConditions.invisibilityOfAllElements(Collections.singletonList(status)));

        clearCache(); // Just to be on the safe side
        return this;
    }

    public WikiRendererType getWikiFormat()
    {
        return WikiRendererType.valueOf((String) executeScript("return LABKEY._wiki.getProps().rendererType;"));
    }

    public void saveAndClose()
    {
        clickAndWait(elementCache().saveAndCloseButton);
    }

    public EditPage setName(String name)
    {
        elementCache().nameInput.set(name);
        return this;
    }

    public EditPage setTitle(String title)
    {
        elementCache().titleInput.set(title);
        return this;
    }

    public EditPage setBody(String body)
    {
        elementCache().bodyTextArea.set(body);
        return this;
    }

    public EditPage setShouldIndex(boolean shouldIndex)
    {
        elementCache().shouldIndexCheckbox.set(shouldIndex);
        return this;
    }

    public EditPage addAttachment(File file)
    {
        getFilePicker().addAttachment(file);
        return this;
    }

    public WikiFilePicker getFilePicker()
    {
        return elementCache().filePicker;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement saveAndCloseButton = Locator.lkButton("Save & Close").findWhenNeeded(this);
        WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement convertButton = Locator.lkButton("Convert To...").findWhenNeeded(this);
        Input nameInput = Input.Input(Locator.name("name"), getDriver()).findWhenNeeded(this);
        Input titleInput = Input.Input(Locator.name("title"), getDriver()).findWhenNeeded(this);
        Input bodyTextArea = Input.Input(Locator.name("body"), getDriver()).findWhenNeeded(this);
        Checkbox shouldIndexCheckbox = Checkbox.Checkbox(Locator.checkboxByName("shouldIndex")).findWhenNeeded(this);
        WikiFilePicker filePicker = new WikiFilePicker(getDriver());
    }

    public class ChangeFormatWindow extends Window
    {
        private final EnumSelect<WikiRendererType> _formatSelect;

        public ChangeFormatWindow()
        {
            super("Change Format", EditPage.this.getDriver());
            _formatSelect = EnumSelect(Locator.id("wiki-input-window-change-format-to"), WikiRendererType.class).waitFor(this);
        }

        public EnumSelect<WikiRendererType> format()
        {
            return _formatSelect;
        }

        public void clickConvert()
        {
            clickButton("Convert", true);
        }

        public void clickCancel()
        {
            clickButton("Cancel", true);
        }
    }
}