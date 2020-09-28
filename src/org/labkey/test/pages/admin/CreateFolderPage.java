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
package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps createFolder.jsp, rendered by 'AdminController.CreateFolderAction'
 */
public class CreateFolderPage extends LabKeyPage
{
    public CreateFolderPage(WebDriver test)
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
        clickAndWait(newElementCache().nextButton);
        return new SetFolderPermissionsPage(getDriver());
    }

    public CreateFolderPage setName(String name)
    {
        setFormElement(newElementCache().nameInput, name);
        return this;
    }

    public CreateFolderPage selectFolderType(String folderType)
    {
        if (null == folderType || folderType.equals("None"))
        {
            folderType = "Custom";
        }

        WebElement folderTypeRadioButton =Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]")
                .waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        folderTypeRadioButton.click();

        if (folderType.equalsIgnoreCase("Custom"))
            waitFor(()-> Locator.tagWithText("div", "Choose Modules:").findElementOrNull(getDriver()) != null, 4000 );

        if (folderType.equalsIgnoreCase("Create From Template Folder"))
            waitFor(()-> Locator.input("templateSourceId").findElementOrNull(getDriver()) != null, 4000 );

         return this;
    }

    public CreateFolderPage createFromTemplateFolder(String templateFolder)
    {
        selectFolderType("Create From Template Folder");
        _ext4Helper.waitForMaskToDisappear();
        _ext4Helper.selectComboBoxItem(Locator.xpath("//div")
                .withClass("labkey-wizard-header")
                .withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
        _ext4Helper.checkCheckbox("Include Subfolders");

        return this;
    }

    public CreateFolderPage setTemplatePartCheckBox(String templatePart, boolean checked)
    {
        Checkbox checkbox = new Checkbox(Locator.xpath("//td[label[text()='"+templatePart+"']]/input").waitForElement(getDriver(), 4000));
        checkbox.set(checked);
        return this;
    }

    public CreateFolderPage addTabs(String[] tabsToAdd)
    {
        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                waitAndClick(Locator.xpath("//td[./label[text()='" + tabname + "']]/input[@type='button' and contains(@class, 'checkbox')]"));
        }
        return this;
    }

    public CreateFolderPage setTitle(String title)
    {
        setFormElement(newElementCache().titleInput, title);
        return this;
    }

    public CreateFolderPage setUseNameAsDisplayTitle()
    {
        _ext4Helper.checkCheckbox(Locator.ehrCheckboxWithLabel( "Use name as display title"));
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
        final WebElement titleInput = Locator.input("title").findWhenNeeded(this).withTimeout(4000);

        // TODO: Add support for "Use name as title" setting
//        final WebElement useNameAsDisplayTitleCheckBox =  Locator.ehrCheckboxWithLabel( "Use name as display title")
//                .findWhenNeeded(this).withTimeout(4000);
        // TODO: Add support for configuring the "Folder Type" settings

        // See AbstractContainerHelper.createSubfolder for what it supports and replace it


    }
}