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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.net.URISyntaxException;

/**
 * User: tchadick
 * Date: 12/18/12
 * Time: 11:56 AM
 */
public class WorkbookHelper extends AbstractHelperWD
{
    public WorkbookHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    /**
     * Create a new workbook in a project containing a workbook webpart on the portal page
     * @param project Name of parent project
     * @param title Title of created workbook
     * @param description Description of created workbook
     * @param folderType Type of created workbook
     */
    public String createWorkbook(String project, String title, String description, WorkbookFolderType folderType)
    {
        _test.clickFolder(project);
        _test.clickButton("Insert New");

        _test.setFormElement(Locator.id("workbookTitle"), title);
        _test.setFormElement(Locator.id("workbookDescription"), description);
        _test.selectOptionByValue(Locator.id("workbookFolderType"), folderType.toString());

        _test.clickButton("Create Workbook");
        _test.waitForElement(Locator.css(".wb-name"));

        try
        {
            String path = _test.getURL().toURI().getPath();
            path = path.replaceAll(".*/workbook-", "");
            path = path.replaceAll("/begin.view", "");
            return path;
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }



    public enum WorkbookFolderType
    {
        ASSAY_WORKBOOK("Assay Test Workbook"),
        FILE_WORKBOOK("File Test Workbook"),
        DEFAULT_WORKBOOK("Workbook");

        private final String _type;

        WorkbookFolderType(String type)
        {
            this._type = type;
        }

        @Override
        public String toString()
        {
            return _type;
        }
    }

    /**
     * Create and verify file workbook
     * @param projectName Name of parent project
     * @param title Title of created workbook
     * @param description Description of created workbook
     */
    public void createFileWorkbook(String projectName, String title, String description)
    {
        // Create File Workbook
        createWorkbook(projectName, title, description, WorkbookFolderType.FILE_WORKBOOK);
        _test.waitForElement(Locator.linkWithText("Files"));
        Assert.assertEquals(title, _test.getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        Assert.assertEquals(description, _test.getText(Locator.xpath("//div[@id='wb-description']")));
        _test.assertLinkNotPresentWithText(title); // Should not appear in folder tree.
    }

}
