/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Study;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.SearchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@Category({DailyB.class, Study.class})
public class StudyPublishTest extends StudyProtectedExportTest
{
    private final String ID_PREFIX = "PUBLISHED-";
    private final int ID_DIGITS = 8;
    private final File PROTOCOL_DOC = new File( getLabKeyRoot() + getStudySampleDataPath() + "/Protocol.txt");
    private final String STUDY_LABEL = "Original Study";
    private final String STUDY_INVESTIGATOR = "Original Investigator";
    private final String STUDY_GRANT = "Original Grant";
    private final String STUDY_DESCRIPTION = "This is the study we start with.";

    private final String GROUP0_NAME = "Unshared Group";
    private final String[] GROUP0_PTIDS = {"1234"};
    private final String GROUP1_NAME = "Group 1";
    private final String[] GROUP1_PTIDS = {"999320016", "999321029", "999320485", "999320518", "999320529", "999320533"};
    private final String GROUP2_NAME = "Group 2";
    private final String[] GROUP2_PTIDS = {"999320016", "999320719", "999320565", "999320576", "999320582", "999320590"};
    private final String GROUP3_NAME = "Group 3";
    private final String[] GROUP3_PTIDS = {"999321033", "999320613", "999320624", "999320638", "999320646", "999320652"};
    private final String[] PTIDS_WITHOUT_SPECIMENS = {"1234", "999320016", "999320485", "999320518", "999320529", "999320533",
            "999320541", "999320557", "999320565", "999320576", "999320582", "999320590", "999320609", "999320613", "999320624",
            "999320638", "999320646", "999320652", "999320660", "999320671", "999320687", "999320695", "999320703", "999320719",
            "999321029", "999321033"};

    private final String[] DATASETS = {"RCB-1: Reactogenicity-Baseline", "RCM-1: Reactogenicity-Early Assessment", "RCE-1: Reactogenicity-Day 0", "RCH-1: Reactogenicity-Day 1", "RCF-1: Reactogenicity-Day 2", "RCT-1: Reactogenicity-Day 3", "CPS-1: Screening Chemistry Panel", "DEM-1: Demographics"};
    private final String REPORT_DATASET = "RCB-1: Reactogenicity-Baseline";
    private final String UNPUBLISHED_REPORT_DATASET = "AE-1:(VTN) AE Log";
    private final String DATE_SHIFT_DATASET_LABEL = "CPS-1: Screening Chemistry Panel";
    private final String DATE_SHIFT_DATASET = "CPS-1 (" + DATE_SHIFT_DATASET_LABEL + ")";
    private final String DATE_SHIFT_REQUIRED_VISIT = "101";
    private final ImmutablePair<String, String> UNSHIFTED_DATE_FIELD = new ImmutablePair<>("CPSdt", "Initial Spec Collect Date");
    private final ImmutablePair<String, String> SHIFTED_DATE_FIELD = new ImmutablePair<>("CPScredt", "2a.Alt Creat Coll Date");
    private HashMap<String, Set<String>> unshiftedDatesByStudy = new HashMap<>();
    private HashMap<String, Set<String>> preshiftedDatesByStudy = new HashMap<>();
    private final String[] VISITS = {"101", "201", "601", "1301"};
    private final String R_VIEW = "Shared R View";
    private final String R_VIEW2 = "R View on Unpublished Dataset";
    private final String R_VIEW_UNSHARED = "Unshared R View";
    private final String MOUSE_REPORT = "Shared Mouse Report";
    private final String TIME_CHART = "Shared Time Chart";
    private final String[] TIME_CHART_MEASURE1 = {DATASETS[2], "2.Body temperature"};
    private final String[] TIME_CHART_MEASURE2 = {DATASETS[3], "2.Body temperature"};
    private final String CUSTOM_VIEW = "Shared Custom View";
    private final String[] CUSTOM_VIEW_PTIDS = {"999320016", "999321029", "999320518"};
    private final String CUSTOM_VIEW2 = "Private Custom View";
    private final String[] CUSTOM_VIEW_PTIDS2 = {};

    private final File LIST_ARCHIVE =  new File(getLabKeyRoot() + getStudySampleDataPath() + "/searchTest.lists.zip");
    private final String[] LISTS = {"CustomIndexing", "Indexed as one doc", "List To Delete", "List1", "List2", "MetaDataSet"};

    private final String PUB1_NAME = "PublishedStudy";
    private final String PUB1_DESCRIPTION = "";
    private final String[] PUB1_GROUPS = {GROUP1_NAME};
    private final String[] PUB1_DATASETS = DATASETS;
    private final String[] PUB1_DEPENDENT_DATASETS = {"APX-1: Abbreviated Physical Exam"}; // Visit dates are defined here.
    private final String[] PUB1_VISITS = {VISITS[0], VISITS[2], VISITS[3]};
    private final String[] PUB1_VIEWS = {CUSTOM_VIEW};
    private final String[] PUB1_REPORTS = {TIME_CHART, /*R_VIEW, R_VIEW2,*/ MOUSE_REPORT};
    private final String[] PUB1_LISTS = {LISTS[0], LISTS[1], LISTS[2], LISTS[3]};
    private final int PUB1_EXPECTED_SPECIMENS = 38;

