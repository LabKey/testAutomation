/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
package org.labkey.test.tests.perf;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Perf;
import org.labkey.test.util.ListHelper;

@Category(Perf.class)
public class SchemaBrowserPerfTest extends PerformanceTest
{
    @Override
    protected String getProjectName()
    {
        return "Schema Browser Perf Test Project";
    }

    @Test
    public void testSteps()
    {
        setIsPerfTest(true);
        _containerHelper.createProject(getProjectName(), "Study");

        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudyWith200Tables.zip"));
        openStudyFolderTime();

        // add additional tables as needed
        // createDatasets("TestD", 100);
    }

    private void openStudyFolderTime() {
        goToSchemaBrowser();
        // test home page links
//        waitAndClick(Locator.tagWithClass("span", "labkey-link").withText("core"));
//        waitForElement(Locator.tagWithClass("div", "lk-qd-name").withText("core Schema"));
        long startTime = System.currentTimeMillis();
        selectSchema("study");
        elapsedTime = System.currentTimeMillis() - startTime;
        writePerfDataToFile();
    }

    private void createLists(int count)
    {
        for (int x = 0; x < count; x++)
        {
            _listHelper.createList(getProjectName(), "TestList"+x,
                    ListHelper.ListColumnType.AutoInteger, "AuthorId",
                    new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "first name test desc"),
                    new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "")
            );
        }

    }

    private void createDatasets(String nameBase, int count)
    {
        for (int x = 0; x < count; x++)
        {
            createDataset(nameBase+x);
        }

    }

    protected void createDataset(String name)
    {
        _studyHelper.goToManageDatasets();
        waitForText("Create New Dataset");
        click(Locator.xpath("//a[text()='Create New Dataset']"));
        waitForElement(Locator.xpath("//input[@name='typeName']"));
        setFormElement(Locator.xpath("//input[@name='typeName']"), name);
        clickButton("Next");
        waitForElement(Locator.xpath("//input[@id='name0-input']"));
        assertTextNotPresent("XTest");
        setFormElement(Locator.xpath("//input[@id='name0-input']"), "XTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name1-input']"));
        assertTextNotPresent("YTest");
        setFormElement(Locator.xpath("//input[@id='name1-input']"), "YTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name2-input']"));
        assertTextNotPresent("ZTest");
        setFormElement(Locator.xpath("//input[@id='name2-input']"), "ZTest");
        clickButton("Save");
    }

}
