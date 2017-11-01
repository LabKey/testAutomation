/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Reports;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.LookAndFeelTimeChart;
import org.labkey.test.components.SaveChartDialog;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.ext4cmp.Ext4FileFieldRef;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.Locator.NBSP;

@Category({DailyC.class, Reports.class})
public class ReportAndDatasetNotificationTest extends StudyBaseTest
{
    private final PortalHelper _portalHelper = new PortalHelper(this);

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ReportDatasetNotifyVerifyProject";
    }

    @Override
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        initializeFolder();
        initializePipeline(null);

        clickFolder(getFolderName());

        log("Import study with reports and datasets");
        importFolderFromZip(TestFileUtils.getSampleData("studies/ReportDatasetNotifyTest.folder.zip"));
    }

    @Override
    protected void doVerifySteps()
    {
        log("Subscribe to some categories");
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickAndWait(Locator.linkContainingText("Manage Notifications"));

        assertGridPanel(false);
        selectNotificationOption("select");
        assertGridPanel(true);
        _ext4Helper.checkGridCellCheckbox("Uncategorized", 0);
        _ext4Helper.checkGridCellCheckbox("Cons", 0);
        _ext4Helper.checkGridCellCheckbox("Reports", 0);
        _ext4Helper.checkGridCellCheckbox(NBSP + NBSP + NBSP + NBSP + "Exams", 0);
        clickButton("Save");

        log("Send notification and check email in dumbster");
        beginAt("/reports/" + getProjectName() + "/sendDailyDigest.view");
        clickFolder(getFolderName());
        goToModule("Dumbster");
        click(Locator.linkContainingText("Report/Dataset Change Notification"));
        assertTextPresent("Participation and Genetic Consent",
                "Box Plot - Antigen Spot Counts",
                "Renal Study",
                "Participant Weight",
                "Physical Exam",
                "R Regression: Blood Pressure: All");
        assertTextNotPresent("HIV Test Results",
                "Participant Views: CD4, Virus vs. T",
                "Luminex Assay 100",
                "Status Assessment");
        clickFolder(getFolderName());

        log("Set notification option All before changing content");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickAndWait(Locator.linkContainingText("Manage Notifications"));
        selectNotificationOption("all");
        assertGridPanel(false);
        clickButton("Save");

        verifyContentModified();

        log("Send notification and check email in dumbster");
        beginAt("/reports/" + getProjectName() + "/sendDailyDigest.view");
        clickFolder(getFolderName());
        goToModule("Dumbster");
        new EmailRecordTable(getDriver()).clickSubject("Report/Dataset Change Notification");
        assertTextPresent(TIMECHART_NAME, R_NAME, LINKREPORT_NAME, PLOT_NAME);

    }

    private static final String TIMECHART_NAME = "Mean Cohort Lymph Levels";
    private static final String R_NAME = "R Cohort Regression: CD4, Virus, T";
    private static final String PLOT_NAME = "Systolic over Diastolic";
    private static final String PARTICIPANTREPORT_NAME = "Participant Report: Vital Signs and Lymphocyte Levels";
    private static final String LINKREPORT_NAME = "Renal Study";
    private static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String ATTACHMENT_REPORT_NAME = "Attachment Report1";
    private static final String ATTACHMENT_REPORT_DESCRIPTION = "This attachment report uploads a file";
    private static final File ATTACHMENT_REPORT_FILE = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Microarray/", "test1.jpg"); // arbitrary image file
    private static final File ATTACHMENT_REPORT2_FILE = new File(TestFileUtils.getLabKeyRoot() + "/sampledata/Microarray/", "test2.jpg"); // arbitrary image file

    private static final String[] datasetsContentChanging = {
        TIMECHART_NAME, R_NAME, PLOT_NAME, PARTICIPANTREPORT_NAME, LINKREPORT_NAME, ATTACHMENT_REPORT_NAME
    };

    private static final String[] datasetsContentNotChanging = {
            R_NAME, PARTICIPANTREPORT_NAME, LINKREPORT_NAME, ATTACHMENT_REPORT_NAME, TIMECHART_NAME
    };

    protected final SimpleDateFormat _formatter = new SimpleDateFormat(DATEFORMAT);

    @LogMethod
    protected void verifyContentModified()
    {
        log("Modify various reports' contents");
        clickFolder(getFolderName());

        // Add attachment report
        goToManageViews();
        BootstrapMenu.find(getDriver(),"Add Report")
                .clickSubMenu(true,"Attachment Report");

        setFormElement(Locator.name("viewName"), ATTACHMENT_REPORT_NAME);
        setFormElement(Locator.name("description"), ATTACHMENT_REPORT_DESCRIPTION);
        setFormElement(Locator.id("uploadFile-button-fileInputEl"), ATTACHMENT_REPORT_FILE);
        Ext4FileFieldRef ref = Ext4FileFieldRef.create(this);
        ref.setToFile(ATTACHMENT_REPORT_FILE);
        clickButton("Save");
        // save should return back to manage views page
        waitForText("Manage Views");

        // change date format
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Formats"));
        setFormElement(Locator.input("defaultDateFormat"), DATEFORMAT);
        clickButton("Save");
        clickTab("Clinical and Assay Data");
        waitForElement(Locator.linkWithText("GenericAssay"));

        // record current time
        long currentTime = System.currentTimeMillis();
        sleep(1000);        // Make sure we can tell if Content Modified changed

        openCustomizePanel("Data Views");
        _ext4Helper.uncheckCheckbox("datasets");
        _ext4Helper.uncheckCheckbox("queries");
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        // make modifications
        openReport(TIMECHART_NAME);
        TimeChartWizard timeChartWizard = new TimeChartWizard(this);
        waitForElement(Locator.css("svg"));
        clickButton("Edit");
        timeChartWizard.verifySvgChart(2, null);
        LookAndFeelTimeChart lookAndFeelDialog = timeChartWizard.clickChartLayoutButton();
        lookAndFeelDialog.setSubjectSelectionType(LookAndFeelTimeChart.SubjectSelectionType.Participants).clickApply();
        timeChartWizard.verifySvgChart(5, null);
        timeChartWizard.reSaveReport();

        openReport(R_NAME);
        waitAndClick(Ext4Helper.Locators.tab("Source"));
        String script = _extHelper.getCodeMirrorValue("script-report-editor");
        setCodeEditorValue("script-report-editor", script + "     #an edit");
        clickButton("Save");

        openReport(PLOT_NAME);
        waitForElement(Locator.css("svg"));
        clickButton("Edit");
        waitForElement(Locator.css("svg"));
        clickButton("Chart Type", 0);
        ChartTypeDialog chartTypeDialog = new ChartTypeDialog(getDriver());
        assertEquals("Scatter", chartTypeDialog.getChartTypeTitle());
        chartTypeDialog.setChartType(ChartTypeDialog.ChartType.Box);
        doAndWaitForElementToRefresh(() -> chartTypeDialog.clickApply(), Locator.css("svg"), shortWait());
        _ext4Helper.waitForMaskToDisappear();
        clickButton("Save", 0);
        SaveChartDialog saveChartDialog = new SaveChartDialog(this);
        saveChartDialog.waitForDialog();
        doAndWaitForPageSignal(() -> saveChartDialog.clickSave(), "genericChartDirty");

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        openReport(PARTICIPANTREPORT_NAME);
        enableEditMode();
        Locator.XPathLocator deleteButton = Locator.xpath("//img[@data-qtip = 'Delete']");
        click(deleteButton); // Delete a column.
        clickButton("Save", 0);
        waitForElementToDisappear(deleteButton.notHidden());
        _ext4Helper.waitForMaskToDisappear();

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        log("edit link report");
        Locator linkDetails = Locator.xpath("//tr/td/div/a[text()=\"" + LINKREPORT_NAME + "\"]/../../../td/div/a[@data-qtip=\"Click to navigate to the Detail View\"]");
        waitAndClickAndWait(linkDetails);
        clickButton("Edit Report", "Update Link Report");
        setFormElement(Locator.name("linkUrl"), "http://www.labkey.org");
        clickButton("Save");

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        log("edit attachment report");
        linkDetails = Locator.xpath("//tr/td/div/a[text()=\"" + ATTACHMENT_REPORT_NAME + "\"]/../../../td/div/a[@data-qtip=\"Click to navigate to the Detail View\"]");
        waitAndClickAndWait(linkDetails);
        clickButton("Edit Report", "Update Attachment Report");
        setFormElement(Locator.id("uploadFile-button-fileInputEl"), ATTACHMENT_REPORT2_FILE);
        Ext4FileFieldRef ref1 = Ext4FileFieldRef.create(this);
        ref1.setToFile(ATTACHMENT_REPORT2_FILE);
        clickButton("Save");

        // add content modified column
        clickTab("Clinical and Assay Data");
        openCustomizePanel("Data Views");
        _ext4Helper.checkCheckbox("Content Modified");
        _ext4Helper.uncheckCheckbox("Details");
        _ext4Helper.uncheckCheckbox("Access");
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        checkModifiedReports(datasetsContentChanging, currentTime, 6, true, "Content should have been modified for these reports");

        log("modify reports' metadata, where Content Modified should not change");
        currentTime = System.currentTimeMillis();
        sleep(1000);        // Make sure we can tell if Content Modified changed

        clickTab("Clinical and Assay Data");
        openCustomizePanel("Data Views");
        _ext4Helper.checkCheckbox("Details");
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        // TODO: uncomment when #21263 is fixed
/*        openReport(TIMECHART_NAME);
        clickButton("Edit", "Save As");
        clickButtonByIndex("Save", 0, 0);
        waitForText("Viewable By");
        saveReport(false);
*/
        openReport(R_NAME);
        waitAndClick(Ext4Helper.Locators.tab("Source"));
        _ext4Helper.checkCheckbox("Show source tab to all users");
        clickButton("Save");

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        openReport(PARTICIPANTREPORT_NAME);
        enableEditMode();
        _ext4Helper.selectRadioButton("Only me");
        clickButton("Save", 0);
        waitForElementToDisappear(deleteButton.notHidden());
        _ext4Helper.waitForMaskToDisappear();

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        log("edit link report");
        waitForElement(Locator.linkWithText(LINKREPORT_NAME));
        scrollIntoView(Locator.linkWithText(LINKREPORT_NAME));
        linkDetails = Locator.xpath("//tr/td/div/a[text()=\"" + LINKREPORT_NAME + "\"]/../../../td/div/a[@data-qtip=\"Click to navigate to the Detail View\"]");
        clickAndWait(linkDetails);
        clickButton("Edit Report", "Update Link Report");
        _ext4Helper.checkCheckbox("Target:");
        clickButton("Save");

        clickTab("Clinical and Assay Data");
        waitForText("Tests");
        collapseCategory("Tests");
        log("edit attachment report");
        waitForElement(Locator.linkWithText(ATTACHMENT_REPORT_NAME));
        scrollIntoView(Locator.linkWithText(ATTACHMENT_REPORT_NAME));
        linkDetails = Locator.xpath("//tr/td/div/a[text()=\"" + ATTACHMENT_REPORT_NAME + "\"]/../../../td/div/a[@data-qtip=\"Click to navigate to the Detail View\"]");
        clickAndWait(linkDetails);
        clickButton("Edit Report", "Update Attachment Report");
        _ext4Helper.checkCheckbox("Shared:");
        clickButton("Save");

        clickTab("Clinical and Assay Data");
        openCustomizePanel("Data Views");
        _ext4Helper.uncheckCheckbox("Details");
        clickButton("Save", 0);
        _ext4Helper.waitForMaskToDisappear();

        checkModifiedReports(datasetsContentNotChanging, currentTime, 5, false, "Content should not have been modified for these reports");
    }

    private void openCustomizePanel(String title)
    {
        _portalHelper.clickWebpartMenuItem(title, false, "Customize");
        waitForElement(Ext4Helper.Locators.ext4Button("Manage Categories"), WAIT_FOR_JAVASCRIPT);
    }

    protected void saveReport(boolean expectReload)
    {
        clickAndWait(Ext4Helper.Locators.ext4Button("Save").index(1), expectReload ? WAIT_FOR_PAGE : 0);
        if (!expectReload)
        {
            waitForElement(Locator.tagWithClass("div", "x4-window").containing("Report saved successfully."));
            _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
        waitFor(() -> isTextPresent("Please select at least one") || // group/participant
                        isTextPresent("No data found") ||
                        isElementPresent(Locator.css("svg")),
                "Time chart failed to appear after saving", WAIT_FOR_JAVASCRIPT);
    }

    private void enableEditMode()
    {
        waitAndClick(Locator.tag("span").withAttribute("title","Edit"));
        waitForText("Choose Measures");
    }

    protected void openReport(String reportName)
    {
        Locator reportLink = Locator.linkWithText(reportName);
        if (!isElementPresent(reportLink))
            clickTab("Clinical and Assay Data");
        log("Open report " + reportName);
        waitAndClickAndWait(reportLink);
    }

    private void checkModifiedReports(String[] reports, long currentTime, int expectedCount, boolean modifiedExpected, String errorMessage)
    {
        try
        {
            // check content modified
            int contentCount = 0;
            for (String entry : reports)
            {
                Locator dateLoc = Locator.xpath("//tr/td/div/a[text()=\"" + entry + "\"]/../../../td/div");
                String dateText = dateLoc.findElements(getDriver()).get(1).getText();       // should find 1 with report name and 1 with date
                long date = _formatter.parse(dateText).getTime();
                if (modifiedExpected)
                {
                    if (date > currentTime)
                        contentCount += 1;
                }
                else
                {
                    if (date < currentTime)
                        contentCount += 1;
                }
            }
            assertEquals(errorMessage, expectedCount, contentCount);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void selectNotificationOption(String option)
    {
        if ("none".equalsIgnoreCase(option))
            _ext4Helper.selectRadioButton("None.");
        else if ("all".equalsIgnoreCase(option))
            _ext4Helper.selectRadioButton("All. Your daily digest will list changes and additions to all reports and datasets.");
        else if ("select".equalsIgnoreCase(option))
            _ext4Helper.selectRadioButton("By category. Your daily digest will list changes and additions to reports and datasets in the subscribed categories.");
    }

    protected void assertGridPanel(boolean assertEnabled)
    {
        Locator gridPanelDisabled = Locator.xpath("//div[contains(@class, 'x4-grid') and contains(@class, 'x4-item-disabled')]");
        Locator gridPanelEnabled = Locator.xpath("//div[contains(@class, 'x4-grid')]");

        if (assertEnabled)
        {
            assertElementNotPresent(gridPanelDisabled);
            assertElementPresent(gridPanelEnabled);
        }
        else
        {
            assertElementPresent(gridPanelDisabled);
        }
    }

    protected void collapseCategory(String category)
    {
        Locator.XPathLocator dataViewRow = Locator.xpath("//tr").withClass("x4-grid-tree-node-leaf").notHidden();
        int dataViewCount = getElementCount(dataViewRow);
        assertElementPresent(Locator.xpath("//tr").withClass("x4-grid-tree-node-expanded").append("/td/div").withText(category));
        click(Locator.xpath("//div").withText(category).append("/img").withClass("x4-tree-expander"));
        waitForElementToDisappear(dataViewRow.index(dataViewCount - 1), WAIT_FOR_JAVASCRIPT);
    }
}
