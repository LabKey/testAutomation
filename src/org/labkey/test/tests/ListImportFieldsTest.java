/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: tgaluhn
 * Date: 4/24/2014
 */
@Category({DailyA.class, Data.class})
public class ListImportFieldsTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "List Import Fields Test";
    private static final File LIST_FIELD_IMPORT = new File(TestFileUtils.getSampledataPath(), "lists/ListImportFields.txt");
    private static final String LIST_NAME = "Test List";
    private static final String REPLACEMENT_COL = "Nukeum";
    private ListHelper listHelper = new ListHelper(this);

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @BeforeClass
    @LogMethod
    public static void doSetup() throws Exception
    {
        ListImportFieldsTest initTest = (ListImportFieldsTest)getCurrentTest();
        initTest.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Test
    public void FieldImportExportTest() throws Exception
    {
        listHelper.beginCreateList(PROJECT_NAME, LIST_NAME);
        clickButton("Create List", 0);

        // Key field name is editable, type  is fixed
        waitForElement(Locator.name("ff_name0"));
        assertElementNotPresent(Locator.xpath("//input[@name='ff_type0']"));

        clickButton("Import Fields", "WARNING");
        String listCols = TestFileUtils.getFileContents(LIST_FIELD_IMPORT);
        setFormElement(Locator.id("schemaImportBox"), listCols);

        clickButton("Import", 0);
        waitForElement(Locator.xpath("//input[@name='ff_name2']"), WAIT_FOR_JAVASCRIPT);

        clickButton("Export Fields", "The schema can be copied");
        assertTrue("Error: List field 'Test Field' not found in field export", getFormElement(Locator.id("schemaImportBox")).contains("Test Field"));
        assertFalse("Error: List Key field found in field export", getFormElement(Locator.id("schemaImportBox")).contains("Key"));
        clickButton("Done", "Save");
        clickButton("Save", 0);
        waitForText("Edit Design");
        clickButton("Done", 0);
        waitForElement(Locator.linkWithText("Details"));
        clickAndWait(Locator.linkWithText("Details"));
        waitForText("Insert New");

        Map<String, String> dataRow = new HashMap<>();
        dataRow.put("Test Field", "Some Data");
        dataRow.put("Another Test Field", "4");
        listHelper.insertNewRow(dataRow);

        click(Locator.linkWithText("View Design"));
        waitForText("Edit Design");
        clickButton("Edit Design", 0);
        waitForText("Import Fields");
        clickButton("Import Fields", "WARNING");
        setFormElement(Locator.id("schemaImportBox"), "Property\n" + REPLACEMENT_COL);
        clickButton("Import", 0);
        waitForElement(Locator.xpath("//input[@name='ff_name1']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Save", 0);
        waitForText("Edit Design");
        clickButton("Done", 0);
        waitForElement(Locator.linkWithText("Details"));
        clickAndWait(Locator.linkWithText("Details"));
        waitForText("No data to show.");
        assertTextPresent(REPLACEMENT_COL);
    }
}
