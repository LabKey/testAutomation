/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({InDevelopment.class})
public class SharedReportTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        SharedReportTest init = (SharedReportTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        RReportHelper rHelper = new RReportHelper(this);
        rHelper.ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("simpletest");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSharedReport()
    {

    }

    @Test
    public void testSharedModuleReport()
    {

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SharedReportTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}