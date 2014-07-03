/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.DailyA;

import java.io.File;

@Category({DailyA.class})
public class QueryExportTest extends AbstractExportTest
{
    private static final File LIST_ARCHIVE = new File(getSampledataPath(), "lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";
    private static final String QUERY_NAME = "NIMHQuery";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Query Export Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return false;
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
        return QUERY_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "query";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        QueryExportTest initTest = (QueryExportTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._listHelper.importListArchive(initTest.getProjectName(), LIST_ARCHIVE);

        String sql = "SELECT SubjectId, Name FROM " + LIST_NAME;
        initTest.createQuery(initTest.getProjectName(), QUERY_NAME, "lists", sql, null, false);
    }

    @Override
    public void goToDataRegionPage()
    {
        clickProject(getProjectName());
        navigateToQuery("lists", QUERY_NAME);
    }

}
