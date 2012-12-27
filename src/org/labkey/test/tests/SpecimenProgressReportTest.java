/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.junit.Assert;
import org.labkey.test.util.UIAssayHelper;

import java.io.IOException;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/11/12
 * Time: 2:42 PM
 */
public class SpecimenProgressReportTest extends BaseSeleniumWebTest
{
    public static final String STUDY_PIPELINE_ROOT = getLabKeyRoot() + "/sampledata/specimenprogressreport";
    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    private static final String studyFolder = "study folder";
    private static final String assayFolder = "assay folder";
    int pipelineCount = 0;
    private static final String assay1 = "PCR";
    private static final String assay1File = "PCR Data.tsv";
    private static final String assay1XarPath = "/assays/PCR.xar";
    private static final String assay2 = "RNA";
    private static final String assay2File = "RNA Data.tsv";
    private static final String assay2XarPath = "/assays/RNA.xar";
    private Locator.XPathLocator tableLoc = Locator.xpath("//table[@id='dataregion_ProgressReport']");

    @Override
    protected String getProjectName()
    {
        //Issue 16247: tricky characters in project name cause alert when trying to add a lookup to a rho query in the folder
        return "Specimen Progress Report Test" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    public boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _assayHelper = new UIAssayHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), studyFolder, "Study");
        importFolderFromZip(STUDY_PIPELINE_ROOT + "/Study.folder.zip");

        // set the label for the unscheduled visit
        clickLinkWithText("Manage");
        clickLinkWithText("Manage Visits");
        clickAndWait(Locator.xpath("//td[contains(text(),'999.0-999.9999')]/../td/a[contains(text(), 'edit')]"));
        setFormElement(Locator.name("label"), "SR");
        clickLinkWithText("Save");

        createAssayFolder();
        waitForText("This assay is unlocked");
        assertTextPresent("33 collections have occurred.",  "48 results from " + assay1 + " have been uploaded", "46 " + assay1 + " queries");
        assertTextNotPresent("Configuration error:",
                "You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report.");

        waitForElement(tableLoc);
        Assert.assertEquals(2, getXpathCount( Locator.xpath("//td[contains(@class, 'available')]")));
        Assert.assertEquals(22, getXpathCount( Locator.xpath("//td[contains(@class, 'query')]")));
        Assert.assertEquals(2, getXpathCount( Locator.xpath("//td[contains(@class, 'collected')]")));
        Assert.assertEquals(0, getXpathCount(Locator.xpath("//td[contains(@class, 'invalid')]")));

        flagSpecimenForReview(assay1, assay1File, null);

        waitForElement(tableLoc);
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[contains(@class, 'invalid')]")));

        // verify legend text and ordering
        assertTextPresentInThisOrder("specimen expected", "specimen received by lab", "specimen not collected",
                "specimen collected", "specimen received but invalid", "assay results available"); // "query" appears too many times on page!

        // verify setting the PCR additional grouping column
        verifyAdditionalGroupingColumn(assay1, "gene");

        // verify unscheduled visit ordering for the RNA assay
        verifyUnscheduledVisitDisplay(assay2);
    }

    private void verifyAdditionalGroupingColumn(String assayName, String groupCol)
    {
        clickLinkWithText(assayFolder);
        waitForText("46 " + assayName + " queries");
        configureGroupingColumn(assayName, groupCol);
        waitForElement(tableLoc);
        clickLinkWithText("48 results from " + assayName + " have been uploaded.");
        assertTextPresent("Participant Visit not found", 8);
        assertTextPresent("2 duplicates found", 4);
    }

    private void verifyUnscheduledVisitDisplay(String assayName)
    {
        clickLinkWithText(assayFolder);
        waitForElement(tableLoc);
        configureAssayProgressDashboard(assay2);
        configureAssaySchema(assayName);

        flagSpecimenForReview(assayName, assay2File, "2011-03-02");

        clickLinkWithText(assayFolder);
        waitForElement(tableLoc);

        _ext4Helper.selectRadioButtonById(assayName + "-boxLabelEl");
        waitForElement(tableLoc);
        assertTextPresentInThisOrder("SR1", "SR2", "SR3");
        Assert.assertEquals(4, getXpathCount( Locator.xpath("//td[contains(@class, 'available')]")));
        Assert.assertEquals(2, getXpathCount( Locator.xpath("//td[contains(@class, 'query')]")));
        Assert.assertEquals(4, getXpathCount( Locator.xpath("//td[contains(@class, 'collected')]")));
        Assert.assertEquals(1, getXpathCount(Locator.xpath("//td[contains(@class, 'invalid')]")));

        clickLinkWithText("7 results from " + assayName + " have been uploaded.");
        assertTextPresent("Participant Visit not found", 1);
        assertTextPresent("Specimen type is not expected by this Assay", 1);
    }

    private void flagSpecimenForReview(String assayName, String runName, @Nullable String collectionDateFilterStr)
    {
        clickLinkWithText(assayFolder);

        clickLinkWithText(assayName);
        clickLinkWithText(runName);

        if (collectionDateFilterStr != null)
        {
            DataRegionTable drt = new DataRegionTable("Data", this);
            drt.setFilter("Date", "Equals", collectionDateFilterStr);
        }

        click(Locator.tagWithAttribute("img", "title", "Flag for review"));
        clickButton("OK", 0);
        waitForElement(Locator.tagWithAttribute("img", "title", "Flagged for review"));

        clickLinkWithText(assayFolder);
    }

    private void createAssayFolder() throws CommandException, IOException
    {
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), assayFolder, "Assay");
        enableModule(assayFolder, "rho");

        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + assay1XarPath, ++pipelineCount, assay1);
        _assayHelper.importAssay(assay1, STUDY_PIPELINE_ROOT + "/assays/" + assay1File,  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );
        clickLinkWithText(assayFolder);
        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + assay2XarPath, ++pipelineCount, assay2);
        _assayHelper.importAssay(assay2, STUDY_PIPELINE_ROOT + "/assays/" + assay2File,  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );


        clickLinkWithText(assayFolder);
        addWebPart("Assay Progress Dashboard");
        addWebPart("Assay Progress Report");
        assertTextPresent("You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report", 2);

        configureAssayProgressDashboard(assay1);
        configureAssaySchema(assay1);
        clickLinkWithText(assayFolder);
    }

    private void configureAssaySchema(String assayName)
    {
        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo("", "rho", assayName + " Query");
        _assayHelper.addAliasedFieldToMetadata("assay.General." + assayName, "Data", "RowId", "qcmessage", lookupInfo);
        clickButton("Save", 0);
        waitForText("Save successful.");
        clickButton("View Data");

        log("Set up data viewto include QC message");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("qcmessage/QueryMessage", "Query Message");
        _customizeViewsHelper.saveCustomView();
    }

    private void configureAssayProgressDashboard(String assayName)
    {
        clickWebpartMenuItem("Assay Progress Dashboard", true, "Customize");
        _extHelper.checkCheckbox(assayName);
        click(Locator.tagContainingText("label", "Specimen Report Study Folder Study"));
        clickButton("Save");
    }

    private void configureGroupingColumn(String label, String name)
    {
        clickWebpartMenuItem("Assay Progress Dashboard", true, "Customize");
        setFormElement(Locator.xpath("//td/label[contains(text(),'" + label + ":')]/../../td/input[@name='groupingColumn']"), name);
        clickButton("Save");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
