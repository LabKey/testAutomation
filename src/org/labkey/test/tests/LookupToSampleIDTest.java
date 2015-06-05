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
/**
 * Created by binalpatel on 5/4/15.
 */

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({InDevelopment.class})
public class LookupToSampleIDTest extends BaseWebDriverTest
{
    private static final String FOLDER_NAME = "TestingGPATAssay";
    private static final String FOLDER_TYPE_STUDY = "Study" ;
    private static final File SAMPLE_SET = new File("/GPAT/SampleIDLookupSampleSetData.xlsx");
    private static final String SAMPLE_SET_NAME = "SampleSet";
    private static final File ASSAY_IMPORT = new File("/GPAT/SampleIDLookupAssayImportData.xlsx");
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

        //create a folder of type Study
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);

        //set folder type to 'general'
        goToProjectHome(getProjectName());
        clickFolder(FOLDER_NAME);
        _containerHelper.setFolderType(FOLDER_TYPE_STUDY);

        goToProjectHome(getProjectName());
        clickFolder(FOLDER_NAME);

        //add Sample Set webpart
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Sets");

        //import a sample set
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), SAMPLE_SET_NAME);
        checkRadioButton(Locator.radioButtonByNameAndValue("uploadType", "file"));
        setFormElement(Locator.name("file"), TestFileUtils.getSampleData(SAMPLE_SET.getPath())); //TODO: MOve to the top
        clickButton("Submit");

        goToProjectHome(getProjectName());
        clickFolder(FOLDER_NAME);

        portalHelper.addWebPart("Assay List");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testStringTableLookupValue()
    {
        goToProjectHome();
        clickFolder(FOLDER_NAME);
        String assayName = ASSAY_IMPORT.getName() + "_String";

        createAssay("General", assayName, SAMPLE_SET_NAME); //Create New Assay
        importDataInAssay(assayName); //import data into assay
        testAssay(assayName); //test links
    }

    @Test
    public void testIntegerTableLookupValue()
    {
        goToProjectHome();
        clickFolder(FOLDER_NAME);
        String assayName = ASSAY_IMPORT.getName() + "_Integer";

        createAssay("General", assayName, SAMPLE_SET_NAME+" (Integer)"); //Create New Assay
        importDataInAssay(assayName); //import data into assay
        testAssay(assayName); //test links
    }

    private void createAssay(String type, String name, String lookupTableValue)
    {
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", type));
        clickButton("Next");
        setFormElement(Locator.name("AssayDesignerName"), name);

        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo("/LookupToSampleIDTest Project/TestingGPATAssay", "Samples", lookupTableValue);

        _listHelper.addLookupField("Data Fields", 4, SAMPLE_ID_FIELD_NAME, SAMPLE_ID_FIELD_LABEL, lookupInfo);
        clickButton("Save & Close");
    }

    private void importDataInAssay(String assayName)
    {
        goToProjectHome();
        clickFolder(FOLDER_NAME);
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        checkRadioButton(Locator.radioButtonById("RadioBtn-SampleInfo"));
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), TestFileUtils.getSampleData(ASSAY_IMPORT.getPath()));

        clickButton("Save and Finish");
    }

    private void testAssay(String assayName)
    {
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText("batch"));
        clickAndWait(Locator.imageWithSrc("/labkey/Experiment/images/graphIcon.gif", false));
        clickTab("Text View");
        click(Locator.linkContainingText("123456"));
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