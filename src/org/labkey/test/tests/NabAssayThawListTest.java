/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.AbstractAssayHelper;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class, Assays.class})
public class NabAssayThawListTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";

    private final String ASSAY_BACKGROUND_IMPORT_PROJECT = "Background Import Assay Data From List Project";

    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for NAb assay";
    public static final String THAW_LIST_ASSAY_ID = "thaw list ptid + visit";

    private final String SAMPLE_DATA_ROOT = TestFileUtils.getLabKeyRoot() + "/sampledata/Nab/";
    protected final File TEST_ASSAY_NAB_FILE1 = TestFileUtils.getSampleData("Nab/m0902051;3997.xls");
    private final String THAW_LIST_NAME = "NabThawList";
    private final String THAW_LIST_ARCHIVE = SAMPLE_DATA_ROOT + THAW_LIST_NAME + ".zip";

    private final String THAW_LIST_MISSING_COLUMNS = THAW_LIST_NAME + "MissingColumns";
    private final String THAW_LIST_ARCHIVE_MISSING_COLUMNS = SAMPLE_DATA_ROOT + THAW_LIST_NAME + "_MISSING_COLUMNS.zip";

    private final String THAW_LIST_MISSING_ROWS = THAW_LIST_NAME + "MissingRows";
    private final String THAW_LIST_ARCHIVE_MISSING_ROWS = SAMPLE_DATA_ROOT + THAW_LIST_NAME + "_MISSING_ROWS.zip";

    private final String THAW_LIST_BAD_DATATYPES = THAW_LIST_NAME + "BadDataTypes";
    private final String THAW_LIST_ARCHIVE_BAD_DATATYPES = SAMPLE_DATA_ROOT + THAW_LIST_NAME + "_BAD_DATATYPES.zip";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("nab");
    }

    @Override
    protected String getProjectName()
    {
        return "Nab Thaw ListTest Verify Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        NabAssayThawListTest init = (NabAssayThawListTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        // default project
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), TEST_ASSAY_FLDR_NAB);

        // project for validateBackgroundImporting
        _containerHelper.deleteProject(ASSAY_BACKGROUND_IMPORT_PROJECT, false);
        _containerHelper.createProject(ASSAY_BACKGROUND_IMPORT_PROJECT, null);
    }

    /**
     * Performs Nab Thaw List Default save/apply at upload.
     */
    @Test
    public void runUITests()
    {
        log("Testing NAb Assay Designer with Thaw List default");

        PortalHelper portalHelper = new PortalHelper(this);
        goToProjectHome();

        log("Create new Nab assay");
        portalHelper.addWebPart("Assay List");

        //create a new nab assay
        _assayHelper.createAssayAndEdit("TZM-bl Neutralization (NAb)", TEST_ASSAY_NAB)
                .setDescription(TEST_ASSAY_NAB_DESC)
                .save();

        log("Set default for ParticipantVisitResolver at Project level");
        // We'll override it later at the folder level.
        click(Locator.xpath("//div[text()='ParticipantVisitResolver']"));
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text') and text()='Advanced']"));
        clickAndWait(Locator.linkContainingText("value"));
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.ParticipantVisitDate.name()));
        clickButton("Save Defaults");
        clickButton("Save & Close");

        // Add the list we'll use for the thaw list lookup
        new ListHelper(this).importListArchive(TEST_ASSAY_FLDR_NAB, THAW_LIST_ARCHIVE);

        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        portalHelper.addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        log("Set folder level override of PVR default to thaw list.");
        _assayHelper.setDefaultValues(TEST_ASSAY_NAB, AbstractAssayHelper.AssayDefaultAreas.BATCH_FIELDS);

        // Are we seeing the default set in the parent project?
        Assert.assertEquals("Default participant visit resolver not inherited from project",
                AssayImportOptions.VisitResolverType.ParticipantVisitDate.name(),
                Locator.checkedRadioInGroup("participantVisitResolver").findElement(getDriver()).getAttribute("value"));

        // Now override
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", "Lookup"));

        // 20583 We now hide the option to paste in a tsv for the default value
        assertElementNotPresent(Locator.radioButtonByNameAndValue("ThawListType", "Text"));

        // 20998 as part of that fix, the List option should already be checked
        waitForElement(Locator.css(".schema-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListSchemaName"), "lists");
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), THAW_LIST_NAME);

        clickButton("Save Defaults");

        log("Uploading NAb Run");

        AssayImportOptions.ImportOptionsBuilder iob = new AssayImportOptions.ImportOptionsBuilder().
                assayId(THAW_LIST_ASSAY_ID).
                visitResolver(AssayImportOptions.VisitResolverType.LookupList).
                useDefaultResolver(true).
                cutoff1("50").
                cutoff2("70").
                virusName("Nasty Virus").
                virusId("5433211").
                curveFitMethod("Polynomial").
                sampleIds(new String[]{"1", "2", "3", "4", "5"}).
                initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                runFile(TEST_ASSAY_NAB_FILE1);

        importData(iob.build());

        verifyRunDetails();

        // Verify fix for 20441.
        log("Verify links to default values in parent folder work");
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        _assayHelper.setDefaultValues(TEST_ASSAY_NAB, AbstractAssayHelper.AssayDefaultAreas.BATCH_FIELDS);
        // If we don't blow up hitting this next link, the fix for 20441 is good.
        waitAndClickAndWait(Locator.linkContainingText("edit default values for this table in"));

        // As long as we're here, make sure inheritance is still being acknowledged.
        Assert.assertEquals("Default participant visit resolver not inherited from project",
                            AssayImportOptions.VisitResolverType.ParticipantVisitDate.name(),
                            Locator.checkedRadioInGroup("participantVisitResolver").findElement(getDriver()).getAttribute("value"));
        assertTextPresent("These values are overridden by defaults");

        log("Verify Delete and Re-import doesn't autofill SpecimenId");
        navToRunDetails();
        clickAndWait(Locator.linkWithText("Delete and Re-import"));
        Assert.assertEquals("Wrong participant visit resolver selected",
                "Lookup", Locator.checkedRadioInGroup("participantVisitResolver").findElement(getDriver()).getAttribute("value"));
        Assert.assertEquals("Wrong participant visit resolver selected",
                "List", Locator.checkedRadioInGroup("ThawListType").findElement(getDriver()).getAttribute("value"));
        waitForFormElementToEqual(Locator.tagWithName("input", "ThawListList-QueryName"), "NabThawList");
        clickButton("Next");
        assertElementPresent(Locator.input("specimen1_SpecimenID").withoutAttribute("value", ""));
        // Let's make sure *some* of the last entered values did get auto-filled.
        assertElementPresent(Locator.input("specimen1_InitialDilution").withAttribute("value", "20.0"));

        verifyValidation(iob);
    }

    protected void navToRunDetails()
    {
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        clickAndWait(Locator.linkWithText("run details"));
    }

    protected void verifyRunDetails()
    {
        DilutionAssayHelper assayHelper = new DilutionAssayHelper(this);

        log("verify " + THAW_LIST_ASSAY_ID);
        navToRunDetails();
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit, "A");
    }

    protected void verifyValidation(AssayImportOptions.ImportOptionsBuilder iob)
    {
        ListHelper lh = new ListHelper(this);
        lh.importListArchive(TEST_ASSAY_FLDR_NAB, THAW_LIST_ARCHIVE_MISSING_ROWS);
        lh.importListArchive(TEST_ASSAY_FLDR_NAB, THAW_LIST_ARCHIVE_MISSING_COLUMNS);
        lh.importListArchive(TEST_ASSAY_FLDR_NAB, THAW_LIST_ARCHIVE_BAD_DATATYPES);

        log("Verify can't select a thaw list with missing required columns.");
        setDefaultThawList(THAW_LIST_MISSING_COLUMNS);
        assertElementPresent(Locator.tagContainingText("font", "missing required column(s)"));

        log("Verify import with datatype mismatch for visitId fails.");
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), THAW_LIST_BAD_DATATYPES);
        clickButton("Save Defaults");
        importData(iob.resetDefaults(true).assayId(THAW_LIST_ASSAY_ID + " Bad Datatype").build());
        assertTextPresent("Can not convert VisitId value: 1x to double for specimenId: 1",
                "Can not convert VisitId value: 4x to double for specimenId: 4");

        log("Verify import with unresolved specimens fails.");
        setDefaultThawList(THAW_LIST_MISSING_ROWS);
        importData(iob.assayId(THAW_LIST_ASSAY_ID + " Missing thaw list rows").build());
        assertTextPresent("Can not resolve thaw list entry for specimenId: 4",
                "Can not resolve thaw list entry for specimenId: 5");
    }

    private void setDefaultThawList(String listName)
    {
        navigateToFolder(getProjectName(), TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        _assayHelper.setDefaultValues(TEST_ASSAY_NAB, AbstractAssayHelper.AssayDefaultAreas.BATCH_FIELDS);
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), listName);
        clickButton("Save Defaults");
    }

    @Test
    public void validateBackgroundImporting()
    {
        final String ASSAY_NAME = "Issue26774Assay";
        final String LIST_NAME = "DataList";
        final File STUDY_ZIP = TestFileUtils.getSampleData("AssayBackgroundImport/assayTestList.xls");

        // This test is to cover a testing gap identified in bug 26774 (Issues linking assay runs with sample sets).

        log("Test a General assay background importing data from a list.");

        PortalHelper portalHelper = new PortalHelper(this);

        clickProject(ASSAY_BACKGROUND_IMPORT_PROJECT);

        log("Create new General assay");
        portalHelper.addWebPart("Assay List");

        _assayHelper.createAssayAndEdit("General", ASSAY_NAME)
                .setDescription("Validating fix for issue 26774.")
                .setBackgroundImport(true)
                .saveAndClose();

        log("Create a list with data coming from the test file.");

        portalHelper.addWebPart("Lists");
        portalHelper.clickAndWait(Locator.linkWithText("Manage Lists"));

        _listHelper.createListFromFile(ASSAY_BACKGROUND_IMPORT_PROJECT, LIST_NAME, STUDY_ZIP);

        log("go back to home.");
        goToProjectHome(ASSAY_BACKGROUND_IMPORT_PROJECT);

        log("Now import the data from the list into the assay.");
        portalHelper.clickAndWait(Locator.linkWithText(ASSAY_NAME));
        portalHelper.clickButton(("Import Data"), "Batch Properties");

        portalHelper.checkRadioButton(Locator.radioButtonById("RadioBtn-Lookup"));
        portalHelper.waitForText("Use an existing sample list");
        portalHelper.checkRadioButton(Locator.radioButtonById("RadioBtn-ThawListType-List"));
        portalHelper.waitForText("Schema:");

        _ext4Helper.selectComboBoxItem(Locator.id("thawListSchemaName"), "lists");
        waitForElement(Locator.css(".query-loaded-marker"));
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), LIST_NAME);

        portalHelper.clickButton("Next", "Run Properties");

        portalHelper.checkRadioButton(Locator.radioButtonById("Fileupload"));
        portalHelper.waitForElement(Locator.xpath("//input[@name='__primaryFile__']"));
        portalHelper.setFormElement(Locator.xpath("//input[@name='__primaryFile__']"), STUDY_ZIP.getPath());

        portalHelper.clickButton("Save and Finish", "Data Pipeline");

        log("Validate that the pipeline job completed successfully.");
        waitForPipelineJobsToComplete(1, "Assay import job", false);

        log("Looks like pipeline job completed. Let's go home and clean up.");
        goToHome();
        _containerHelper.deleteProject(ASSAY_BACKGROUND_IMPORT_PROJECT, false);
    }
}
