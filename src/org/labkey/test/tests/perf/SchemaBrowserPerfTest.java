/*
 * Copyright (c) 2016 LabKey Corporation
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
import org.labkey.api.writer.PrintWriters;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Perf;
import org.labkey.test.util.ListHelper;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

// this test generates ball park times for Schema Browser use cases on a study that has 200 dataSets.
//
// we would like to see an average of 5 seconds for an empty cache and 1 second for a full cache. for details on generating baseline see:
// https://docs.google.com/document/d/1pq-1QAN_-Z-_etfLXkn3zfO2lKNb2J1wcxp7WVKTSws/edit#heading=h.ii6uvmm3thhn
//
// the times arrived at manually using the open study folder test described in the above doc are: 13.44 seconds for an empty cache and  1.79 seconds for a full cache
// the times using this automated test are a bit different, so should be used in comparison with other runs of the automated test
//
// todo: more accurate timing is likely possible by using the goToURL() method to directly make the get requests for the study folder url (instead of simulating user clicks),
// but for some reason calls to goToURL timed out even though the page appeared to load ok in the browser.
// Another alternative may be to issue curl statements to invoke and time the get requests.

// as of 3/30/16 the times recorded in teamcity-info.xml using this test are:
//<build>
//        <statisticValue key="Open Study Folder With Empty Cache try - 0(ms)" value="11884"/>
//        <statisticValue key="Open Study Folder With Empty Cache try - 1(ms)" value="11643"/>
//        <statisticValue key="Open Study Folder With Empty Cache try - 2(ms)" value="11819"/>
//        <statisticValue key="Open Study Folder With Empty Cache try - 3(ms)" value="11585"/>
//        <statisticValue key="Open Study Folder With Empty Cache try - 4(ms)" value="11665"/>
//        <statisticValue key="Open Study Folder With Empty Cache Average (ms)" value="11719"/>
//        <statisticValue key="Open Study Folder With Full Cache try - 0(ms)" value="1008"/>
//        <statisticValue key="Open Study Folder With Full Cache try - 1(ms)" value="904"/>
//        <statisticValue key="Open Study Folder With Full Cache try - 2(ms)" value="976"/>
//        <statisticValue key="Open Study Folder With Full Cache try - 3(ms)" value="966"/>
//        <statisticValue key="Open Study Folder With Full Cache try - 4(ms)" value="1044"/>
//        <statisticValue key="Open Study Folder With Full Cache Average (ms)" value="979"/>
//        <statisticValue key="Open StudyData Schema With Full Cache try - 0(ms)" value="570"/>
//        <statisticValue key="Open StudyData Schema With Full Cache try - 1(ms)" value="650"/>
//        <statisticValue key="Open StudyData Schema With Full Cache try - 2(ms)" value="560"/>
//        <statisticValue key="Open StudyData Schema With Full Cache try - 3(ms)" value="706"/>
//        <statisticValue key="Open StudyData Schema With Full Cache try - 4(ms)" value="607"/>
//        <statisticValue key="Open StudyData Schema With Full Cache Average (ms)" value="618"/>
//</build>


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
        // add additional tables as needed
        // createDatasets("TestD", 100);
        writePerfDataToFile(studyBaselineEmptyCache(), studyBaselineFullCache(), studyDataBaselineFullCache());
    }

    private long[] studyBaselineEmptyCache() {
        long[] emptyCacheOpenStudyTimes = new long[5];
        // run tests
        for (int x = 0 ; x < 5; x++) {
            beginAt("/admin/caches.view?clearCaches=1", 120000);
            goToHome();
            clickProject(getProjectName());
            goToSchemaBrowser();
            long startTime = System.currentTimeMillis();
            selectSchema("study");
            waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
            emptyCacheOpenStudyTimes[x] = System.currentTimeMillis() - startTime;
        }
        return emptyCacheOpenStudyTimes;
     }

    private long[] studyBaselineFullCache() {
        long[] fullCacheOpenStudyTimes = new long[5];
        // prepare cache
        beginAt("/admin/caches.view?clearCaches=1", 120000);
        goToHome();
        clickProject(getProjectName());
        goToSchemaBrowser();
        selectSchema("study");
        waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
        // run tests
        for (int x = 0 ; x < 5; x++) {
            goToHome();
            clickProject(getProjectName());
            goToSchemaBrowser();
            long startTime = System.currentTimeMillis();
            selectSchema("study");
            waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
            fullCacheOpenStudyTimes[x] = System.currentTimeMillis() - startTime;
        }
        return fullCacheOpenStudyTimes;
    }

    private long[] studyDataBaselineFullCache() {
        long[] emptyCacheOpenStudyDataTimes = new long[5];
        // prepare cache
        beginAt("/admin/caches.view?clearCaches=1", 120000);
        goToHome();
        clickProject(getProjectName());
        goToSchemaBrowser();
        selectSchema("study");
        waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
        selectSchema("StudyData");
        waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
        // run tests
        for (int x = 0 ; x < 5; x++) {
            goToHome();
            clickProject(getProjectName());
            goToSchemaBrowser();
            selectSchema("study");
            waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
            long startTime = System.currentTimeMillis();
            selectSchema("StudyData");
            waitForElementToDisappear(Locator.css("div.lk-qd-loading").withText("loading..."), 100000);
            emptyCacheOpenStudyDataTimes[x] = System.currentTimeMillis() - startTime;
        }
        return emptyCacheOpenStudyDataTimes;
    }

    private void writePerfDataToFile(long[] emptyCacheOpenStudyTimes, long[] fullCacheOpenStudyTimes, long[] fullCacheOpenStudyDataTimes)
    {
        File xmlFile = new File(TestFileUtils.getLabKeyRoot(), "teamcity-info.xml");
        long total = 0;

        try (Writer writer = PrintWriters.getPrintWriter(xmlFile))
        {
            xmlFile.createNewFile();
            writer.write("<build>\n");
            renderTimesForUseCase("Open Study Folder With Empty Cache", emptyCacheOpenStudyTimes, writer );
            renderTimesForUseCase("Open Study Folder With Full Cache", fullCacheOpenStudyTimes, writer );
            renderTimesForUseCase("Open StudyData Schema With Full Cache", fullCacheOpenStudyDataTimes, writer );
            writer.write("</build>");
        }
        catch (IOException ignored) {}
    }

    private void renderTimesForUseCase(String useCaseName, long[] times, Writer writer ) {
        long total = 0;
        try
        {
            for (int x = 0; x < times.length; x++)
            {
                writer.write("\t<statisticValue key=\"" + useCaseName + " try - " + x + "(ms)\" value=\"" + times[x] + "\"/>\n");
                total = total + times[x];
            }
            writer.write("\t<statisticValue key=\"" + useCaseName + " Average (ms)\" value=\"" + total / times.length + "\"/>\n");
        }
        catch (IOException ignored) {}
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
