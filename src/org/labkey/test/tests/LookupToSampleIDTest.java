/*
 * Copyright (c) 2015 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDomainEditor;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class})
public class LookupToSampleIDTest extends BaseWebDriverTest
{
    private static final String FOLDER_NAME = "TestingGPATAssay";
    private static final String FOLDER_TYPE_STUDY = "Study" ;
    private static final File SAMPLE_SET = TestFileUtils.getSampleData("GPAT/SampleIDLookupSampleSetData.xlsx");
    private static final String SAMPLE_SET_NAME = "SampleSet";
    private static final File ASSAY_IMPORT = TestFileUtils.getSampleData("GPAT/SampleIDLookupAssayImportData.xlsx");
    private static final String SAMPLE_ID_FIELD_NAME = "SampleID";
    private static final String SAMPLE_ID_FIELD_LABEL = "Sample ID";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        LookupToSampleIDTest init = (LookupToSampleIDTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME, FOLDER_TYPE_STUDY);
        clickFolder(FOLDER_NAME);

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
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickFolder(FOLDER_NAME);
    }

    @Test
    public void testStringTableLookupValue()
    {
        String assayName = "LookupAssay_String";

        createAssay("General", assayName, SAMPLE_SET_NAME, "String");
        importDataInAssay(assayName); //import data into assay
        testAssay(assayName); //test links
    }

    @Test @Ignore //This test is incomplete. Out of scope of the story (Spec 22999: Have result row in the assay have a lookup to the sample.) for which this Automated Test file was created.
    public void testIntegerTableLookupValue()
    {
        String assayName = "LookupAssay_Integer";

        createAssay("General", assayName, SAMPLE_SET_NAME, "Integer");
        //select rows using SelectRowsCommand
        //get rowids for each sample

        //        importDataInAssay(assayName); //import data into assay
        //        testAssay(assayName); //test links
    }

    private void createAssay(String type, String name, String lookupTableValue, String lookupTableType)
    {
        AssayDomainEditor assayDesigner = _assayHelper.createAssayAndEdit(type, name);

        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo("/LookupToSampleIDTest Project/TestingGPATAssay", "samples", lookupTableValue);
        lookupInfo.setTableType(lookupTableType);

        _listHelper.addLookupField(name + " Data Fields", 4, SAMPLE_ID_FIELD_NAME, SAMPLE_ID_FIELD_LABEL, lookupInfo);
        assayDesigner.saveAndClose();
    }

    private void importDataInAssay(String assayName)
    {
        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        checkRadioButton(Locator.radioButtonById("RadioBtn-SampleInfo"));
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), ASSAY_IMPORT);

        clickButton("Save and Finish");
    }

    private void testAssay(String assayName)
    {
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.id("Batches").append(Locator.linkContainingText("batch")));
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
