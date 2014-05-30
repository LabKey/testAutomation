/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.DilutionAssayHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

@Category({DailyA.class, Assays.class})
public class NabAssayThawListTest extends AbstractQCAssayTest
{
    private final static String TEST_ASSAY_PRJ_NAB = "Nab Thaw ListTest Verify Project";            //project for nab test
    private final static String TEST_ASSAY_FLDR_NAB = "nabassay";

    protected static final String TEST_ASSAY_NAB = "TestAssayNab";
    protected static final String TEST_ASSAY_NAB_DESC = "Description for NAb assay";
    public static final String THAW_LIST_ASSAY_ID = "thaw list ptid + visit";

    private final String SAMPLE_DATA_ROOT = getLabKeyRoot() + "/sampledata/Nab/";
    protected final String TEST_ASSAY_NAB_FILE1 = SAMPLE_DATA_ROOT + "m0902051;3997.xls";
    private final String THAW_LIST_NAME = "NabThawList";
    private final String THAW_LIST_ARCHIVE = SAMPLE_DATA_ROOT + THAW_LIST_NAME + ".zip";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/nab";
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
    protected void runUITests()
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
        waitForText("Save successful.", 20000);

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
        waitAndClick(Locator.menuItem(TEST_ASSAY_NAB + " Batch Fields"));

        // Are we seeing the default set in the parent project?
        assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.ParticipantVisitDate.name()));

        // Now override
        click(Locator.radioButtonByNameAndValue("participantVisitResolver", "Lookup"));
        click(Locator.radioButtonByNameAndValue("ThawListType", "List"));
        waitForElement(getButtonLocatorContainingText("Choose list"));
        click(getButtonLocatorContainingText("Choose list"));
        click(Locator.divById("partdown_table"));
        Locator tableListBox = Locator.tagWithClass("select", "gwt-ListBox");
        waitForElement(tableListBox);
        selectOptionByValue(tableListBox, THAW_LIST_NAME);
        click(getButtonLocatorContainingText("Close"));
        clickButton("Save Defaults");

        log("Uploading NAb Run");

        importData(
                new AssayImportOptions.ImportOptionsBuilder().
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
                        filePath(TEST_ASSAY_NAB_FILE1).
                        build()
        );

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

    }

    protected void verifyRunDetails()
    {
        clickAndWait(Locator.linkWithText("View Runs"));
        DilutionAssayHelper assayHelper = new DilutionAssayHelper(this);

        log("verify " + THAW_LIST_ASSAY_ID);
        clickAndWait(Locator.linkWithText("View Runs"));
        clickAndWait(Locator.linkWithText(THAW_LIST_ASSAY_ID));
        clickAndWait(Locator.linkWithText("run details"));
        assayHelper.verifyDataIdentifiers(AssayImportOptions.VisitResolverType.ParticipantVisit, "A");
    }
}