    private final String PUB2_NAME = "PublishedToProject";
    private final String PUB2_DESCRIPTION = "Publish to new project";
    private final String[] PUB2_GROUPS = {}; // all
    private final String[] PUB2_DATASETS = {}; // all
    private final String[] PUB2_DEPENDENT_DATASETS = {};
    private final String[] PUB2_VISITS = {}; // all
    private final String[] PUB2_VIEWS = {}; // none;
    private final String[] PUB2_REPORTS = {TIME_CHART, /*R_VIEW, R_VIEW2,*/ MOUSE_REPORT};
    private final String[] PUB2_LISTS = {LISTS[3], LISTS[4], LISTS[5]};
    private final int PUB2_EXPECTED_SPECIMENS = 0;
    
    private final String PUB3_NAME = "PublishedNonAnon";
    private final String PUB3_DESCRIPTION = "Non-anonymized published study";
    private final String[] PUB3_GROUPS = {GROUP2_NAME, GROUP3_NAME};
    private final String[] PUB3_DATASETS = {DATASETS[0], DATASETS[1], DATASETS[2], DATASETS[4], DATASETS[5], DATASETS[6]}; // DAY 1 omitted
    private final String[] PUB3_DEPENDENT_DATASETS = {"APX-1: Abbreviated Physical Exam", "EVC-1: Enrollment Vaccination", DATASETS[7]}; // Visit dates are defined here.
    private final String[] PUB3_VISITS = {VISITS[0], VISITS[1], VISITS[2]};
    private final String[] PUB3_VIEWS = {}; // none
    private final String[] PUB3_REPORTS = {}; // none
    private final String[] PUB3_LISTS = {}; // none
    private final int PUB3_EXPECTED_SPECIMENS = 29;

    private final String SPECIMEN_ARCHIVE_B = "/sampledata/study/specimens/sample_b.specimens";
    private final String[] SPECIMEN_KEY_FIELDS = {"SpecimenNumber", "MouseId"};
    private final String[] SPECIMEN_PROTECTED_FIELDS = {"DerivativeType", "VolumeUnits"};

