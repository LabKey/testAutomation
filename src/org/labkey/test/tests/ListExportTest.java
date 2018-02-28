/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Hosting;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class, Hosting.class})
public class ListExportTest extends AbstractExportTest
{
    private static final File LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";

    @Override
    protected String getProjectName()
    {
        return "List Download Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return true;
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
    public ColumnHeaderType[] getExportHeaderTypes()
    {
        return new ColumnHeaderType[]{ColumnHeaderType.Caption, ColumnHeaderType.None, ColumnHeaderType.FieldKey};
    }

    @Override
    protected String getExportedXlsTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getExportedTsvTestColumnHeader(ColumnHeaderType exportType)
    {
        return "Name";
    }

    @Override
    protected String getDataRegionColumnName()
    {
        return "Name";
    }

    @Override
    protected String getDataRegionSchemaName()
    {
        return "lists";
    }

    @Override
    protected String getDataRegionQueryName()
    {
        return LIST_NAME;
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
        ListExportTest initTest = (ListExportTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._listHelper.importListArchive(initTest.getProjectName(), LIST_ARCHIVE);
    }

    @Override
    public void goToDataRegionPage()
    {
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(LIST_NAME));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }
}
