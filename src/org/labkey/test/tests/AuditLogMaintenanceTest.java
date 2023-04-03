package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Hosting;
import org.labkey.test.pages.core.admin.AuditLogMaintenancePage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.util.ListHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AuditLogMaintenanceTest extends BaseWebDriverTest
{
    private final static String listName = "List for audit log entries";

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
        new PipelineStatusDetailsPage(getDriver()).waitForStatus("COMPLETE");
        Assert.assertTrue("Expected audit event was deleted", isTextPresent("Skipping \"SampleTimelineEvent\"; its provider doesn't allow deleting",
                "Skipping \"InventoryAuditEvent\"; its provider doesn't allow deleting"));
        switchToMainWindow();

        log("Verifying audit logs got deleted");
        ShowAuditLogPage auditLogPage = goToAdminConsole().clickAuditLog();
        auditLogPage.selectView("List events");
        Assert.assertTrue("Audit log table not cleared", auditLogPage.getLogTable().getDataRowCount() == 0);
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
    public void restoreSettings() throws IOException, CommandException
    {
        SimplePostCommand command = new SimplePostCommand("professional", "auditLogMaintenance");
        command.setJsonObject(new JSONObject(Map.of("retentionTime", -1, "export", false)));
        command.execute(createDefaultConnection(), null);
    }
}
