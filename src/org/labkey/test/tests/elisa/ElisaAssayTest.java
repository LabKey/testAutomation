/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests.elisa;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.assay.plate.PlateDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.tests.AbstractAssayTest;
import org.labkey.test.util.ExperimentalFeaturesHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.QCAssayScriptHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({Daily.class, Assays.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ElisaAssayTest extends AbstractAssayTest
{
    private final static String TEST_ASSAY_PRJ_ELISA = "ELISA Test Verify Project";
    private final static String TEST_ASSAY_FLDR_NAB = "ELISA";

    protected static final String TEST_ASSAY_ELISA = "TestAssayELISA";
    protected static final String TEST_ASSAY_ELISA_DESC = "Description for ELISA assay";

    protected final static String TEST_ASSAY_USR_NAB_READER = "nabreader1@security.test";
    private final static String TEST_ASSAY_GRP_NAB_READER = "Nab Dataset Reader";   //name of Nab Dataset Readers group

    protected static final File TEST_ASSAY_ELISA_FILE1 = TestFileUtils.getSampleData("Elisa/biotek_01.xlsx");
    protected static final File TEST_ASSAY_ELISA_FILE2 = TestFileUtils.getSampleData("Elisa/biotek_02.xls");
    protected static final File TEST_ASSAY_ELISA_FILE3 = TestFileUtils.getSampleData("Elisa/biotek_03.xlsx");
    protected static final File TEST_ASSAY_ELISA_FILE4 = TestFileUtils.getSampleData("Elisa/biotek_04.xls");

    private static final String PLATE_TEMPLATE_NAME = "ELISAAssayTest Template";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return TEST_ASSAY_PRJ_ELISA;
    }
    
    @Test
    public void runUITests() throws Exception
    {
        ExperimentalFeaturesHelper.disableExperimentalFeature(createDefaultConnection(), "elisaMultiPlateSupport");

        log("Starting ELISA Assay BVT Test");

        //revert to the admin user
        ensureSignedInAsPrimaryTestUser();

        log("Testing ELISA Assay Designer");

        // set up a scripting engine to run a java transform script
        new QCAssayScriptHelper(this).ensureEngineConfig();

        //create a new test project
        _containerHelper.createProject(TEST_ASSAY_PRJ_ELISA, null);

        //setup a pipeline for it
        setupPipeline(TEST_ASSAY_PRJ_ELISA);

        //add the Assay List web part so we can create a new ELISA assay
        clickProject(TEST_ASSAY_PRJ_ELISA);
        new PortalHelper(this).addWebPart("Assay List");

        //create a new ELISA template
        createTemplate();

        log("Setting up ELISA assay");
        clickProject(TEST_ASSAY_PRJ_ELISA);
        clickButton("Manage Assays");
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("ELISA", TEST_ASSAY_ELISA)
            .setDescription(TEST_ASSAY_ELISA_DESC)
            .setPlateTemplate(PLATE_TEMPLATE_NAME);
        // set the specimenId field default value to be : last entered
        assayDesignerPage.expandFieldsPanel("Sample")
            .getField("SpecimenId")
            .clickAdvancedSettings()
            .setDefaultValueType(FieldDefinition.DefaultType.LAST_ENTERED)
            .apply();
        assayDesignerPage.clickFinish();

        clickProject(TEST_ASSAY_PRJ_ELISA);
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_ELISA));

        log("Uploading ELISA Runs");
        clickButton("Import Data");
        clickButton("Next");

        uploadFile(TEST_ASSAY_ELISA_FILE1, "A", "Save and Import Another Run", true, 1, 5);
        assertTextPresent("Upload successful.");
        uploadFile(TEST_ASSAY_ELISA_FILE2, "B", "Save and Import Another Run", true, 1, 5);
        assertTextPresent("Upload successful.");
        uploadFile(TEST_ASSAY_ELISA_FILE3, "C", "Save and Import Another Run", true, 1, 5);
        assertTextPresent("Upload successful.");
        uploadFile(TEST_ASSAY_ELISA_FILE4, "D", "Save and Finish", true, 1, 5);
    }

    protected void createTemplate()
    {
        PlateDesignerPage.PlateDesignerParams params = new PlateDesignerPage.PlateDesignerParams(8, 12);
        params.setTemplateType("default");
        params.setAssayType("ELISA");
        PlateDesignerPage plateDesigner = PlateDesignerPage.beginAt(this, params);

        plateDesigner.setName(PLATE_TEMPLATE_NAME);
        plateDesigner.saveAndClose();
    }

    protected void uploadFile(File file, String uniqueifier, String finalButton, boolean testPrepopulation, int startSpecimen, int lastSpecimen)
    {
        for (int i = startSpecimen; i <= lastSpecimen; i++)
        {
            Locator specimenLocator = Locator.name("specimen" + (i) + "_SpecimenID");
            Locator participantLocator = Locator.name("specimen" + (i) + "_ParticipantID");

            // test for prepopulation of specimen form element values
//            if (testPrepopulation)
//                assertFormElementEquals(participantLocator, "ptid " + (i) + " " + uniqueifier);
            setFormElement(specimenLocator, "specimen " + (i) + " " + uniqueifier);
            setFormElement(participantLocator, "ptid " + (i) + " " + uniqueifier);

            setFormElement(Locator.name("specimen" + (i) + "_VisitID"), "" + (i));
        }

        setFormElement(Locator.name("__primaryFile__"), file);
        setFormElement(Locator.name("curveFitMethod"), "Linear");

        clickButton("Next");

        String allErrors = "";
        if (!testPrepopulation)
        {
            clickButton(finalButton); // cause errors
            clickAndWait(Locator.linkWithText("Too many errors to display (click to show all).\n"));
            _extHelper.waitForExtDialog("All Errors");
            allErrors = Locator.css(".x-window-body").findElement(getDriver()).getText();
            _extHelper.clickExtButton("All Errors", "Close", 0);
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }

        String[] letters = {"A","B","C","D","E","F","G","H"};
        for (int i = 0; i <= 5; i++)
        {
            setFormElement(Locator.name(letters[i].toLowerCase()+"1"+letters[i]+"2_Concentration"), "" + (i + 1));

            if (!testPrepopulation)
            {
                assertTrue("Missing error for well group " + letters[i], allErrors.contains("Value for well group: " + letters[i] + "1-" + letters[i] + "2 cannot be blank."));
            }
        }

        clickButton(finalButton);
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
