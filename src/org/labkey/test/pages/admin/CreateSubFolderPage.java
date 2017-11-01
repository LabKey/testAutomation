/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CreateSubFolderPage extends LabKeyPage
{
    public CreateSubFolderPage(WebDriver test)
    {
        super(test);
    }


    @Override
    public void waitForPage()
    {
        waitFor(()-> Locator.input("name").findElementOrNull(getDriver()) != null, WAIT_FOR_JAVASCRIPT);
    }

    public SetFolderPermissionsPage clickNext()
    {
        scrollIntoView(newElementCache().nextButton);
        newElementCache().nextButton.click();
        return new SetFolderPermissionsPage(getDriver());
    }

    public CreateSubFolderPage setFolderName(String name)
    {
        setFormElement(newElementCache().nameInput, name);
        return this;
    }

    public CreateSubFolderPage selectFolderType(String folderType)
    {
        WebElement folderTypeRadioButton =Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
         folderTypeRadioButton.click();

         if (folderType.equalsIgnoreCase("Custom"))
             waitFor(()-> Locator.tagWithText("div", "Choose Modules:").findElementOrNull(getDriver()) != null, 4000 );

        if (folderType.equalsIgnoreCase("Create From Template Folder"))
            waitFor(()-> Locator.input("templateSourceId").findElementOrNull(getDriver()) != null, 4000 );

         return this;
    }

    public CreateSubFolderPage createFromTemplateFolder(String templateFolder)
    {
        selectFolderType("Create From Template Folder");
        _ext4Helper.waitForMaskToDisappear();
        _ext4Helper.selectComboBoxItem(Locator.xpath("//div")
                .withClass("labkey-wizard-header")
                .withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
        _ext4Helper.checkCheckbox("Include Subfolders");

        return this;
    }

    public CreateSubFolderPage setTemplatePartCheckBox(String templatePart, boolean checked)
    {
        Checkbox checkbox = new Checkbox(Locator.xpath("//td[label[text()='"+templatePart+"']]/input").waitForElement(getDriver(), 4000));
        checkbox.set(checked);
        return this;
    }

    public CreateSubFolderPage addTabs(String[] tabsToAdd)
    {
        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                waitAndClick(Locator.xpath("//td[./label[text()='" + tabname + "']]/input[@type='button' and contains(@class, 'checkbox')]"));
        }
        return this;
    }



    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    private class Elements extends LabKeyPage.ElementCache
    {
        final WebElement nameInput = Locator.input("name").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        final WebElement nextButton = Locator.lkButton("Next").refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        // TODO: Add support for "Use name as title" setting
        // TODO: Add support for configuring the "Folder Type" settings

        // See AbstractContainerHelper.createSubfolder for what it supports and replace it


    }
}