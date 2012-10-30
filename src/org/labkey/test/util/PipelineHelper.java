/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 8/16/12
 * Time: 11:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class PipelineHelper
{
    BaseSeleniumWebTest _test = null;
    private static final String CUSTOM_PROPERTY = "customProperty";

    public PipelineHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    /**
     * from the pipeline webpart, in the current directory, rename a file
     * @param currentName current file name
     * @param newName new file name
     */
    public void renameFile(String currentName, String newName)
    {
        Locator l = Locator.xpath("//div[text()='" + currentName + "']");
        _test.clickAt(l, "1,1");
        _test.click(Locator.css("button.iconRename"));

        _test.waitForDraggableMask();
        _test._extHelper.setExtFormElementByLabel("Filename:", newName);
        Locator btnLocator = Locator.extButton("Rename");
        _test.click(btnLocator);
        _test.waitForText(newName);
    }

    /**
     * from the pipeline webpart, create a new folder.  Currently only supports creation in current view, this
     * could be upgraded
     * @param folderName name of folder to create.
     */
    public void createFolder(String folderName)
    {
        _test.clickButton("Create Folder", _test.WAIT_FOR_EXT_MASK_TO_APPEAR);
        _test.setFormElement("folderName", folderName);
        _test.clickButton("Submit", BaseSeleniumWebTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForText(folderName);
    }

    public void goToConfigureTab()
    {

    }
    public void addCreateFolderButton()
    {
        _test.dragAndDrop(Locator.xpath("//td[contains(@class, 'x-table-layout-cell')]//button[text()='Create Folder']"),
                         Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]"));
        _test.waitForElement(Locator.xpath("(//button[contains(@class, 'iconFolderNew')])[2]"), _test.defaultWaitForPage);
    }

    //TODO
    //won't work for adding and then removing something
    public void removeButton(String name)
    {
            _test.click(Locator.xpath("(" + Locator.buttonContainingText(name).toXpath() + ")[3]"));
            _test.click(Locator.tagContainingText("span", "remove"));
    }

    public void commitPipelineAdminChanges()
    {

        _test.clickButton("Submit", 0);
    }

    public void goToConfigureButtonsTab()
    {
        goToAdminMenu();

            _test._extHelper.clickExtTab("Toolbar and Grid Settings");
            _test.waitForText("Configure Grid columns and Toolbar");
    }

    public void goToAdminMenu()
    {
        _test.clickButton("Admin", _test.WAIT_FOR_EXT_MASK_TO_APPEAR);
        _test._extHelper.waitForExtDialog("Manage File Browser Configuration", 5000);
    }

    /**
     * From pipeline screen, move a file to a folder
     * @param fileName  Name of file to move
     * @param destinationPath path of folder to move file to.  Currently supports only folders in the same directory,
     * this can be expanded when necessary.
     */
    public void moveFile(String fileName, String destinationPath)
    {
        Locator file =  Locator.tagContainingText("div",fileName);
        _test.waitForElement(file);
        _test.clickAt(file, "1,1");
        _test.click(Locator.xpath("//button[contains(@class,'iconMove')]"));
        _test.waitForExtMask();
        //TODO:  this doesn't yet support nested folders
        Locator folder =Locator.xpath("(//a/span[contains(text(),'" + destinationPath + "')])[2]");
        _test.waitForElement(folder, 3*_test.defaultWaitForPage);  //if it still isn't coming up, that's a product bug
        _test.clickAt(folder, "1,1");
        _test.clickButton("Move", _test.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    public void uploadFile(File file, String description, String customProperty, String lookupColumn )
    {

            _test.setFormElement(Locator.xpath("//input[contains(@class, 'x-form-file') and @type='file']"), file.toString());
            _test._extHelper.setExtFormElementByLabel("Description:", description);
            _test.clickButton("Upload", 0);
            _test._extHelper.waitForExtDialog("Extended File Properties", _test.WAIT_FOR_JAVASCRIPT);
            _test.setFormElement(CUSTOM_PROPERTY, customProperty);
            _test._extHelper.selectComboBoxItem("LookupColumn:",lookupColumn);
            _test.clickButton("Done", 0);
            _test.waitForExtMaskToDisappear();


            _test.waitForText(description, _test.WAIT_FOR_JAVASCRIPT);
            _test.waitForText(customProperty, _test.WAIT_FOR_JAVASCRIPT);
            _test.waitForText(lookupColumn, _test.WAIT_FOR_JAVASCRIPT);
    }


    public void importFile(String fileName, String importAction)
    {
            _test._extHelper.clickFileBrowserFileCheckbox(fileName);
            _test.selectImportDataAction(importAction);
    }
}