    private int _pipelineJobs = 0;

    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        _containerHelper.deleteProject(PUB2_NAME, afterTest, 1000000);
    }

    @Override
    protected void doCreateSteps()
    {
        // fail fast if R is not configured
        RReportHelper _rReportHelper = new RReportHelper(this);
        _rReportHelper.ensureRConfig();

        _pipelineJobs += 2;
        importStudy();
        startSpecimenImport(_pipelineJobs);
        waitForPipelineJobsToComplete(_pipelineJobs, "study import", false);

        setParticipantIdPreface(ID_PREFIX, ID_DIGITS);
        setStudyProperties(STUDY_LABEL, STUDY_INVESTIGATOR, STUDY_GRANT, STUDY_DESCRIPTION);

        // Create some mouse groups
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP1_NAME, "Mouse", true, GROUP1_PTIDS);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP2_NAME, "Mouse", true, GROUP2_PTIDS);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP3_NAME, "Mouse", true, GROUP3_PTIDS);
        _studyHelper.createCustomParticipantGroup(getProjectName(), getFolderName(), GROUP0_NAME, "Mouse", false, GROUP0_PTIDS);

        //Create Wiki
        createWiki("Test Wiki", "Test Wiki Title");

        //Add a module to find later
        enableModule("List", false);


        // Create some views and reports
        createRView(R_VIEW, REPORT_DATASET, true);
        createRView(R_VIEW_UNSHARED, REPORT_DATASET, false);
        createRView(R_VIEW2, UNPUBLISHED_REPORT_DATASET, true);
        createMouseReport(MOUSE_REPORT, TIME_CHART_MEASURE2, TIME_CHART_MEASURE1);
        createTimeChart(TIME_CHART, TIME_CHART_MEASURE2, TIME_CHART_MEASURE1);
        createCustomView(CUSTOM_VIEW, DATASETS[1], CUSTOM_VIEW_PTIDS, true);
        createCustomView(CUSTOM_VIEW2, DATASETS[2], CUSTOM_VIEW_PTIDS2, false);

        // Create some lists
        _listHelper.importListArchive(getFolderName(), LIST_ARCHIVE);

        // Set some specimen fields as protected to test exclusion with snapshot and refresh
        setSpecimenFieldsProtected(SPECIMEN_KEY_FIELDS, SPECIMEN_PROTECTED_FIELDS);

        setUnshiftedDateField(DATE_SHIFT_DATASET, UNSHIFTED_DATE_FIELD.getKey());

        clickProject(getProjectName());
        clickFolder(getFolderName());
        // Publish the study in a few different ways
        publishStudy(PUB1_NAME, PUB1_DESCRIPTION, 2, PUB1_GROUPS, PUB1_DATASETS, PUB1_VISITS, PUB1_VIEWS, PUB1_REPORTS, PUB1_LISTS, true, true);
        publishStudy(PUB2_NAME, PUB2_DESCRIPTION, 0, PUB2_GROUPS, PUB2_DATASETS, PUB2_VISITS, PUB2_VIEWS, PUB2_REPORTS, PUB2_LISTS, false, false);
        publishStudy(PUB3_NAME, PUB3_DESCRIPTION, 1, PUB3_GROUPS, PUB3_DATASETS, PUB3_VISITS, PUB3_VIEWS, PUB3_REPORTS, PUB3_LISTS, true, false, false, true, false, true);

        // load specimen set B to test the specimen refresh for the published studies
        startSpecimenImport(++_pipelineJobs, SPECIMEN_ARCHIVE_B);
    }

    @Override
    protected void doVerifySteps()
    {
        verifyPipelineJobLinks(PUB3_NAME, PUB2_NAME, PUB1_NAME);
        verifyPublishedStudy(PUB1_NAME, getProjectName(), GROUP1_PTIDS, PUB1_DATASETS, PUB1_DEPENDENT_DATASETS, PUB1_VISITS, PUB1_VIEWS, PUB1_REPORTS, PUB1_LISTS, true, true, PUB1_EXPECTED_SPECIMENS);
        verifyPublishedStudy(PUB2_NAME, PUB2_NAME, PTIDS_WITHOUT_SPECIMENS, PUB2_DATASETS, PUB2_DEPENDENT_DATASETS, PUB2_VISITS, PUB2_VIEWS, PUB2_REPORTS, PUB2_LISTS, false, false, PUB2_EXPECTED_SPECIMENS);
        // concat group 2 and group 3 ptids for the last publisehd study ptid list
        ArrayList<String> group2and3ptids = new ArrayList<>();
        group2and3ptids.addAll(Arrays.asList(GROUP2_PTIDS));
        group2and3ptids.addAll(Arrays.asList(GROUP3_PTIDS));
        verifyPublishedStudy(PUB3_NAME, getProjectName(), group2and3ptids.toArray(new String[group2and3ptids.size()]), PUB3_DATASETS, PUB3_DEPENDENT_DATASETS, PUB3_VISITS, PUB3_VIEWS, PUB3_REPORTS, PUB3_LISTS, true, false, PUB3_EXPECTED_SPECIMENS, false, true, false, true);

        verifySpecimenRefresh();
    }

    /**
     * Visit pipeline status page and check for links to published 'studies'
     * @param studies published studies in reverse creation order
     */
    private void verifyPipelineJobLinks(String... studies)
    {
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(_pipelineJobs, "Publish Study", false);

        for (int i = 0; i < studies.length; i++)
        {
            pushLocation();
            clickAndWait(Locator.linkWithText("Publish Study", i));
            assertTitleContains(studies[i]);
            popLocation();
        }
    }

    protected void verifyPublishedStudy(String name, String projectName, String[] ptids, String[] datasets, String[] dependentDatasets,
                                        String[] visits, String[] views, String[] reports, String[] lists,
                                        boolean includeSpecimens, boolean refreshSpecimens, int expectedSpecimenCount)
    {
        verifyPublishedStudy(name, projectName, ptids, datasets, dependentDatasets, visits, views, reports, lists, includeSpecimens, refreshSpecimens, expectedSpecimenCount, true, true, true, false);
    }

    @LogMethod
    protected void verifyPublishedStudy(@LoggedParam final String name, final String projectName, final String[] ptids, final String[] datasets, final String[] dependentDatasets,
                                        final String[] visits, final String[] views, final String[] reports, final String[] lists,
                                        final boolean includeSpecimens, final boolean refreshSpecimens, final int expectedSpecimenCount,
                                        final boolean removeProtected, final boolean shiftDates, final boolean alternateIDs, final boolean maskClinicNames)
    {
        SearchHelper searchHelper = new SearchHelper(this);
        // Verify alternate IDs (or lack thereof)
        for (String ptid : ptids)
        {
            searchHelper.searchForSubjects(ptid);
            if (alternateIDs)
                assertFalse("Published study contains non-alternate ID: " + ptid, getText(Locator.id("searchResults")).contains(name));
            else
                assertTrue("Published study doesn't contain ID: " + ptid, getText(Locator.id("searchResults")).contains(name));
        }

        // Go to published study
        hoverProjectBar();
        if (isElementPresent(Locator.linkWithText(name)))
            clickProject(name);
        else
        {
            clickProject(getProjectName());
            clickFolder(name);
        }

        //Assert webparts/wikis are present
        waitForElement(Locator.xpath("//table[@name='webpart']"));
        assert(getXpathCount(Locator.xpath("//table[@name='webpart']")) == 7);
        assertTextPresent("Test Wiki Title");

        //assert the added module is present
        waitForElement(Locator.xpath("//span[@id='adminMenuPopupText']"));
        click(Locator.xpath("//span[@id='adminMenuPopupText']"));
        waitForElement(Locator.xpath("//span[text()='Go To Module']"));
        mouseOver(Locator.xpath("//span[text()='Go To Module']"));
        waitForElement(Locator.xpath("//span[text()='List']"));

        // Verify published participant count
        clickAndWait(Locator.linkWithText("Mice"));
        waitForElement(Locator.xpath("//*[contains(@class, 'lk-filter-panel-label') and text()='All']"));
        _ext4Helper.deselectAllParticipantFilter();
        waitForText("No matching");
        _ext4Helper.selectAllParticipantFilter();
        waitForElement(Locator.id("participantsDiv1.status").withText("Found "+ptids.length+" mice of "+ptids.length+"."));
        if (alternateIDs)
            assertTextNotPresent(ptids);
        else
            assertTextPresent(ptids);

        //Verify Cohorts present
        goToManageStudy();
        waitForText("This study defines 2 cohorts");

        // Verify correct published datasets
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        if (datasets.length > 0)
            assertEquals("Unexpected number of datasets", datasets.length + dependentDatasets.length, getXpathCount(Locator.xpath("//td[contains(@class, 'datasets')]//tr")) - 1);
        else // All visits were published
            assertEquals("Unexpected number of datasets", datasetCount, getXpathCount(Locator.xpath("//td[contains(@class, 'datasets')]//tr")) - 1);
        for (String dataset: datasets)
        {
            pushLocation();
            clickAndWait(Locator.linkWithText(dataset));
            clickButton("View Data");
            if (alternateIDs)
                assertTextNotPresent(ptids);
            popLocation();
        }
        for (String dataset: dependentDatasets)
        {
            pushLocation();
            clickAndWait(Locator.linkWithText(dataset));
            clickButton("View Data");
            if (alternateIDs)
                assertTextNotPresent(ptids);
            popLocation();
        }

        //Verify correct published visits
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Visits"));
        if (visits.length > 0)
            assertEquals("Unexpected number of visits", visits.length, getXpathCount(Locator.xpath("//table[@id = 'visits']/tbody/tr")) - 1);
        else // All visits were published
            assertEquals("Unexpected number of visits", visitCount, getXpathCount(Locator.xpath("//table[@id = 'visits']/tbody/tr")) - 1);
        for (String visit: visits)
        {
            assertTextPresent(visit);
        }

        if (Arrays.asList(visits).contains(DATE_SHIFT_REQUIRED_VISIT))
        {
            log("Verify expected date shifting");
            goToQueryView("study", DATE_SHIFT_DATASET, false);
            Set<String> unshiftedDates = new HashSet<>();
            Set<String> shiftedDates = new HashSet<>();
            DataRegionTable datasetTable = new DataRegionTable("query", this, true, true);
            unshiftedDates.addAll(datasetTable.getColumnDataAsText(UNSHIFTED_DATE_FIELD.getValue()));
            shiftedDates.addAll(datasetTable.getColumnDataAsText(SHIFTED_DATE_FIELD.getValue()));
            assertTrue("Column '" + UNSHIFTED_DATE_FIELD.getValue() + "' should not be shifted", unshiftedDatesByStudy.get(name).containsAll(unshiftedDates));

            if (shiftDates)
            {
                assertFalse("Column '" + SHIFTED_DATE_FIELD.getValue() + "' should be shifted", preshiftedDatesByStudy.get(name).containsAll(shiftedDates));
            }
            else
            {
                assertTrue("Column '" + SHIFTED_DATE_FIELD.getValue() + "' should not be shifted", preshiftedDatesByStudy.get(name).containsAll(shiftedDates));
            }
        }
        else if (visits.length > 0)
        {
            fail("Test error: Please export visit [" + DATE_SHIFT_REQUIRED_VISIT + "] to allow verification of date shifting");
        }

        if (reports.length > 0 || views.length > 0)
        {
            goToManageViews();
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
            if(reports.length > 0)
            {
                waitForText(reports[0]);
            }
            else
            {
                waitForText(views[0]);
            }

            String viewXpath = "//tr[contains(@class, 'x4-grid-tree-node-leaf')]";
            assertEquals("Unexpected number of views/reports", views.length + reports.length, getXpathCount(Locator.xpath(viewXpath)));
        }

        // Verify published reports/views
        if (reports.length > 0)
        {
            assertTextPresent(reports);
            for (final String report : reports)
            {
                pushLocation();
                clickAndWait(Locator.linkWithText(report));

                waitFor(new Checker()
                    {
                        public boolean check()
                        {
                            return isTextPresent(report) || isTextPresent("Table or query not found") || getResponseCode() == 404;
                        }
                    }, "View did not load: " + report, WAIT_FOR_JAVASCRIPT);

                if (isTextPresent(report))
                {
                    _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
                    if (alternateIDs)
                        assertTextNotPresent(ptids);
                    else
                        assertTextPresent(ptids);

                    if (isTextPresent("Time Chart"))
                    {
                        waitForText("RCH-1: Reactogenicity-Day 1, RCE-1: Reactogenicity-Day 0"); // chart title
                        waitForElement(Locator.css("svg g a path"));
                    }
                }
                else
                {
                    for (String dataset : datasets)
                    {
                        assertTextNotPresent(dataset);
                    }
                }

                popLocation();
                _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
                waitForText(reports[0]);
            }


        }

        if(views.length > 0)
        {
            assertTextPresent(views);
            for (final String view : views)
            {
                // Verify the views.
                pushLocation();
                clickAndWait(Locator.linkWithText(view));

                // Do some sort of check.
                assertTextPresent(view);

                popLocation();
                _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
                waitForText(views[0]);
            }
        }

        // Verify published lists
        if (lists.length > 0)
        {
            goToModule("List");
            assertTextPresent(lists);
            assertEquals("Unexpected number of lists", lists.length, getXpathCount(Locator.xpath("id('lists')//tr")));
            for (String list : lists)
            {
                pushLocation();
                clickAndWait(Locator.xpath("id('lists')//tr[./td[normalize-space() = '"+list+"']]/td[2]/a"));
                assertTitleContains(list);
                popLocation();
            }
        }

        // Verify published specimens
        clickAndWait(Locator.linkWithText("Specimen Data"));
        if (includeSpecimens)
        {
            waitForText("By Vial Group");
            clickAndWait(Locator.linkWithText("By Individual Vial"));
            waitForElement(Locator.paginationText(expectedSpecimenCount));

            // verify that the alternate IDs are used
            if (alternateIDs)
                assertTextPresent("PUBLISHED-", 2*expectedSpecimenCount); // once for mouse ID link and once for mouse ID display

            //TODO: verify date shifting

            // verify protected specimen fields were removed
            DataRegionTable t1 = new DataRegionTable("SpecimenDetail", this);
            for (String field : SPECIMEN_PROTECTED_FIELDS)
            {
                t1.setFilter(field, (removeProtected ? "Is Not Blank" : "Is Blank"), null);
                assertTextPresent("No data to show.");
                t1.clearFilter(field);
            }

            // verify that the vials are filtered by the correct ptids and visits
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.showHiddenItems();
            _customizeViewsHelper.addCustomizeViewColumn("SequenceNum");
            _customizeViewsHelper.applyCustomView();
            DataRegionTable t2 = new DataRegionTable("SpecimenDetail", this);
            if (!alternateIDs) // we only know the IDs if they are not alternateIDs
            {
                t2.setFilter("MouseId", "Does Not Equal Any Of (e.g. \"a;b;c\")", createOneOfFilterString(ptids));
                assertTextPresent("No data to show.");
                t2.clearFilter("MouseId");
            }
            t2.setFilter("SequenceNum", "Does Not Equal Any Of (e.g. \"a;b;c\")", createOneOfFilterString(visits));
            assertTextPresent("No data to show.");
            t2.clearFilter("SequenceNum");

            // verify that the request related specimen reports are hidden
            clickAndWait(Locator.linkWithText("Specimen Data"));
            waitAndClick(Locator.tagContainingText("span", "Specimen Reports")); // expand
            waitAndClickAndWait(Locator.linkWithText("View Available Reports"));
            assertTextNotPresent("Requested Vials by Type and Timepoint");
            assertElementPresent(Locator.linkWithText("show options"), 6);
        }
        else
        {
            waitForText("No specimens found.", WAIT_FOR_JAVASCRIPT);
        }
        // verify that the specimen request options are hidden from the manage study page
        goToManageStudy();
        assertTextNotPresent("Specimen Repository Settings", "Repository Type", "Display and Behavior", "Specimen Request Settings");
        assertTextPresent("NOTE: specimen repository and request settings are not available for ancillary or published studies.");
        // verify that the additive, derivative, etc. tables were populated correctly
        goToQueryView("study", "SpecimenAdditive", false);
        assertElementPresent(includeSpecimens ? Locator.paginationText(1, 42, 42) : Locator.xpath("//tr/td/em[text() = 'No data to show.']"));
        beginAt(getCurrentRelativeURL().replace("SpecimenAdditive", "SpecimenDerivative"));
        assertElementPresent(includeSpecimens ? Locator.paginationText(1, 99, 99) : Locator.xpath("//tr/td/em[text() = 'No data to show.']"));
        beginAt(getCurrentRelativeURL().replace("SpecimenDerivative", "SpecimenPrimaryType"));
        assertElementPresent(includeSpecimens ? Locator.paginationText(1, 59, 59) : Locator.xpath("//tr/td/em[text() = 'No data to show.']"));
        beginAt(getCurrentRelativeURL().replace("SpecimenPrimaryType", "Location"));
        assertElementPresent(includeSpecimens ? Locator.paginationText(1, 24, 24) : Locator.xpath("//tr/td/em[text() = 'No data to show.']"));

        // verify masked clinic information
        if (maskClinicNames)
        {
            verifyMaskedClinics(8);
        }
        goToProjectHome();
    }

    private void verifySpecimenRefresh()
    {
        // trigger the system maintenance task to refresh study snapshot specimen data
        startSystemMaintenance("Refresh study snapshot specimen data");
        waitForSystemMaintenanceCompletion();

        // verify specimen refresh for PUB1 study
        goToProjectHome();
        clickFolder(PUB1_NAME);
        clickAndWait(Locator.linkWithText("Specimen Data"));
        clickAndWait(Locator.linkWithText("By Individual Vial"));
        waitForElement(Locator.paginationText(37)); // updated number of total specimens
        assertTextNotPresent("BAQ00051-10"); // GUID removed via refresh
        assertTextPresent("PUBLISHED-", 2*37); // transformations still applied
        DataRegionTable table = new DataRegionTable("SpecimenDetail", this);
        table.setFilter("Clinic", "Is Not Blank", null); // Clinic column should be empty with refresh
        assertTextPresent("No data to show.");
        table.clearFilter("Clinic");

        // verify that specimens were not refreshed for PUB3 study
        goToProjectHome();
        clickFolder(PUB3_NAME);
        clickAndWait(Locator.linkWithText("Specimen Data"));
        clickAndWait(Locator.linkWithText("By Individual Vial"));
        table = new DataRegionTable("SpecimenDetail", this);
        table.setFilter("Clinic", "Is Blank", null); // Clinic column should NOT be empty
        assertTextPresent("No data to show.");
        table.clearFilter("Clinic");
    }

    private void publishStudy(String name, String description, int rootProjectOrFolder, String[] groups, String[] datasets,
                              String[] visits, String[] views, String[] reports, String[] lists, boolean includeSpecimens, boolean refreshSpecimens)
    {
        publishStudy(name, description, rootProjectOrFolder, groups, datasets, visits, views, reports, lists, includeSpecimens, refreshSpecimens, true, true, true, false);
    }

    // Test should be in an existing study.
    @LogMethod
    private void publishStudy(@LoggedParam String name, String description, int rootProjectOrFolder, String[] groups, String[] datasets,
                              String[] visits, String[] views, String[] reports, String[] lists, boolean includeSpecimens, boolean refreshSpecimens,
                              boolean removeProtected, boolean shiftDates, boolean alternateIDs, boolean maskClinicNames)
    {
        pushLocation();
        waitAndClickAndWait(Locator.linkWithText(DATE_SHIFT_DATASET_LABEL));
        Set<String> unshiftedDates = new HashSet<>();
        Set<String> preshiftedDates = new HashSet<>();
        if (groups != null && groups.length > 0)
        {
            for (String group : groups)
            {
                _extHelper.clickMenuButton(true, "Mouse Groups", group);
                DataRegionTable datasetTable = new DataRegionTable("Dataset", this, true, true);
                unshiftedDates.addAll(datasetTable.getColumnDataAsText(UNSHIFTED_DATE_FIELD.getValue()));
                preshiftedDates.addAll(datasetTable.getColumnDataAsText(SHIFTED_DATE_FIELD.getValue()));
            }
        }
        else
        {
            DataRegionTable datasetTable = new DataRegionTable("Dataset", this, true, true);
            unshiftedDates.addAll(datasetTable.getColumnDataAsText(UNSHIFTED_DATE_FIELD.getValue()));
            preshiftedDates.addAll(datasetTable.getColumnDataAsText(SHIFTED_DATE_FIELD.getValue()));
        }
        unshiftedDatesByStudy.put(name, unshiftedDates);
        preshiftedDatesByStudy.put(name, preshiftedDates);
        popLocation();

        pushLocation();
        clickTab("Manage");

        log("Publish study.");
        clickButton("Publish Study", 0);
        _extHelper.waitForExtDialog("Publish Study");

        // Wizard page 1 : General Setup
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'General Setup']"));
        setFormElement(Locator.name("studyName"), name);
        setFormElement(Locator.name("studyDescription"), description);
        assertTrue(PROTOCOL_DOC.exists());
        setFormElement(Locator.name("protocolDoc"), PROTOCOL_DOC);
        selectLocation(rootProjectOrFolder);
        clickButton("Next", 0);

        // Wizard page 2 : Mice
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Mice']"));
        waitForElement(Locator.css(".studyWizardParticipantList"));
        if (groups != null && groups.length > 0)
        {
            clickButton("Next", 0);
            _extHelper.waitForExtDialog("Error");
            assertTextPresent("You must select at least one Mouse group.");
            _extHelper.clickExtButton("Error", "OK", 0);
            checkRadioButton("renderType", "existing");
            for (String group : groups)
            {
                _extHelper.selectExtGridItem("label", group, -1, "studyWizardParticipantList", true);
            }
            assertTextNotPresent(GROUP0_NAME); // Unshared groups shouldn't show up here
        }
        else
        {
            checkRadioButton("renderType", "all");
        }
        clickButton("Next", 0);

        // Wizard page 3 : Datasets
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Datasets']"));
        waitForElement(Locator.css(".studyWizardDatasetList"));
        for (String dataset : datasets)
        {
            _extHelper.selectExtGridItem("Label", dataset, -1, "studyWizardDatasetList", true);
        }
        if (datasets.length == 0) // select all
            click(Locator.css(".studyWizardDatasetList .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 4 : Visits
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Visits']"));
        waitForElement(Locator.css(".studyWizardVisitList"));
        clickButton("Next", 0);
        _extHelper.waitForExtDialog("Error");
        assertTextPresent("You must select at least one visit.");
        _extHelper.clickExtButton("Error", "OK", 0);
        for (String visit : visits)
        {
            _extHelper.selectExtGridItem("SequenceNumMin", visit, -1, "studyWizardVisitList", true);
        }
        if (visits.length == 0) // select all
            click(Locator.css(".studyWizardVisitList .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 5 : Specimens
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Specimens']"));
        if (!includeSpecimens) uncheckCheckbox(Locator.name("includeSpecimens"));
        if (refreshSpecimens) checkRadioButton("specimenRefresh", "true");
        clickButton("Next", 0);

        // Wizard Page 6 : Study Objects
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Study Objects']"));
        click(Locator.css(".studyObjects .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 7 : Lists
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Lists']"));
        waitForElement(Locator.css(".studyWizardListList"));
        for (String list : lists)
        {
            _extHelper.selectExtGridItem("name", list, -1, "studyWizardListList", true);
        }
        clickButton("Next", 0);

        // Wizard page 8 : Views
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Views']"));
        waitForElement(Locator.css(".studyWizardViewList"));
        assertTextNotPresent(R_VIEW_UNSHARED);
        for (String view : views)
        {
            _extHelper.selectExtGridItem("name", view, -1, "studyWizardViewList", true);
        }
        clickButton("Next", 0);

        // Wizard Page 9 : Reports
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Reports']"));
        waitForElement(Locator.css(".studyWizardReportList"));
        for (String report : reports)
        {
            waitForElement(Locator.css(".studyWizardReportList .x-grid3-col-1")); // Make sure grid is filled in
            _extHelper.selectExtGridItem("name", report, -1, "studyWizardReportList", true);
        }
        clickButton("Next", 0);

        // Wizard page 10 : Folder Objects
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Folder Objects']"));
        click(Locator.css(".folderObjects .x-grid3-hd-checker  div"));
        clickButton("Next", 0);

        // Wizard page 11 : Publish Options
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Publish Options']"));
        if (!removeProtected) uncheckCheckbox(Locator.name("removeProtected"));
        if (!shiftDates) uncheckCheckbox(Locator.name("shiftDates"));
        if (!alternateIDs) uncheckCheckbox(Locator.name("alternateids"));
        if (maskClinicNames) checkCheckbox(Locator.name("maskClinic"));
        clickButton("Finish");

        _pipelineJobs++;

        if (rootProjectOrFolder == 0)
            _containerHelper.addCreatedProject(name); // Add to list so that it will be deleted during cleanup

        popLocation();
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";
    // Starting at an existing data grid
    private void createTimeChart(String name, String[]... datasetMeasurePairs)
    {
        goToManageViews();
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Time Chart");

        waitForElement(Locator.ext4Button("Choose a Measure"), WAIT_FOR_JAVASCRIPT);
        click(Locator.ext4Button("Choose a Measure"));
        _extHelper.waitForExtDialog("Add Measure...");
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        String measureXpath = _extHelper.getExtDialogXPath("Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+datasetMeasurePairs[0][1]+"')]]";
        waitForElement(Locator.xpath("(" + measureXpath + ")[2]"));
        setFormElement(Locator.name("filterSearch"), datasetMeasurePairs[0][0]);
        waitForElementToDisappear(Locator.xpath("(" + measureXpath + ")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates
        waitForElement(Locator.xpath(measureXpath));
        click(Locator.xpath(measureXpath));
        click(Locator.ext4Button("Select"));
        waitForText("No data found for the following measures/dimensions", WAIT_FOR_JAVASCRIPT);

        if (datasetMeasurePairs.length > 1)
        {
            clickButton("Measures", 0);
            waitForText("Divide data into Series");
            waitForElement(Locator.ext4Button("Add Measure"));
            for (int i = 1; i < datasetMeasurePairs.length; i++)
            {
                click(Locator.ext4Button("Add Measure"));
                _extHelper.waitForExtDialog("Add Measure...");
                _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

                measureXpath = _extHelper.getExtDialogXPath("Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+datasetMeasurePairs[i][1]+"')]]";
                waitForElement(Locator.xpath("("+measureXpath+")[2]"));
                setFormElement(Locator.name("filterSearch", i), datasetMeasurePairs[i][0]);
                waitForElementToDisappear(Locator.xpath("("+measureXpath+")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates
                waitForElement(Locator.xpath(measureXpath));
                click(Locator.xpath(measureXpath));
                click(Locator.ext4Button("Select"));

                waitForText(datasetMeasurePairs[i][1] + " from " + datasetMeasurePairs[i][0]);
            }
            click(Locator.ext4Button("OK"));
        }

        _ext4Helper.checkGridRowCheckbox("All");
        _ext4Helper.uncheckGridRowCheckbox("All");
        waitForText("No mouse selected. Please select at least one mouse.");
        for (String mouseId : GROUP1_PTIDS)
        {
            // Select all of the Mice in GROUP1
            _ext4Helper.checkGridRowCheckbox(mouseId);
        }
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT); // Make sure charts are rendered
        waitForText("No calculated interval values");

        // Add point click function
        clickButton("Developer", 0);
        waitForElement(Locator.ext4Button("Cancel"));
        clickButton("Enable", 0);
        setFormElement("point-click-fn-textarea", getFileContents(TEST_DATA_API_PATH + "/timeChartPointClickTestFn.js"));
        waitAndClick(Locator.ext4Button("OK"));
        _ext4Helper.waitForMaskToDisappear();

        // Visit-based chart
        waitForElement(Locator.css("svg text").containing("Days Since Contact Date"));
        fireEvent(Locator.css("svg text").containing("Days Since Contact Date").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT), SeleniumEvent.click);
        waitForElement(Locator.ext4Button("Cancel")); // Axis label windows always have a cancel button. It should be the only one on the page
        _ext4Helper.selectRadioButton("Chart Type:", "Visit Based Chart");
        waitAndClick(Locator.ext4Button("OK"));
        waitForElement(Locator.css("svg text").containing("Visit"));

        clickButton("Save", 0);
        waitForText("Viewable By");
        setFormElement(Locator.name("reportName"), name);

        clickButtonByIndex("Save", 1);
    }

    private static final String ADD_MEASURE_TITLE = "Add Measure";
    private void createMouseReport(String reportName, String[]... datasetMeasurePairs)
    {
        goToManageViews();
        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "Mouse Report");
        waitAndClickButton("Choose Measures", 0);
        _extHelper.waitForExtDialog(ADD_MEASURE_TITLE);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        for (String[] pair : datasetMeasurePairs)
        {
            _extHelper.setExtFormElementByType(ADD_MEASURE_TITLE, "text", pair[0]);
//            pressEnter(_extHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");
            String measureXpath = _extHelper.getExtDialogXPath(ADD_MEASURE_TITLE) + "//table/tbody/tr[not(contains(@class, 'x4-grid-row-selected'))]/td[div[starts-with(text(), '"+ pair[1]+"')]]";
            waitForElement(Locator.xpath(measureXpath), WAIT_FOR_JAVASCRIPT); // Make sure measure has appeared
            waitForElementToDisappear(Locator.xpath("("+measureXpath+")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates

            _extHelper.clickX4GridPanelCheckbox("label", pair[1], "measuresGridPanel", true);
        }

        clickButton("Select", 0);

        _extHelper.setExtFormElementByLabel("Report Name", reportName);
        clickButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
    }

    private void createRView(String name, String dataset, boolean shareView)
    {
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(dataset));
        clickButton("View Data");
        _customizeViewsHelper.createRView(null, name, shareView);
    }

    private void createWiki(String name, String title)
    {
        goToProjectHome();
        clickFolder(getFolderName());
        addWebPart("Wiki");
        waitForElement(Locator.xpath("//a[text()='Create a new wiki page']"));
        click(Locator.xpath("//a[text()='Create a new wiki page']"));
        waitForElement(Locator.xpath("//input[@name='name']"));
        setFormElement(Locator.xpath("//input[@name='name']"), name);
        setFormElement(Locator.xpath("//input[@name='title']"), title);
        clickButton("Save & Close");
    }

    private String createOneOfFilterString(String[] values)
    {
        String filterStr = "";
        for (String val : values)
        {
            if(!filterStr.equals(""))
                filterStr = filterStr + ";";

            filterStr = filterStr + val;
        }
        return filterStr;
    }

    private void createCustomView(String name, String dataset, String[] ptids, boolean shared)
    {
        String ptidFilter = createOneOfFilterString(ptids);

        clickProject(getProjectName());
        clickFolder(getFolderName());

        clickAndWait(Locator.linkWithText(dataset));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewFilter("MouseId", "Mouse Id", "Equals One Of", ptidFilter);
        _customizeViewsHelper.saveCustomView(name, shared);
    }

    /**
     * Selects the destination for study publication
     * @param rootProjectOrFolder => 0 - root, 1 - project, 2 - folder
     */
    private void selectLocation(int rootProjectOrFolder)
    {
        clickButton("Change", 0);
        sleep(1000); // sleep while the tree expands

        if (rootProjectOrFolder == 0)
        {
            Locator rootTreeNode = Locator.tagWithClass("a", "x-tree-node-anchor").withDescendant(Locator.tagWithText("span", "LabKey Server Projects"));
            doubleClick(rootTreeNode);
        }
        else if (rootProjectOrFolder == 1)
        {
            Locator projectTreeNode = Locator.tagWithClass("a", "x-tree-node-anchor").withDescendant(Locator.tagWithText("span", getProjectName()));
            doubleClick(projectTreeNode);
        }
        else
        {
            // noop : subfolder already selected by default
        }
    }

    // Test should be in an existing study.
    private void setStudyProperties(String label, String investigator, String grant, String description)
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Change Study Properties"));

        waitForElement(Locator.name("Label"));
        setFormElement(Locator.name("Label"), label);
        setFormElement(Locator.name("Investigator"), investigator);
        setFormElement(Locator.name("Grant"), grant);
        setFormElement(Locator.name("Description"), description);

        clickButton("Submit");
    }

    // Test should be in an existing study
    private void setSpecimenFieldsProtected(String[] keyFields, String[] protectedFields)
    {
        goToQueryView("study", "SpecimenEvent", true);
        List<String> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(keyFields));
        fields.addAll(Arrays.asList(protectedFields));
        for (String field : fields)
        {
            click(Locator.tagContainingText("div", field));
            click(Locator.tagContainingText("span", "Additional Properties"));
            checkCheckbox(Locator.name("protected"));
        }
        clickButton("Save", 0);
        waitForText("Save successful.");
    }

    private void setUnshiftedDateField(String dataset, String fieldName)
    {
        goToQueryView("study", dataset, true);

        click(Locator.tagContainingText("div", fieldName));
        checkCheckbox(Locator.name("excludeFromShifting"));

        clickButton("Save", 0);
        waitForText("Save successful.");
    }

    private void goToQueryView(String schema, String query, boolean viewMetadata)
    {
        goToSchemaBrowser();
        sleep(1000); // TODO: why is it having issues selecting the schema from the tree?
        if (!viewMetadata)
        {
            viewQueryData(schema, query);
            waitForElement(Locator.css(".labkey-data-region"));
        }
        else
        {
            selectQuery(schema, query);
            waitForText("edit metadata");
            clickAndWait(Locator.linkContainingText("edit metadata"));
            waitForElement(Locator.xpath("id('org.labkey.query.metadata.MetadataEditor-Root')//td[contains(@class, 'labkey-wp-title-left')]").withText("Metadata Properties"));
        }
    }
}
