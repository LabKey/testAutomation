/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.apache.commons.lang.math.NumberUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.List;

public class ElispotAssayTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_ELISPOT = "Elispot Test Verify Project";
    private final static String TEST_ASSAY_FLDR_NAB = "elispot";

    protected static final String TEST_ASSAY_ELISPOT = "TestAssayElispot";
    protected static final String TEST_ASSAY_ELISPOT_DESC = "Description for Elispot assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected final String TEST_ASSAY_ELISPOT_FILE1 = getLabKeyRoot() + "/sampledata/Elispot/CTL_040A20042503-0001p.xls";
    protected final String TEST_ASSAY_ELISPOT_FILE2 = getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W4.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE3 = getLabKeyRoot() + "/sampledata/Elispot/Zeiss_datafile.txt";
    protected final String TEST_ASSAY_ELISPOT_FILE4 = getLabKeyRoot() + "/sampledata/Elispot/AID_0161456 W5.txt";

    private static final String PLATE_TEMPLATE_NAME = "ElispotAssayTest Template";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/nab";
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_ELISPOT;
    }

    /**
     * Performs Luminex designer/upload/publish.
     */
    protected void runUITests()
    {
        log("Starting Elispot Assay BVT Test");

        //revert to the admin user
        revertToAdmin();

        log("Testing Elispot Assay Designer");

        // set up a scripting engine to run a java transform script
        prepareProgrammaticQC();

        //create a new test project
        createProject(TEST_ASSAY_PRJ_ELISPOT);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_ELISPOT);

        //add the Assay List web part so we can create a new elispot assay
        clickLinkWithText(TEST_ASSAY_PRJ_ELISPOT);
        addWebPart("Assay List");

        //create a new elispot template
        clickNavButton("Manage Assays");
        clickNavButton("Configure Plate Templates");
        clickLinkWithText("new 96 well (8x12) ELISpot default template");
        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);

        setText("templateName", PLATE_TEMPLATE_NAME);
        clickNavButton("Save & Close");
        assertTextPresent(PLATE_TEMPLATE_NAME);

        //create a new elispot assay
        clickLinkWithText(TEST_ASSAY_PRJ_ELISPOT);
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "ELISpot");
        clickNavButton("Next");

        log("Setting up Elispot assay");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        selenium.type("//input[@id='AssayDesignerName']", TEST_ASSAY_ELISPOT);

        selectOptionByValue(Locator.xpath("//select[@id='plateTemplate']"), PLATE_TEMPLATE_NAME);
        selenium.type("//textarea[@id='AssayDesignerDescription']", TEST_ASSAY_ELISPOT_DESC);

        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        clickLinkWithText(TEST_ASSAY_PRJ_ELISPOT);
        clickLinkWithText("Assay List");
        clickLinkWithText(TEST_ASSAY_ELISPOT);

        log("Uploading Elispot Runs");
        clickNavButton("Import Data");
        clickNavButton("Next");

        selectOptionByText("plateReader", "Cellular Technology Ltd. (CTL)");
        uploadFile(TEST_ASSAY_ELISPOT_FILE1, "A", "Save and Import Another Run");
        assertTextPresent("Upload successful.");

        selectOptionByText("plateReader", "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE2, "B", "Save and Import Another Run");
        assertTextPresent("Upload successful.");

        selectOptionByText("plateReader", "Zeiss");
        uploadFile(TEST_ASSAY_ELISPOT_FILE3, "C", "Save and Finish");

        assertElispotData();
        runTransformTest();
    }

    private void uploadFile(String filePath, String uniqueifier, String finalButton)
    {
        for (int i = 0; i < 4; i++)
        {
            setFormElement("specimen" + (i + 1) + "_ParticipantID", "ptid " + (i + 1) + " " + uniqueifier);
            setFormElement("specimen" + (i + 1) + "_VisitID", "" + (i + 1));
            setFormElement("specimen" + (i + 1) + "_SampleDescription", "blood");
        }

        setFormElement("dataCollectorName", "File upload");
        File file1 = new File(filePath);
        setFormElement("__primaryFile__", file1);
        clickNavButton("Next");

        for (int i = 0; i < 6; i++)
        {
            setFormElement("antigen" + (i + 1) + "_AntigenID", "" + (i + 1));
            setFormElement("antigen" + (i + 1) + "_AntigenName", "atg_" + (i + 1) + uniqueifier);
            setFormElement("antigen" + (i + 1) + "_CellWell", "150");
        }

        clickNavButton(finalButton, 60000);
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    private void assertElispotData()
    {
        clickLinkWithText("Zeiss_datafile.txt");
        
        assertTextPresent("ptid 1 C");
        assertTextPresent("ptid 2 C");
        assertTextPresent("ptid 3 C");
        assertTextPresent("ptid 4 C");

        assertTextPresent("atg_1C");
        assertTextPresent("atg_2C");
        assertTextPresent("atg_3C");
        assertTextPresent("atg_4C");

        clickLinkWithText("view runs");
        clickLinkWithText("AID_0161456 W4.txt");

        assertTextPresent("ptid 1 B");
        assertTextPresent("ptid 2 B");
        assertTextPresent("ptid 3 B");
        assertTextPresent("ptid 4 B");

        assertTextPresent("atg_1B");
        assertTextPresent("atg_2B");
        assertTextPresent("atg_3B");
        assertTextPresent("atg_4B");

        // show the normalized spot count column and verify it is calculated correctly
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "NormalizedSpotCount");
        CustomizeViewsHelper.applyCustomView(this);

        DataRegionTable dataTable = new DataRegionTable(TEST_ASSAY_ELISPOT + " Data", this);
        List<String> cellWell = dataTable.getColumnDataAsText("CellWell");
        List<String> spotCount = dataTable.getColumnDataAsText("SpotCount");
        List<String> normalizedSpotCount = dataTable.getColumnDataAsText("NormalizedSpotCount");

        for (int i=0; i < cellWell.size(); i++)
        {
            int cpw = NumberUtils.toInt(cellWell.get(i), 0);
            Float sc = NumberUtils.toFloat(spotCount.get(i));
            Float nsc = NumberUtils.toFloat(normalizedSpotCount.get(i));
            Float computed = sc;

            if (cpw != 0)
                computed = sc / cpw * 1000000;

            assertEquals(computed.intValue(), nsc.intValue());
        }
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.revertUnsavedView(this);

        clickLinkWithText("view runs");
        clickLinkContainingText("details");

        assertTextPresent("Plate Summary Information");
        assertTextPresent("Sample Well Groups:");
        assertTextPresent("Antigen Well Groups:");
        assertTextPresent("Antigen 7 Mean");
        assertTextPresent("Antigen 7 Median");
        assertTextPresent("Antigen 8 Mean");
        assertTextPresent("Antigen 8 Median");
        assertTextPresent("blood");

        // test color hilighting of sample and antigen well groups
        checkRadioButton("sampleGroup", 1);
        assertElementPresent(getLocatorForHilightedWell("lk_sample_Specimen_2", "1023.0"));
        assertElementPresent(getLocatorForHilightedWell("lk_sample_Specimen_2", "1021.0"));
        assertElementPresent(getLocatorForHilightedWell("lk_sample_Specimen_2", "1028.0"));

        // antigen well group
        checkRadioButton("sampleGroup", 5);
        assertElementPresent(getLocatorForHilightedWell("lk_antigen_Antigen_2", "765.0"));
        assertElementPresent(getLocatorForHilightedWell("lk_antigen_Antigen_2", "591.0"));
        assertElementPresent(getLocatorForHilightedWell("lk_antigen_Antigen_2", "257.0"));

        // test the mean and median values
        DataRegionTable table = new DataRegionTable(TEST_ASSAY_ELISPOT + " AntigenStats", this);
        String[] expectedMeans = new String[]{"15555.6", "8888.9", "122222.2", "46666.7"};
        String[] expectedMedians = new String[]{"13333.3", "13333.3", "126666.7", "40000.0"};

        int row = 0;
        for (String mean : expectedMeans)
            assertEquals(mean, table.getDataAsText(row++, "Atg1CMean"));

        row = 0;
        for (String median : expectedMedians)
            assertEquals(median, table.getDataAsText(row++, "Atg1CMedian"));

        // verify customization of the run details view is possible
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 7_Mean");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 7_Median");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 8_Mean");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 8_Median");
        CustomizeViewsHelper.saveCustomView(this, "Without Antigen7&8");

        clickMenuButton("Views", "default");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 7_Mean");
        CustomizeViewsHelper.removeCustomizeViewColumn(this, "Antigen 7_Median");
        CustomizeViewsHelper.saveDefaultView(this);

        clickMenuButton("Views", "Without Antigen7&8");
        assertTextNotPresent("Antigen 7 Mean");
        assertTextNotPresent("Antigen 7 Median");
        assertTextNotPresent("Antigen 8 Mean");
        assertTextNotPresent("Antigen 8 Median");
        
        clickMenuButton("Views", "default");
        assertTextNotPresent("Antigen 7 Mean");
        assertTextNotPresent("Antigen 7 Median");
        assertTextPresent("Antigen 8 Mean");
        assertTextPresent("Antigen 8 Median");
    }

    private Locator getLocatorForHilightedWell(String className, String count)
    {
        String xpath = String.format("//div[contains(@class, '%s') and contains(@style, 'background-color: rgb(18, 100, 149);')]//a[contains(text(), %s)]",
                className, count);
        return Locator.xpath(xpath);
    }

    /**
     * Cleanup entry point.
     */
    protected void doCleanup()
    {
        revertToAdmin();
        try {
            deleteProject(TEST_ASSAY_PRJ_ELISPOT);
            deleteEngine();
        }
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    } //doCleanup()

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading Elispot Runs with a transform script");

        clickLinkWithText(TEST_ASSAY_PRJ_ELISPOT);
        clickLinkWithText(TEST_ASSAY_ELISPOT);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        clickNavButton("Save & Close");

        clickLinkWithText(TEST_ASSAY_PRJ_ELISPOT);
        clickLinkWithText(TEST_ASSAY_ELISPOT);
        clickNavButton("Import Data");
        clickNavButton("Next");

        setFormElement("name", "transformed assayId");
        selectOptionByText("plateReader", "AID");
        uploadFile(TEST_ASSAY_ELISPOT_FILE4, "D", "Save and Finish");

        // verify there is a spot count value of 747.747 and a custom column added by the transform
        clickLinkWithText("AID_0161456 W5.txt");
        assertTextPresent("747.7");
        assertTextPresent("Custom Elispot Column");
        assertTextPresent("transformed!");
    }
}
