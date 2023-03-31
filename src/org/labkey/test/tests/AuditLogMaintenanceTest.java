package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.core.admin.AuditLogMaintenancePage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AuditLogMaintenanceTest extends BaseWebDriverTest
{
    private final static String listName = "List for audit log entries - 2";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("audit");
    }

    @BeforeClass
    public static void setupProject()
    {
        AuditLogMaintenanceTest init = (AuditLogMaintenanceTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Test
    public void testAuditLogDeletionByAdmin()
    {
        goToProjectHome();
        log("Creating audit log entries");
        _listHelper.createList(getProjectName(), listName, ListHelper.ListColumnType.AutoInteger, "id");

        log("Setting audit log maintenance parameters");
        AuditLogMaintenancePage auditLogMaintenancePage = goToAdminConsole().clickAuditLogMaintenance();
        auditLogMaintenancePage.setRetentionTime("0 seconds - all audit log data will be deleted! For testing purposes only!")
                .setExportDataBeforeDeleting(true)
                .clickSave();

        log("Running audit log maintenance job");
        goToAdminConsole().clickSystemMaintenance();
        waitAndClick(Locator.linkWithText("Audit Log Maintenance"));
        switchToWindow(1);
        shortWait().until(ExpectedConditions.textToBePresentInElementLocated(Locator.id("status-text"), "COMPLETE"));
        switchToMainWindow();

        log("Verifying audit logs got deleted");
        ShowAuditLogPage auditLogPage = goToAdminConsole().clickAuditLog();
        auditLogPage.selectView("List events");
        checker().verifyTrue("Audit log table not cleared", auditLogPage.getLogTable().getDataRowCount() == 0);
    }

    @Test
    public void testAuditLogDeletionByNonAdmin()
    {
        goToAdminConsole();
        impersonateRole("Troubleshooter");
        goToAdminConsole().clickAuditLogMaintenance();
        Assert.assertFalse("Troubleshooter should not able to save", isElementPresent(Locator.button("Save")));
        Assert.assertTrue("Missing Done button", isElementPresent(Locator.tagWithText("span", "Done")));
        stopImpersonating();
    }

    @After
    public void restoreSettings()
    {
        goToAdminConsole().clickAuditLogMaintenance()
                .setRetentionTime("None - all audit log data is retained indefinitely")
                .setExportDataBeforeDeleting(false)
                .clickSave();
    }
}
