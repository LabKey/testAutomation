package org.labkey.test.tests;

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;

@Category({DailyB.class, Reports.class})
public class ReportAndDatasetNotificationTest extends StudyBaseTest
{
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
        importFolderFromPipeline("ReportDatasetNotifyTest.folder.zip");
    }

    @Override
    protected void doVerifySteps()
    {
        log("Subscribe to some categories");
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickAndWait(Locator.linkContainingText("Manage Notifications"));
        _ext4Helper.checkGridRowCheckboxAlt("Uncategorized", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Cons", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Reports", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Exams", 0, true);
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
    }

}
