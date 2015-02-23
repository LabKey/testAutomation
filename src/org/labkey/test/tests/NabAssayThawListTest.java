/*
 * Copyright (c) 2009-2015 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class, Assays.class})
public class NabAssayThawListTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Thaw ListTest Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";

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
        return TEST_ASSAY_PRJ_NAB;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    /**
     * Performs Nab Thaw List Default save/apply at upload.
     */
    @Test
    public void runUITests()
    {
        log("Testing NAb Assay Designer with Thaw List default");
         _containerHelper.createProject(TEST_ASSAY_PRJ_NAB, null);

        PortalHelper portalHelper = new PortalHelper(this);


        _containerHelper.createSubfolder(TEST_ASSAY_PRJ_NAB, TEST_ASSAY_FLDR_NAB, null);

        clickProject(TEST_ASSAY_PRJ_NAB);

        log("Create new Nab assay");
        portalHelper.addWebPart("Assay List");
        //create a new nab assay
        clickButton("Manage Assays");
        startCreateNabAssay(TEST_ASSAY_NAB);
        setFormElement(Locator.xpath("//textarea[@id='AssayDesignerDescription']"), TEST_ASSAY_NAB_DESC);

        clickButton("Save", 0);
        waitForText(20000, "Save successful.");

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

        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        portalHelper.addWebPart("Assay List");

        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        log("Set folder level override of PVR default to thaw list.");
        click(Locator.linkWithText("manage assay design"));
        mouseOver(Locator.linkWithText("set default values"));
        prepForPageLoad();
        waitAndClick(Ext4Helper.Locators.menuItem(TEST_ASSAY_NAB + " Batch Fields"));

        // Are we seeing the default set in the parent project?
        assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.ParticipantVisitDate.name()));

        // Now override
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", "Lookup"));

        // 20583 We now hide the option to paste in a tsv for the default value
        assertElementNotPresent(Locator.radioButtonByNameAndValue("ThawListType", "Text"));

        // 20998 as part of that fix, the List option should already be checked
        // click(Locator.radioButtonByNameAndValue("ThawListType", "List"));

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
                //ptids(new String[]{"ptid 1 A", "ptid 2 A", "ptid 3 A", "ptid 4 A", "ptid 5 A"}).
                        //visits(new String[]{"1", "2", "3", "4", "5"}).
                        sampleIds(new String[]{"1", "2", "3", "4", "5"}).
                initialDilutions(new String[]{"20", "20", "20", "20", "20"}).
                dilutionFactors(new String[]{"3", "3", "3", "3", "3"}).
                methods(new String[]{"Dilution", "Dilution", "Dilution", "Dilution", "Dilution"}).
                runFile(TEST_ASSAY_NAB_FILE1);


        importData(iob.build());

        verifyRunDetails();

        // Verify fix for 20441.
        log("Verify links to default values in parent folder work");
        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));

        click(Locator.linkWithText("manage assay design"));
        mouseOver(Locator.linkWithText("set default values"));
        prepForPageLoad();
        waitAndClick(Locator.menuItem(TEST_ASSAY_NAB + " Batch Fields"));
        // If we don't blow up hitting this next link, the fix for 20441 is good.
        waitAndClick(Locator.linkContainingText("edit default values for this table in"));

        // As long as we're here, make sure inheritance is still being acknowledged.
        assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.ParticipantVisitDate.name()));
        assertTextPresent("These values are overridden by defaults");

        log("Verify Delete and Re-import doesn't autofill SpecimenId");
        navToRunDetails();
        click(Locator.linkWithText("Delete and Re-import"));
        assertChecked(Locator.radioButtonByNameAndValue("ThawListType", "List"));
        // TODO: verify its the NabThawList thats selected
        clickButton("Next");
        assertElementPresent(Locator.input("specimen1_SpecimenID").withAttribute("value", ""));
        // Let's make sure *some* of the last enetered values did get auto-filled.
        assertElementPresent(Locator.input("specimen1_InitialDilution").withAttribute("value", "20.0"));

        verifyValidation(iob);

    }

    protected void navToRunDetails()
    {
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
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
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), THAW_LIST_BAD_DATATYPES);
        clickButton("Save Defaults");
        importData(iob.resetDefaults(true).assayId(THAW_LIST_ASSAY_ID + " Bad Datatype").build());
        assertTextPresent("Can not convert VisitId value: 1x to double for specimenId: 1");
        assertTextPresent("Can not convert VisitId value: 4x to double for specimenId: 4");

        log("Verify import with unresolved specimens fails.");
        setDefaultThawList(THAW_LIST_MISSING_ROWS);
        importData(iob.assayId(THAW_LIST_ASSAY_ID + " Missing thaw list rows").build());
        assertTextPresent("Can not resolve thaw list entry for specimenId: 4");
        assertTextPresent("Can not resolve thaw list entry for specimenId: 5");

    }

    private void setDefaultThawList(String listName)
    {
        clickProject(TEST_ASSAY_PRJ_NAB);
        clickFolder(TEST_ASSAY_FLDR_NAB);
        clickAndWait(Locator.linkWithText(TEST_ASSAY_NAB));
        _ext4Helper.clickExt4MenuButton(true, Locator.linkWithText("manage assay design"), false, "set default values", TEST_ASSAY_NAB + " Batch Fields");
        _ext4Helper.selectComboBoxItem(Locator.id("thawListQueryName"), listName);
        clickButton("Save Defaults");
    }
}
