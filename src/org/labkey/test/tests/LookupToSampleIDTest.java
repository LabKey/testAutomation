/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class})
public class LookupToSampleIDTest extends BaseWebDriverTest
{
    private static final String FOLDER_NAME = "TestingGPATAssay";
    private static final String FOLDER_TYPE_STUDY = "Study";
    private static final File SAMPLE_SET = TestFileUtils.getSampleData("GPAT/SampleIDLookupSampleSetData.xlsx");
    private static final File SAMPLE_SET_SUB_FOLDER = TestFileUtils.getSampleData("GPAT/SampleIDLookupSampleSetDataSubFolder.xlsx");
    private static final String SAMPLE_SET_NAME = "SampleSet";
    public static final String SAMPLE_ASSAY_IMPORT_DATA_FILE_NAME = "SampleIDLookupAssayImportData";
    private static final File ASSAY_IMPORT = TestFileUtils.getSampleData("GPAT/" + SAMPLE_ASSAY_IMPORT_DATA_FILE_NAME + ".xlsx");
    public static final String SAMPLE_ASSAY_IMPORT_DATA_FILE_SPLIT_NAME = "SampleIDLookupAssayImportDataForSplitSampleSet";
    private static final File ASSAY_IMPORT_SPLIT = TestFileUtils.getSampleData("GPAT/" + SAMPLE_ASSAY_IMPORT_DATA_FILE_SPLIT_NAME + ".xlsx");
    private static final String SAMPLE_ID_FIELD_NAME = "SampleID";
    private static final String SAMPLE_ID_FIELD_LABEL = "Sample ID";

    private static final String SAMPLE_SET_ID_PROJECT_LEVEL_FOUND = "ID_123456";
    private static final String SAMPLE_SET_ID_PROJECT_LEVEL_NOT_FOUND = "<ID_123456>";
    private static final String SAMPLE_SET_ID_FOLDER_LEVEL_FOUND = "ID_123461";
    private static final String SAMPLE_SET_ID_FOLDER_LEVEL_NOT_FOUND = "<ID_123461>";
    private final String _subfolder = "/" + getProjectName() + "/" + FOLDER_NAME;
    private final String _projectFolder = "/" + getProjectName();

