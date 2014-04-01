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

import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;

public class WorkbookHelper extends AbstractHelper
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
     * @return workbook id
     */
    public int createWorkbook(String project, String title, String description, WorkbookFolderType folderType)
    {
        _test.clickProject(project);
        _test.clickButton("Insert New");

        _test.setFormElement(Locator.id("workbookTitle"), title);
        _test.setFormElement(Locator.id("workbookDescription"), description);
        _test.selectOptionByValue(Locator.id("workbookFolderType"), folderType.toString());

        _test.clickButton("Create Workbook");
        _test.waitForElement(Locator.css(".wb-name"));

        return getWorkbookIdFromUrl(_test.getURL()) ;
    }

    public int getWorkbookIdFromUrl(URL url)
    {
        // path is something like "http://localhost:8080/labkey/project/ContainerContextTest/2/begin.view?"
        // this code pulls "2" out by finding the last and second to last slashes
        try
        {
        String path = url.toURI().getPath();
        int idx = path.lastIndexOf("/");
        path = path.substring(0, idx);
        idx = path.lastIndexOf("/");
        return Integer.parseInt(path.substring(idx + 1));
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
    public int createFileWorkbook(String projectName, String title, String description)
    {
        // Create File Workbook
        int id = createWorkbook(projectName, title, description, WorkbookFolderType.FILE_WORKBOOK);
        _test.waitForElement(Locator.linkWithText("Files"));
        assertEquals(title, _test.getText(Locator.xpath("//span[preceding-sibling::span[contains(@class, 'wb-name')]]")));
        assertEquals(description, _test.getText(Locator.xpath("//div[@id='wb-description']")));
        _test.assertElementNotPresent(Locator.linkWithText(title)); // Should not appear in folder tree.
        return id;
    }

}
