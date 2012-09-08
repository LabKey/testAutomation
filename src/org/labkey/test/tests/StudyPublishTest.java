package org.labkey.test.tests;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.SearchHelper;
import org.labkey.test.util.StudyHelper;

import java.io.File;

/**
 * User: tchadick
 * Date: 8/24/12
 * Time: 11:00 AM
 */
public class StudyPublishTest extends StudyProtectedExportTest
{
    private final String ID_PREFIX = "PUBLISHED-";
    private final int ID_DIGITS = 8;
    private final File PROTOCOL_DOC = new File( getLabKeyRoot() + getStudySampleDataPath() + "/Protocol.txt");
    private final String STUDY_LABEL = "Original Study";
    private final String STUDY_INVESTIGATOR = "Original Investigator";
    private final String STUDY_GRANT = "Original Grant";
    private final String STUDY_DESCRIPTION = "This is the study we start with.";
    private final int STUDY_VISIT_COUNT = 65;
    private final int STUDY_DATASET_COUNT = 47;

    private final String GROUP0_NAME = "Unshared Group";
    private final String[] GROUP0_PTIDS = {"1234"};
    private final String GROUP1_NAME = "Group 1";
    private final String[] GROUP1_PTIDS = {"999321029", "999320016", "999320485", "999320518", "999320529", "999320533"};
    private final String GROUP2_NAME = "Group 2";
    private final String[] GROUP2_PTIDS = {"999320541", "999320557", "999320565", "999320576", "999320582", "999320590"};
    private final String GROUP3_NAME = "Group 3";
    private final String[] GROUP3_PTIDS = {"999320609", "999320613", "999320624", "999320638", "999320646", "999320652"};

    private final String[] DATASETS = {"RCB-1: Reactogenicity-Baseline", "RCM-1: Reactogenicity-Early Assessment", "RCE-1: Reactogenicity-Day 0", "RCH-1: Reactogenicity-Day 1", "RCF-1: Reactogenicity-Day 2", "RCT-1: Reactogenicity-Day 3", "RCP-1: Reactogenicity-Resolution"};
    private final String REPORT_DATASET = "RCB-1: Reactogenicity-Baseline";
    private final String UNPUBLISHED_REPORT_DATASET = "AE-1:(VTN) AE Log";
    private final String[] VISITS = {"201", "601", "1301"};
    private final String R_VIEW = "Shared R View";
    private final String R_VIEW2 = "R View on Unpublished Dataset";
    private final String R_VIEW_UNSHARED = "Unshared R View";
    private final String MOUSE_REPORT = "Mouse Report";
    private final String TIME_CHART = "Time Chart";
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
    private final String PUB1_CONTAINER = "/" + getProjectName() + "/" + getFolderName();
    private final String[] PUB1_GROUPS = {GROUP1_NAME};
    private final String[] PUB1_DATASETS = DATASETS;
    private final String[] PUB1_DEPENDENT_DATASETS = {"APX-1: Abbreviated Physical Exam"}; // Visit dates are defined here.
    private final String[] PUB1_VISITS = {VISITS[1], VISITS[2]};
    private final String[] PUB1_VIEWS = {CUSTOM_VIEW};
    private final String[] PUB1_REPORTS = {TIME_CHART, /*R_VIEW, R_VIEW2,*/ MOUSE_REPORT};
    private final String[] PUB1_LISTS = {LISTS[0], LISTS[1], LISTS[2], LISTS[3]};

    private final String PUB2_NAME = "PublishedToProject";
    private final String PUB2_DESCRIPTION = "Publish to new project";
    private final String PUB2_CONTAINER = "/";
    private final String[] PUB2_GROUPS = {}; // all
    private final String[] PUB2_DATASETS = {}; // all
    private final String[] PUB2_DEPENDENT_DATASETS = {};
    private final String[] PUB2_VISITS = {}; // all
    private final String[] PUB2_VIEWS = {}; // none;
    private final String[] PUB2_REPORTS = {TIME_CHART, /*R_VIEW, R_VIEW2,*/ MOUSE_REPORT};
    private final String[] PUB2_LISTS = {LISTS[3], LISTS[4], LISTS[5]};
    
