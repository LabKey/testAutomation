/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.io.File;

/**
 * User: elvan
 * Date: 8/16/12
 * Time: 11:44 AM
 */
public class PipelineHelper
{
    BaseWebDriverTest _test = null;

    public PipelineHelper(BaseWebDriverTest test)
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
        _test.click(l);
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
        _test.clickButton("Create Folder", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_APPEAR);
        _test.setFormElement(Locator.name("folderName"), folderName);
        _test.clickButton("Submit", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
        _test.waitForElement(Locator.css("#fileBrowser td.x-grid3-cell").withText(folderName));
    }

    public void addCreateFolderButton()
    {
        _test.dragAndDrop(Locator.xpath("//td[contains(@class, 'x-table-layout-cell')]//button[text()='Create Folder']"),
                         Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]"));
        _test.waitForElement(Locator.css(".test-custom-toolbar .iconFolderNew"), _test.defaultWaitForPage);
    }

    //TODO
    //won't work for adding and then removing something
    public void removeButton(String name)
    {
        _test.click(Locator.buttonContainingText(name).index(2));
        _test.click(Locator.tagContainingText("span", "remove"));
    }

    public void commitPipelineAdminChanges()
    {

        _test.clickButton("Submit", 0);
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
        _test.click(file);
        _test.click(Locator.xpath("//button[contains(@class,'iconMove')]"));
        _test._extHelper.waitForExtDialog("Choose Destination", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        //TODO:  this doesn't yet support nested folders
        Locator folder =Locator.xpath("(//a/span[contains(text(),'" + destinationPath + "')])[2]");
        _test.waitForElement(folder, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);  //if it still isn't coming up, that's a product bug
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css(".x-window ul.x-tree-node-ct").withText(destinationPath)));
        _test.click(folder);
        _test.clickButton("Move", BaseWebDriverTest.WAIT_FOR_EXT_MASK_TO_DISSAPEAR);
    }

    public void importFile(String fileName, String importAction)
    {
        _test._extHelper.clickFileBrowserFileCheckbox(fileName);
        _test.selectImportDataAction(importAction);
    }

    public static class Locators
    {
        public static Locator.XPathLocator pipelineStatusLink(int index)
        {
            return Locator.id("dataregion_StatusFiles").append("/tbody/tr/td[2]/a[starts-with(@href, '/labkey/pipeline-status/details.view?')]").index(index);
        }
    }
}
