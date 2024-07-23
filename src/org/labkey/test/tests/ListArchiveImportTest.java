/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ZipUtil;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class ListArchiveImportTest extends BaseWebDriverTest
{
    @Override
    protected @Nullable String getProjectName()
    {
        return "ListArchiveImport";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @BeforeClass
    public static void doSetup()
    {
        ListArchiveImportTest initTest = (ListArchiveImportTest) getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), null);
    }

    /**
     * Regression test:
     * Issue 32667: Errors during list import leave Connection in bad state
     */
    @Test
    public void testImportListArchiveWithError() throws IOException
    {
        final File listArchive = TestFileUtils.getSampleData("lists/bad_archive.lists");
        final String listName = "People";

        log("Import list and test for expected validation error");
        goToProjectHome();

        goToManageLists().importListArchiveExpectingError(new ZipUtil(listArchive).tempZip());

        String expectedError = "File: Could not find referenced file People.xls";
        String actualError = Locators.labkeyError.findOptionalElement(getDriver()).map(WebElement::getText).orElse(null);
        checker().verifyEquals("Wrong error message after attempting to import bad list archive.", expectedError, actualError);

        ManageListsGrid listsGrid = goToManageLists().getGrid();
        listsGrid.setContainerFilter(DataRegionTable.ContainerFilterType.CURRENT_FOLDER);
        if (checker().verifyEquals("Found list after import error.",
            List.of(listName), listsGrid.getListNames()))
        {
            DataRegionTable people = listsGrid.viewListData(listName);
            checker().verifyEquals("Shouldn't be any rows after import error.", 0, people.getDataRowCount());
        }
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }
}