    private final String PUB3_NAME = "PublishedNonAnon";
    private final String PUB3_DESCRIPTION = "Non-anonymized published study";
    private final String PUB3_CONTAINER = "/" + getProjectName();
    private final String[] PUB3_GROUPS = {GROUP2_NAME, GROUP3_NAME};
    private final String[] PUB3_DATASETS = {DATASETS[0], DATASETS[1], DATASETS[2], DATASETS[4], DATASETS[5], DATASETS[6]}; // DAY 1 omitted
    private final String[] PUB3_DEPENDENT_DATASETS = {"APX-1: Abbreviated Physical Exam", "EVC-1: Enrollment Vaccination"}; // Visit dates are defined here.
    private final String[] PUB3_VISITS = {VISITS[0], VISITS[1]};
    private final String[] PUB3_VIEWS = {}; // none
    private final String[] PUB3_REPORTS = {}; // none
    private final String[] PUB3_LISTS = {}; // none

    private int _pipelineJobs = 0;

    public void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
        deleteProject(PUB2_NAME);
        super.doCleanup();
    }

    @Override
    protected void doCreateSteps()
    {
        // fail fast if R is not configured
        RReportHelper.ensureRConfig(this);

        importStudy();
        goToModule("Pipeline");
        waitForPipelineJobsToComplete(++_pipelineJobs, "study import", false);

        setParticipantIdPreface(ID_PREFIX, ID_DIGITS);
        setStudyProperties(STUDY_LABEL, STUDY_INVESTIGATOR, STUDY_GRANT, STUDY_DESCRIPTION);

        // Create some mouse groups
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), GROUP1_NAME, "Mouse", true, GROUP1_PTIDS);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), GROUP2_NAME, "Mouse", true, GROUP2_PTIDS);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), GROUP3_NAME, "Mouse", true, GROUP3_PTIDS);
        StudyHelper.createCustomParticipantGroup(this, getProjectName(), getFolderName(), GROUP0_NAME, "Mouse", false, GROUP0_PTIDS);

        // Create some views and reports
        createRView(R_VIEW, REPORT_DATASET, true);
        createRView(R_VIEW_UNSHARED, REPORT_DATASET, false);
        createRView(R_VIEW2, UNPUBLISHED_REPORT_DATASET, true);
        createMouseReport(MOUSE_REPORT, TIME_CHART_MEASURE2, TIME_CHART_MEASURE1);
        createTimeChart(TIME_CHART, TIME_CHART_MEASURE2, TIME_CHART_MEASURE1);
        createCustomView(CUSTOM_VIEW, DATASETS[1], CUSTOM_VIEW_PTIDS, true);
        createCustomView(CUSTOM_VIEW2, DATASETS[2], CUSTOM_VIEW_PTIDS2, false);

        // Create some lists
        ListHelper.importListArchive(this, getFolderName(), LIST_ARCHIVE);

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        // Publish the study in a few different ways
        publishStudy(PUB1_NAME, PUB1_DESCRIPTION, PUB1_CONTAINER, PUB1_GROUPS, PUB1_DATASETS, PUB1_VISITS, PUB1_VIEWS, PUB1_REPORTS, PUB1_LISTS);
        publishStudy(PUB2_NAME, PUB2_DESCRIPTION, PUB2_CONTAINER, PUB2_GROUPS, PUB2_DATASETS, PUB2_VISITS, PUB2_VIEWS, PUB2_REPORTS, PUB2_LISTS);
        publishStudy(PUB3_NAME, PUB3_DESCRIPTION, PUB3_CONTAINER, PUB3_GROUPS, PUB3_DATASETS, PUB3_VISITS, PUB3_VIEWS, PUB3_REPORTS, PUB3_LISTS, false, false, false);
    }

    @Override
    protected void doVerifySteps()
    {
        verifyPipelineJobLinks(PUB3_NAME, PUB2_NAME, PUB1_NAME);
        verifyPublishedStudy(PUB1_NAME, GROUP1_PTIDS, PUB1_DATASETS, PUB1_DEPENDENT_DATASETS, PUB1_VISITS, PUB1_VIEWS, PUB1_REPORTS, PUB1_LISTS);
        verifyPublishedStudy(PUB2_NAME, GROUP2_PTIDS, PUB2_DATASETS, PUB2_DEPENDENT_DATASETS, PUB2_VISITS, PUB2_VIEWS, PUB2_REPORTS, PUB2_LISTS);
        verifyPublishedStudy(PUB3_NAME, GROUP3_PTIDS, PUB3_DATASETS, PUB3_DEPENDENT_DATASETS, PUB3_VISITS, PUB3_VIEWS, PUB3_REPORTS, PUB3_LISTS, false, false, false);
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
            clickLinkWithText("Publish Study", i);
            assertTitleContains(studies[i]);
            popLocation();
        }
    }

    protected void verifyPublishedStudy(String name, String[] ptids, String[] datasets, String[] dependentDatasets, String[] visits, String[] views, String[] reports, String[] lists)
    {
        verifyPublishedStudy(name, ptids, datasets, dependentDatasets, visits, views, reports, lists, true, true, true);
    }

    protected void verifyPublishedStudy(final String name, final String[] ptids, final String[] datasets, final String[] dependentDatasets, final String[] visits, final String[] views, final String[] reports, final String[] lists, final boolean removeProtected, final boolean shiftDates, final boolean alternateIDs)
    {
        // Verify alternate IDs (or lack thereof)
        for (String ptid : ptids)
        {
            SearchHelper.searchForSubjects(this, ptid);
            if (alternateIDs)
                assertFalse("Published study contains non-alternate ID: " + ptid, getText(Locator.id("searchResults")).contains(name));
            else
                assertTrue("Published study doesn't contain ID: " + ptid, getText(Locator.id("searchResults")).contains(name));
        }

        // Go to published study
        clickLinkWithText(getProjectName());
        clickLinkWithText(name);

        // Verify published participant count
        clickLinkWithText("Mice");
        // TODO: 15978: Too many participants published with study
        // assertTextPresent("Showing all "+ptids.length+" mice.");
        if (alternateIDs)
            assertTextNotPresent(ptids);
        else
            assertTextPresent(ptids);

        // Verify correct published datasets
        goToManageStudy();
        clickLinkWithText("Manage Datasets");
        if (datasets.length > 0)
            assertEquals("Unexpected number of datasets", datasets.length + dependentDatasets.length, getXpathCount(Locator.xpath("//td[contains(@class, 'datasets')]//tr")) - 1);
        else // All visits were published
            assertEquals("Unexpected number of datasets", STUDY_DATASET_COUNT, getXpathCount(Locator.xpath("//td[contains(@class, 'datasets')]//tr")) - 1);
        for (String dataset: datasets)
        {
            pushLocation();
            clickLinkWithText(dataset);
            clickButton("View Data");
            if (alternateIDs)
                assertTextNotPresent(ptids);
            popLocation();
        }
        for (String dataset: dependentDatasets)
        {
            pushLocation();
            clickLinkWithText(dataset);
            clickButton("View Data");
            if (alternateIDs)
                assertTextNotPresent(ptids);
            popLocation();
        }

        //Verify correct published visits
        goToManageStudy();
        clickLinkWithText("Manage Visits");
        if (visits.length > 0)
            assertEquals("Unexpected number of visits", visits.length, getXpathCount(Locator.xpath("//table[@id = 'visits']/tbody/tr")) - 1);
        else // All visits were published
            assertEquals("Unexpected number of visits", STUDY_VISIT_COUNT, getXpathCount(Locator.xpath("//table[@id = 'visits']/tbody/tr")) - 1);
        for (String visit: visits)
        {
            assertTextPresent(visit);
        }

        if (reports.length > 0 || views.length > 0)
        {
            goToManageViews();
            waitForExtMaskToDisappear();
            if(reports.length > 0)
            {
                waitForText(reports[0]);
            }
            else
            {
                waitForText(views[0]);
            }

            String viewXpath = "//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]";
            assertEquals("Unexpected number of views/reports", views.length + reports.length, getXpathCount(Locator.xpath(viewXpath)));
        }

        // Verify published reports/views
        if (reports.length > 0)
        {
            assertTextPresent(reports);
            String reportXpath = "//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]";
            for (final String report : reports)
            {
                pushLocation();
                String thisViewXpath = reportXpath+"[.//*[string()='"+report+"']]";
                click(Locator.xpath(thisViewXpath));
                waitAndClick(Locator.linkWithText("view"));
                waitForPageToLoad();
                waitFor(new Checker()
                    {
                        public boolean check()
                        {
                            return isTextPresent(report) || isTextPresent("Table or query not found") || getResponseCode() == 404;
                        }
                    }, "View did not load: " + report, WAIT_FOR_JAVASCRIPT);

                if (isTextPresent(report))
                {
                    ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
                    if (alternateIDs)
                        assertTextNotPresent(ptids);
                    else
                        assertTextPresent(ptids);

                    if (isTextPresent("Time Chart"))
                    {
                        // Verify that time chart on click function is published with time chart
                        waitAndClick(Locator.css("svg a>path"));
                        ExtHelper.waitForExtDialog(this, "Data Point Information");
                        clickButton("OK", 0);
                        waitForExtMaskToDisappear();
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
                waitForExtMaskToDisappear();
                waitForText(reports[0]);
            }


        }

        if(views.length > 0)
        {
            assertTextPresent(views);
            String viewXpath = "//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]";

            for (final String view : views)
            {
                // Verify the views.
                pushLocation();
                String thisViewXpath = viewXpath+"[.//*[string()='"+view+"']]";
                click(Locator.xpath(thisViewXpath));
                waitAndClick(Locator.linkWithText("view"));
                waitForPageToLoad();

                // Do some sort of check.
                assertTextPresent(view);

                popLocation();
                waitForExtMaskToDisappear();
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

    }

    private void publishStudy(String name, String description, String parentContainer, String[] groups, String[] datasets, String[] visits, String[] views, String[] reports, String[] lists)
    {
        publishStudy(name, description, parentContainer, groups, datasets, visits, views, reports, lists, true, true, true);
    }

    // Test should be in an existing study.
    private void publishStudy(String name, String description, String parentContainer, String[] groups, String[] datasets, String[] visits, String[] views, String[] reports, String[] lists, boolean removeProtected, boolean shiftDates, boolean alternateIDs)
    {
        pushLocation();
        clickTab("Manage");

        log("Publish study.");
        clickButton("Publish Study", 0);
        ExtHelper.waitForExtDialog(this, "Publish Study");

        // Wizard page 1 : General Setup
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'General Setup']"));
        setFormElement(Locator.name("studyName"), name);
        setFormElement(Locator.name("studyDescription"), description);
        assertTrue(PROTOCOL_DOC.exists());
        setFormElement(Locator.name("protocolDoc"), PROTOCOL_DOC);
        selectLocation(parentContainer);
        clickButton("Next", 0);

        // Wizard page 2 : Mice
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Mice']"));
        waitForElement(Locator.css(".studyWizardParticipantList"));
        if (groups != null && groups.length > 0)
        {
            clickButton("Next", 0);
            ExtHelper.waitForExtDialog(this, "Error");
            assertTextPresent("You must select at least one Mouse group.");
            ExtHelper.clickExtButton(this, "Error", "OK", 0);
            checkRadioButton("renderType", "existing");
            for (String group : groups)
            {
                getWrapper().getEval("selenium.selectExtGridItem('label', '"+group+"', -1, 'studyWizardParticipantList', true)");
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
        clickButton("Next", 0);
        ExtHelper.waitForExtDialog(this, "Error");
        assertTextPresent("You must select at least one dataset to create the new study from.");
        ExtHelper.clickExtButton(this, "Error", "OK", 0);
        for (String dataset : datasets)
        {
            getWrapper().getEval("selenium.selectExtGridItem('Label', '"+dataset+"', -1, 'studyWizardDatasetList', true)");
        }
        if (datasets.length == 0) // select all
            getWrapper().getEval("selenium.selectExtGridItem(null, null, -2, 'studyWizardDatasetList', true)");
        clickButton("Next", 0);

        // Wizard page 4 : Visits
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Visits']"));
        waitForElement(Locator.css(".studyWizardVisitList"));
        clickButton("Next", 0);
        ExtHelper.waitForExtDialog(this, "Error");
        assertTextPresent("You must select at least one visit.");
        ExtHelper.clickExtButton(this, "Error", "OK", 0);
        for (String visit : visits)
        {
            getWrapper().getEval("selenium.selectExtGridItem('SequenceNumMin', '" + visit + "', -1, 'studyWizardVisitList', true)");
        }
        if (visits.length == 0) // select all
            getWrapper().getEval("selenium.selectExtGridItem(null, null, -2, 'studyWizardVisitList', true)");
        clickButton("Next", 0);

        // Wizard page 5 : Lists
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Lists']"));
        waitForElement(Locator.css(".studyWizardListList"));
        for (String list : lists)
        {
            getWrapper().getEval("selenium.selectExtGridItem('name', '" + list + "', -1, 'studyWizardListList', true)");
        }
        clickButton("Next", 0);

        // Wizard page 6 : Views
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Views']"));
        waitForElement(Locator.css(".studyWizardViewList"));
        assertTextNotPresent(R_VIEW_UNSHARED);
        for (String view : views)
        {
            getWrapper().getEval("selenium.selectExtGridItem('name', '" + view + "', -1, 'studyWizardViewList', true)");
        }
        clickButton("Next", 0);

        // Wizard Page 7 : Reports
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Reports']"));
        waitForElement(Locator.css(".studyWizardViewList"));
        for (String report : reports)
        {
            getWrapper().getEval("selenium.selectExtGridItem('name', '" + report + "', -1, 'studyWizardReportList', true)");
        }
        clickButton("Next", 0);

        // Wizard page 8 : Publish Options
        waitForElement(Locator.xpath("//div[@class = 'labkey-nav-page-header'][text() = 'Publish Options']"));
        if (!removeProtected) uncheckCheckbox(Locator.name("removeProtected"));
        if (!shiftDates) uncheckCheckbox(Locator.name("shiftDates"));
        if (!alternateIDs) uncheckCheckbox(Locator.name("alternateids"));
        clickButton("Finish");

        _pipelineJobs++;

        if (parentContainer.equals("/"))
            _containerHelper.addCreatedProject(name); // Add to list so that it will be deleted during cleanup

        popLocation();
    }

    private static final String TEST_DATA_API_PATH = "server/test/data/api";
    // Starting at an existing data grid
    private void createTimeChart(String name, String[]... datasetMeasurePairs)
    {
        goToManageViews();
        clickMenuButton("Create", "Time Chart");

        waitForElement(Locator.button("Choose a Measure"), WAIT_FOR_JAVASCRIPT);
        clickButton("Choose a Measure", 0);
        ExtHelper.waitForExtDialog(this, "Add Measure...");
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        setFormElement(Locator.name("filterSearch"), datasetMeasurePairs[0][0]);
        String measureXpath = ExtHelper.getExtDialogXPath(this, "Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+datasetMeasurePairs[0][1]+"')]]";
        waitForElementToDisappear(Locator.xpath("("+measureXpath+")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates
        mouseDown(Locator.xpath(measureXpath));
        clickButton("Select", 0);
        waitForText("Days Since Contact Date", WAIT_FOR_JAVASCRIPT);

        if (datasetMeasurePairs.length > 1)
        {
            clickButton("Measures", 0);
            waitForText("Divide data into Series");
            waitForElement(Locator.button("Add Measure"));
            for (int i = 1; i < datasetMeasurePairs.length; i++)
            {
                clickButton("Add Measure", 0);
                ExtHelper.waitForExtDialog(this, "Add Measure...");
                ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

                setFormElement(Locator.name("filterSearch", i), datasetMeasurePairs[i][0]);
                measureXpath = ExtHelper.getExtDialogXPath(this, "Add Measure...") + "//table/tbody/tr/td[div[starts-with(text(), '"+datasetMeasurePairs[i][1]+"')]]";
                waitForElementToDisappear(Locator.xpath("("+measureXpath+")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates
                mouseDown(Locator.xpath(measureXpath));
                clickButton("Select", 0);

                waitForText(datasetMeasurePairs[i][1] + " from " + datasetMeasurePairs[i][0]);
            }
            clickButton("OK", 0);
        }

        mouseDown(Locator.tagWithText("div", "All"));
        sleep(1000);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT); // Make sure charts are rendered

        // Add point click function
        clickNavButton("Developer", 0);
        waitForElement(Locator.button("Cancel"));
        clickButton("Enable", 0);
        setFormElement("point-click-fn-textarea", getFileContents(TEST_DATA_API_PATH + "/timeChartPointClickTestFn.js"));
        waitAndClick(Locator.button("OK"));

        // Visit-based chart
        getWrapper().getEval("window.showTimeChartAxisPanel('X-Axis');");
        waitForElement(Locator.button("Cancel"));
        Ext4Helper.selectRadioButton(this, "Chart Type:", "Visit Based Chart");
        waitAndClick(Locator.button("OK"));
        waitForTextToDisappear("Days Since Contact Date");

        clickButton("Save", 0);
        waitForText("Viewable By");
        setFormElement(Locator.name("reportName"), name);

        clickButtonByIndex("Save", 1);
    }

    private static final String ADD_MEASURE_TITLE = "Add Measure";
    private void createMouseReport(String reportName, String[]... datasetMeasurePairs)
    {
        goToManageViews();
        clickMenuButton("Create", "Mouse Report");
        waitAndClickNavButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, ADD_MEASURE_TITLE);
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        for (String[] pair : datasetMeasurePairs)
        {
            ExtHelper.setExtFormElementByType(this, ADD_MEASURE_TITLE, "text", pair[0]);
//            pressEnter(ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE)+"//input[contains(@class, 'x4-form-text') and @type='text']");
            String measureXpath = ExtHelper.getExtDialogXPath(this, ADD_MEASURE_TITLE) + "//table/tbody/tr[not(contains(@class, 'x4-grid-row-selected'))]/td[div[starts-with(text(), '"+ pair[1]+"')]]";
            waitForElement(Locator.xpath(measureXpath), WAIT_FOR_JAVASCRIPT); // Make sure measure has appeared
            waitForElementToDisappear(Locator.xpath("("+measureXpath+")[2]"), WAIT_FOR_JAVASCRIPT); // Wait for filter to remove any duplicates

            ExtHelper.clickX4GridPanelCheckbox(this, "label", pair[1], "measuresGridPanel", true);
        }

        clickNavButton("Select", 0);

        ExtHelper.setExtFormElementByLabel(this, "Report Name", reportName);
        clickNavButton("Save", 0);
        waitForElement(Locator.xpath("id('participant-report-panel-1-body')/div[contains(@style, 'display: none')]"), WAIT_FOR_JAVASCRIPT); // Edit panel should be hidden
    }

    private void createRView(String name, String dataset, boolean shareView)
    {
        goToManageStudy();
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(dataset);
        clickButton("View Data");
        CustomizeViewsHelper.createRView(this, null, name, shareView);
    }

    private void createCustomView(String name, String dataset, String[] ptids, boolean shared)
    {
        String ptidFilter = "";

        for (String ptid : ptids)
        {
            if(!ptidFilter.equals(""))
                ptidFilter = ptidFilter + ";";

            ptidFilter = ptidFilter + ptid;
        }

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());

        clickLinkWithText(dataset);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewFilter(this, "MouseId", "Mouse Id", "Equals One Of", ptidFilter);
        CustomizeViewsHelper.saveCustomView(this, name, shared);
    }

    /**
     * Selects the destination for study publication
     * @param containerPath path to the published study's parent. NULL or blank to select the default location.
     */
    private void selectLocation(String containerPath)
    {
        if (containerPath!=null && !containerPath.equals(""))
        {
            clickButton("Change", 0);
            waitForElement(Locator.css(".folder-management-tree:not(.x-panel-collapsed)"));

            String nodeXpath = "//div[contains(@class, 'folder-management-tree')]//ul[contains(@class, 'x-tree-root-ct')]/li/div[contains(@class, 'x-tree-node-el') and string()='LabKey Server Projects']";
            if (containerPath.equals("/"))
            {
                selenium.doubleClick(Locator.xpath(nodeXpath).toString());
            }
            else
            {
                String[] splitPath = containerPath.split("/");
                for (int i = 1; i < splitPath.length; i++)
                {
                    if (getAttribute(Locator.xpath(nodeXpath), "class").contains("x-tree-node-collapsed"))
                    {
                        click(Locator.xpath(nodeXpath + "/img[contains(@class, 'x-tree-ec-icon')]"));
                    }
                    nodeXpath += "/../ul/li/div[contains(@class, 'x-tree-node-el') and string()='"+splitPath[i]+"']";
                    if (!isElementPresent(Locator.xpath(nodeXpath)))
                        Assert.fail("Unable to find folder: " + containerPath);
                }
                selenium.doubleClick(Locator.xpath(nodeXpath).toString());
            }
        }
    }

    // Test should be in an existing study.
    private void setStudyProperties(String label, String investigator, String grant, String description)
    {
        clickTab("Manage");
        clickLinkWithText("Change Study Properties");

        waitForElement(Locator.name("Label"));
        setFormElement(Locator.name("Label"), label);
        setFormElement(Locator.name("Investigator"), investigator);
        setFormElement(Locator.name("Grant"), grant);
        setFormElement(Locator.name("Description"), description);

        clickButton("Submit");
    }
}
