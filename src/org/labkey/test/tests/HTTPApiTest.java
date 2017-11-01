/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.APITestHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class HTTPApiTest extends BaseWebDriverTest
{
    private static final String LIST_NAME = "Test List";

    private static final ListHelper.ListColumn COL1 = new ListHelper.ListColumn("Like", "Like", ListHelper.ListColumnType.String, "What the color is like");
    private static final ListHelper.ListColumn COL2 = new ListHelper.ListColumn("Month", "Month to Wear", ListHelper.ListColumnType.DateTime, "When to wear the color", "M");
    private static final ListHelper.ListColumn COL3 = new ListHelper.ListColumn("Good", "Quality", ListHelper.ListColumnType.Integer, "How nice the color is");
    private final static String[][] TEST_DATA = { { "Blue", "Green", "Red", "Yellow" },
            { "Zany", "Robust", "Mellow", "Light"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"} };
    private final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + CONVERTED_MONTHS[0] + "\t" + TEST_DATA[3][0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + CONVERTED_MONTHS[1] + "\t" + TEST_DATA[3][1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + CONVERTED_MONTHS[2] + "\t" + TEST_DATA[3][2];
    private final static String LIST_ROW4 = TEST_DATA[0][3] + "\t" + TEST_DATA[1][3] + "\t" + CONVERTED_MONTHS[3] + "\t" + TEST_DATA[3][3];
    private final String LIST_DATA = "Color\t" + COL1.getName() +
            "\t" + COL2.getName() + "\t" + COL3.getName() + "\n" + LIST_ROW1 + "\n" + LIST_ROW2 + "\n" + LIST_ROW3 + "\n" + LIST_ROW4;

    protected String getProjectName()
    {
        return "HTTPApiVerifyProject";
    }

    protected File[] getTestFiles()
    {
        return new File[]{new File(TestFileUtils.getLabKeyRoot() + "/server/test/data/api/http-api.xml")};
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

    @BeforeClass
    public static void initTest() throws Exception
    {
        HTTPApiTest init = (HTTPApiTest)getCurrentTest();
        init.createTestData();
    }

    public void createTestData()
    {
        log("Create Project");
        _containerHelper.createProject(getProjectName(), null);
        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Lists");

        log("Create List");
        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.String, "Color", COL1, COL2, COL3);
        _listHelper.clickEditDesign();
        selectOptionByText(Locator.id("ff_titleColumn"), "Like");    // Explicitly set to the PK (auto title will pick wealth column)
        clickButton("Save", 0);
        waitForElement(Locator.id("button_Import Data"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Like");

        log("Upload data");
        clickButton("Import Data");
        _listHelper.submitTsvData(LIST_DATA);
    }

    @Test
    public void testQueryTestLists() throws Exception
    {
        APITestHelper apiTester = new APITestHelper(this);
        apiTester.setTestFiles(getTestFiles());
        apiTester.runApiTests();
    }
}
