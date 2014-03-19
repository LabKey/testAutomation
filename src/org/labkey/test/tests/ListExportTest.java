/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverMultipleTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExcelHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.labkey.test.util.DataRegionExportHelper.*;

@Category({DailyB.class})
public class ListExportTest extends AbstractExportTest
{
    private static final File LIST_ARCHIVE = new File(getSampledataPath(), "lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";

    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @Override
    protected String getTestColumnTitle()
    {
        return "Name";
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 1;
    }

    @Override
    protected String getExportedTsvTestColumnHeader()
    {
        return "name";
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return LIST_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "query";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ListExportTest initTest = new ListExportTest();
        initTest.doCleanup(false);

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._listHelper.importListArchive(initTest.getProjectName(), LIST_ARCHIVE);

        currentTest = initTest;
    }

    public void goToDataRegionPage()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
       deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
