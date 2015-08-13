/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
public class SampleSetTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleSetTestProject";
    private static final String FOLDER_NAME = "SampleSetTestFolder";
    private static final String PROJECT_SAMPLE_SET_NAME = "ProjectSampleSet";
    private static final String FOLDER_SAMPLE_SET_NAME = "FolderSampleSet";
    private static final String FOLDER_CHILDREN_SAMPLE_SET_NAME = "FolderChildrenSampleSet";
    private static final String FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME = "FolderGrandchildrenSampleSet";

    protected static final String PIPELINE_PATH = "/sampledata/xarfiles/expVerify";
    private static final String AMBIGUOUS_CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
            "SampleSetBVTChildB\tSampleSetBVT4\t2.2\n";

    private static final String CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT11\t1.1\n" +
            "SampleSetBVTChildB\tFolderSampleSet.SampleSetBVT4\t2.2\n";

    private static final String REPARENTED_CHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTChildA\tSampleSetBVT13\t1.111\n" +
            "SampleSetBVTChildB\tFolderSampleSet.SampleSetBVT14\t2.222\n";

    private static final String GRANDCHILD_SAMPLE_SET_TSV = "Name\tParent\tOtherProp\n" +
            "SampleSetBVTGrandchildA\tSampleSetBVTChildA,SampleSetBVTChildB\t11.11\n";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);

        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[]{"Experiment"});

        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Sample Sets");

        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), PROJECT_SAMPLE_SET_NAME);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.tagWithName("input", "file"), TestFileUtils.getSampleData("sampleSet.xlsx").getAbsolutePath());
        waitForFormElementToEqual(Locator.id("idCol1"), "0"); // "KeyCol"
        clickButton("Submit");

        clickFolder(FOLDER_NAME);
        // TODO: why is the webpart being added multiple times?
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), FOLDER_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), "KeyCol-Folder\tIntCol-Folder\tStringCol-Folder\n" +
                "SampleSetBVT11\t101\taa\n" +
                "SampleSetBVT4\t102\tbb\n" +
                "SampleSetBVT12\t102\tbb\n" +
                "SampleSetBVT13\t103\tcc\n" +
                "SampleSetBVT14\t104\tdd");
        clickButton("Submit");

        // Do some manual derivation
        clickAndWait(Locator.linkWithText("Sample Sets"));
        assertTextPresent(PROJECT_SAMPLE_SET_NAME, FOLDER_NAME);

        clickButton("Show All Materials");
        assertTextPresent(FOLDER_SAMPLE_SET_NAME);
        assertTextNotPresent(PROJECT_SAMPLE_SET_NAME);

        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        if (isElementPresent(Locator.linkWithText("configure a valid pipeline root for this folder")))
        {
            setPipelineRoot(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH);
        }

        clickFolder(FOLDER_NAME);
        assertTextPresent(FOLDER_SAMPLE_SET_NAME, PROJECT_SAMPLE_SET_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));
        checkCheckbox(Locator.name(".toggle"));
        clickButton("Derive Samples");

        selectOptionByText(Locator.name("inputRole0"), "Add a new role...");
        setFormElement(Locator.id("customRole0"), "FirstRole");
        selectOptionByText(Locator.name("inputRole1"), "Add a new role...");
        setFormElement(Locator.id("customRole1"), "SecondRole");
        selectOptionByText(Locator.name("inputRole2"), "Add a new role...");
        setFormElement(Locator.id("customRole2"), "ThirdRole");
        selectOptionByText(Locator.name("inputRole3"), "Add a new role...");
        setFormElement(Locator.id("customRole3"), "FourthRole");
        selectOptionByText(Locator.name("outputCount"), "2");
        selectOptionByText(Locator.name("targetSampleSetId"), "FolderSampleSet in /SampleSetTestProject/SampleSetTestFolder");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_KeyColFolder"), "SampleSetBVT15");
        setFormElement(Locator.name("outputSample2_KeyColFolder"), "SampleSetBVT16");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500a");
        setFormElement(Locator.name("outputSample1_StringColFolder"), "firstOutput");
        setFormElement(Locator.name("outputSample2_StringColFolder"), "secondOutput");
        clickButton("Submit");

        assertTextPresent("must be of type Integer");
        checkCheckbox(Locator.name("outputSample1_IntColFolderCheckBox"));
        setFormElement(Locator.name("outputSample1_IntColFolder"), "500");
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText("Derive 2 samples"));
        clickAndWait(Locator.linkContainingText("Text View"));
        assertTextPresent("FirstRole", "SecondRole", "ThirdRole", "FourthRole");

        clickAndWait(Locator.linkContainingText("16"));
        clickAndWait(Locator.linkContainingText("derive samples from this sample"));

        selectOptionByText(Locator.name("inputRole0"), "FirstRole");
        selectOptionByText(Locator.name("targetSampleSetId"), "ProjectSampleSet in /SampleSetTestProject");
        clickButton("Next");

        setFormElement(Locator.name("outputSample1_KeyCol"), "200");
        setFormElement(Locator.name("outputSample1_IntCol"), "600");
        setFormElement(Locator.name("outputSample1_StringCol"), "String");
        setFormElement(Locator.name("outputSample1_DateCol"), "BadDate");
        uncheckCheckbox(Locator.name("outputSample1_BoolCol"));
        clickButton("Submit");

        assertTextPresent("must be of type Date and Time");
        setFormElement(Locator.name("outputSample1_DateCol"), "1/1/2007");
        clickButton("Submit");

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT11"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT12"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT13"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));

        clickAndWait(Locator.linkWithText("SampleSetBVT11"));

        assertElementPresent(Locator.linkWithText("Derive sample from SampleSetBVT16"));
        assertElementPresent(Locator.linkWithText("Derive 2 samples from SampleSetBVT11, SampleSetBVT12, SampleSetBVT13, SampleSetBVT14, SampleSetBVT4"));

        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkWithText(FOLDER_SAMPLE_SET_NAME));

        assertTextPresent("aa", "bb", "cc", "dd", "firstOutput", "secondOutput");

        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Show All Materials");
        assertTextPresent("ProjectSampleSet", "200");

        // Try to derive samples using the parent column
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), AMBIGUOUS_CHILD_SAMPLE_SET_TSV);
        fireEvent(Locator.name("data"), SeleniumEvent.change);
        waitForElement(Locator.css("select#parentCol > option").withText("Parent"));
        Locator.id("parentCol").findElement(getDriver()).sendKeys("Parent"); // combo-box helper doesn't work
        clickButton("Submit");
        assertTextPresent("More than one match for parent material");

        // Try again with a qualified sample name
        setFormElement(Locator.name("data"), CHILD_SAMPLE_SET_TSV);
        fireEvent(Locator.name("data"), SeleniumEvent.change);
        waitForElement(Locator.css("select#parentCol > option").withText("Parent"));
        Locator.id("parentCol").findElement(getDriver()).sendKeys("Parent"); // combo-box helper doesn't work
        clickButton("Submit");
        assertTextPresent("SampleSetBVTChildA");

        fileAttachmentTest();
        clickAndWait(Locator.linkWithText("FolderChildrenSampleSet"));
        clickAndWait(Locator.linkWithText("SampleSetBVTChildB"));

        // Make sure that the parent got wired up
        clickAndWait(Locator.linkWithText("SampleSetBVT4"));
        // Check out the run
        clickAndWait(Locator.linkWithText("Derive sample from SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVT4"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTChildB"));

        // Make a grandchild set, but first try to insert as a duplicate set name
        clickTab("Experiment");
        clickAndWait(Locator.linkWithText("Sample Sets"));
        clickButton("Import Sample Set");
        setFormElement(Locator.name("name"), FOLDER_CHILDREN_SAMPLE_SET_NAME);
        setFormElement(Locator.name("data"), GRANDCHILD_SAMPLE_SET_TSV);
        fireEvent(Locator.name("data"), SeleniumEvent.change);
        waitForElement(Locator.css("select#parentCol > option").withText("Parent"));
        Locator.id("parentCol").findElement(getDriver()).sendKeys("Parent"); // combo-box helper doesn't work
        clickButton("Submit");

        assertTextPresent("A sample set with that name already exists");
        setFormElement(Locator.name("name"), FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME);
        Locator.id("parentCol").findElement(getDriver()).sendKeys("Parent"); // combo-box helper doesn't work
        clickButton("Submit");

        clickAndWait(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Filter out any child materials, though there shouldn't be any
        setFilter("childMaterials", "Name", "Is Blank");
        // Check for parents and grandparents
        assertTextPresent("SampleSetBVTChildB", "SampleSetBVT4", "SampleSetBVT11");

        // Verify that we've chained things together properly
        clickAndWait(Locator.linkWithText("SampleSetBVTChildA"));
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertTextPresent("SampleSetBVT11");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT11"));
        assertTextPresent("SampleSetBVTGrandchildA");

        // Go up the chain one more hop
        clearAllFilters("parentMaterials", "Name");
        clickAndWait(Locator.linkWithText("SampleSetBVT11"));
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTChildA"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertTextPresent("SampleSetBVTChildA", "SampleSetBVTGrandchildA");

        clickAndWait(Locator.linkWithText(FOLDER_CHILDREN_SAMPLE_SET_NAME));
        clickButton("Import More Samples");
        checkRadioButton(Locator.radioButtonById("insertOrUpdateChoice"));
        setFormElement(Locator.name("data"), REPARENTED_CHILD_SAMPLE_SET_TSV);
        clickButton("Submit");

        clickAndWait(Locator.linkWithText("SampleSetBVTChildB"));
        assertTextPresent("2.222");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT4"));
        // Filter out any child materials so we can just check for parents
        setFilter("childMaterials", "Name", "Is Blank");
        assertElementPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementNotPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));
        // Switch to filter out any parent materials so we can just check for children
        setFilter("parentMaterials", "Name", "Is Blank");
        clearFilter("childMaterials", "Name");
        assertElementNotPresent(Locator.linkWithText("SampleSetBVT14"));
        assertElementPresent(Locator.linkWithText("SampleSetBVTGrandchildA"));

        // Verify that the event was audited
        goToModule("Query");
        selectQuery("auditLog", "SampleSetAuditEvent");
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickAndWait(Locator.linkWithText("view data"));
        assertTextPresent(
                "Samples inserted or updated in: " + FOLDER_SAMPLE_SET_NAME,
                "Samples inserted or updated in: " + FOLDER_CHILDREN_SAMPLE_SET_NAME,
                "Samples inserted or updated in: " + FOLDER_GRANDCHILDREN_SAMPLE_SET_NAME);
    }

    final File experimentFilePath = new File(TestFileUtils.getLabKeyRoot() + PIPELINE_PATH, "experiment.xar.xml");

    private void fileAttachmentTest()
    {
        enableFileInput();

        setFileAttachment(0, experimentFilePath);
        setFileAttachment(1, new File(TestFileUtils.getLabKeyRoot() +  "/sampledata/sampleset/RawAndSummary~!@#$%^&()_+-[]{};',..xlsx"));
        inserNewWithFileAttachmentTest();
    }

    private void inserNewWithFileAttachmentTest()
    {
        clickButton("Insert New");
        setFormElement(Locator.name("quf_Name"), "SampleSetInsertedManually");
        setFormElement(Locator.name("quf_FileAttachment"), experimentFilePath);
        clickButton("Submit");
        //a double upload causes the file to be appended with a count
        assertTextPresent("experiment-1.xar.xml");
    }

    private void enableFileInput()
    {
        String inputFormID =   "name2-input";
        String fileField = "FileAttachment";
        clickButton("Edit Fields");
        waitForElement(Locator.lkButton("Add Field"), defaultWaitForPage);
        ListHelper listHelper = new ListHelper(this);
        listHelper.addField(new ListHelper.ListColumn(fileField, fileField, ListHelper.ListColumnType.File, fileField));
        clickButton("Save");
    }

    private void setFileAttachment(int index, File attachment)
    {
        clickAndWait(Locator.linkWithText("edit").index(index));
        setFormElement(Locator.name("quf_FileAttachment"),  attachment);
        clickButton("Submit");

        DataRegionTable drt = new DataRegionTable("Material", this);

        String path = drt.getDataAsText(index, "File Attachment");
        assertNotNull("Path shouldn't be null", path);
        assertTrue("Path didn't contain " + attachment.getName() + ", but was: " + path, path.contains(attachment.getName()));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