    @BeforeClass
    public static void setupProject()
    {
        LookupToSampleIDTest init = (LookupToSampleIDTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        //add Sample Set webpart
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Sets");
        portalHelper.addWebPart("Assay List");

        //import a sample set
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), SAMPLE_SET_NAME);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.name("file"), SAMPLE_SET);
        clickButton("Submit");

        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, FOLDER_TYPE_STUDY);

        clickFolder(FOLDER_NAME);
        portalHelper.addWebPart("Sample Sets");
        portalHelper.addWebPart("Assay List");

        //import more to sample set while in subfolder
        clickAndWait(Locator.linkWithText(SAMPLE_SET_NAME));
        clickButton("Import More Samples");
        checkRadioButton(Locator.radioButtonById("insertOrUpdateChoice"));
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.name("file"), SAMPLE_SET_SUB_FOLDER);
        clickButton("Submit");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickFolder(FOLDER_NAME);
    }

    @Test
    public void testSamplesSameContainerIntegerWarning()
    {
        String assayName = "LookupAssay_Integer_Same";
        String sampleSetFolder = _subfolder;

        createAssay(assayName, SAMPLE_SET_NAME, "Integer", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT_SPLIT); //import data into assay
        waitForText("Failed to convert 'SampleID': Could not translate value: ID_123456");

        goToProjectHome();
        clickFolder(FOLDER_NAME);
        assayName = "LookupAssay_Integer_Same_Success";
        sampleSetFolder = _projectFolder;

        createAssay(assayName, SAMPLE_SET_NAME, "Integer", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT); //import data into assay

        clickAndWait(Locator.linkContainingText(SAMPLE_ASSAY_IMPORT_DATA_FILE_NAME));

        waitForText(SAMPLE_SET_ID_PROJECT_LEVEL_FOUND);

        assertTextNotPresent(SAMPLE_SET_ID_PROJECT_LEVEL_NOT_FOUND);
    }

    @Test
    public void testSamplesReferencedAcrossMultipleContainers()
    {
        String assayName = "LookupAssay_String_Same";
        String sampleSetFolder = _subfolder;

        createAssay(assayName, SAMPLE_SET_NAME, "String", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT_SPLIT); //import data into assay
        clickAndWait(Locator.linkContainingText(SAMPLE_ASSAY_IMPORT_DATA_FILE_SPLIT_NAME));
        waitForText(SAMPLE_SET_ID_PROJECT_LEVEL_NOT_FOUND);
        waitForText(SAMPLE_SET_ID_FOLDER_LEVEL_FOUND);
        assertTextNotPresent(SAMPLE_SET_ID_FOLDER_LEVEL_NOT_FOUND);

        goToProjectHome();
        clickFolder(FOLDER_NAME);
        assayName = "LookupAssay_String_Project";
        sampleSetFolder = _projectFolder;

        createAssay(assayName, SAMPLE_SET_NAME, "String", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT_SPLIT); //import data into assay
        clickAndWait(Locator.linkContainingText(SAMPLE_ASSAY_IMPORT_DATA_FILE_SPLIT_NAME));
        waitForText(SAMPLE_SET_ID_PROJECT_LEVEL_FOUND);
        assertTextNotPresent(SAMPLE_SET_ID_PROJECT_LEVEL_NOT_FOUND);
        waitForText(SAMPLE_SET_ID_FOLDER_LEVEL_NOT_FOUND);

        goToProjectHome();
        clickFolder(FOLDER_NAME);
        assayName = "LookupAssay_String_Default";
        sampleSetFolder = null;//will leave as default or [current project]

        createAssay(assayName, SAMPLE_SET_NAME, "String", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT_SPLIT); //import data into assay
        clickAndWait(Locator.linkContainingText(SAMPLE_ASSAY_IMPORT_DATA_FILE_SPLIT_NAME));
        waitForText(SAMPLE_SET_ID_PROJECT_LEVEL_FOUND);
        waitForText(SAMPLE_SET_ID_FOLDER_LEVEL_FOUND);
        assertTextNotPresent(SAMPLE_SET_ID_FOLDER_LEVEL_NOT_FOUND);
    }

    @Test
    public void testStringTableLookupValue()
    {
        String assayName = "LookupAssay_String";
        String sampleSetFolder = "/LookupToSampleIDTest Project/TestingGPATAssay";

        createAssay(assayName, SAMPLE_SET_NAME, "String", sampleSetFolder);
        importDataInAssay(assayName, ASSAY_IMPORT); //import data into assay
        testAssay(assayName); //test links
    }

    @Test @Ignore //This test is incomplete. Out of scope of the story (Spec 22999: Have result row in the assay have a lookup to the sample.) for which this Automated Test file was created.
    public void testIntegerTableLookupValue()
    {
        String assayName = "LookupAssay_Integer";
        String sampleSetFolder = "/LookupToSampleIDTest Project/TestingGPATAssay";

        createAssay(assayName, SAMPLE_SET_NAME, "Integer", sampleSetFolder);
    }

    private void createAssay(String name, String lookupTableValue, String lookupTableType, String sampleSetFolder)
    {
        AssayDesignerPage assayDesigner = _assayHelper.createAssayAndEdit("General", name);

        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo(sampleSetFolder, "samples", lookupTableValue);
        lookupInfo.setTableType(lookupTableType);

        _listHelper.addLookupField(name + " Data Fields", 4, SAMPLE_ID_FIELD_NAME, SAMPLE_ID_FIELD_LABEL, lookupInfo);
        assayDesigner.saveAndClose();
    }

    private void importDataInAssay(String assayName, File assayImportFile)
    {
        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        checkRadioButton(Locator.radioButtonById("RadioBtn-SampleInfo"));
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), assayImportFile);

        clickButton("Save and Finish");
    }

    private void testAssay(String assayName)
    {
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(new DataRegionTable("Batches", this).link(0, "Name"));
        clickAndWait(Locator.tag("img").withAttribute("src", "/labkey/Experiment/images/graphIcon.gif"));
        clickAndWait(Locator.linkWithText("Text View"));
        clickAndWait(Locator.linkContainingText("123456"));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "LookupToSampleIDTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
