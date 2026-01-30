package org.labkey.test.tests.core.security;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.SummaryStatisticsHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.TROUBLESHOOTER_ROLE;

@Category({Git.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class TroubleshooterRoleTest extends BaseWebDriverTest
{
    protected static final String TROUBLESHOOTER_USER = "troubleshooter@troubleshooter.test";
    protected int _troubleShooterId;

    @BeforeClass
    public static void setupProject()
    {
        TroubleshooterRoleTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, TROUBLESHOOTER_USER);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    protected void doSetup()
    {
        _troubleShooterId = _userHelper.createUser(TROUBLESHOOTER_USER).getUserId();
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(_troubleShooterId, getRole(), "/");
        _containerHelper.createProject(getProjectName());
    }

    protected String getRole()
    {
        return TROUBLESHOOTER_ROLE;
    }

    @Test
    public void testAuditLogsIsAccessible() throws Exception
    {
        // Ensure that there is at least one event to see
        new IntListDefinition("AuditList", "id").create(createDefaultConnection(), getProjectName());

        impersonate(TROUBLESHOOTER_USER);
        ShowAdminPage showAdminPage = goToAdminConsole().goToSettingsSection();

        log("Verifying audit log link is present");
        assertTrue("Audit log is not present for troubleshooter",
                isElementPresent(Locator.linkWithText("audit log")));

        log("Verify the export file is non empty");
        ShowAuditLogPage auditLogPage = showAdminPage.clickAuditLog();
        auditLogPage.selectView("Domain events");
        DataRegionTable logTable = auditLogPage.getLogTable();
        assertTrue("Troubleshooter should see audit entries", logTable.getDataRowCount() > 0);
        File exportedFile = logTable.expandExportPanel().exportText();
        int exportedRowCount = IteratorUtils.size(FileUtils.lineIterator(exportedFile)) - 1;
        assertTrue("Empty downloaded [" + exportedFile.getName() + "]", exportedRowCount > 0);
    }

    @Test
    public void testAdminConsoleVisibility()
    {
        impersonate(TROUBLESHOOTER_USER);

        log("Verify permissions from troubleshooter");
        verifySitePermissionSetting(false);
        stopImpersonating();

        log("Verify the permissions for admin ");
        goToHome();
        verifySitePermissionSetting(true);
    }

    // Verify fix for Issue 47508 / GitHub Issue #26: auditLog table visibility is inconsistent
    @Test
    public void testAllAuditTableVisibility()
    {
        impersonate(TROUBLESHOOTER_USER);
        ShowAdminPage showAdminPage = goToAdminConsole().goToSettingsSection();

        log("Verify \"Group and role\" audit event table is viewable");
        ShowAuditLogPage auditLogPage = showAdminPage.clickAuditLog();
        auditLogPage.selectView("Group and role events");
        assertTextNotPresent("You do not have permission to see this data.");
        DataRegionTable logTable = auditLogPage.getLogTable();
        assertTrue(logTable.getDataRowCount() > 0);
    }

    protected void verifySitePermissionSetting(boolean canSave)
    {
        log("Verify permissions for look and feel setting");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        checker().verifyEquals("Incorrect access for look and feel setting", canSave,
                isElementPresent(Locator.tagWithText("span","Save")));

        log("Verify permissions for configure page elements");
        goToAdminConsole().goToSettingsSection().clickConfigurePageElements();
        checker().verifyEquals("Incorrect access for configure page element", canSave,
                isElementPresent(Locator.tagWithText("span","Save")));

        log("Verify permissions for External Redirect Hosts");
        goToAdminConsole().goToSettingsSection().clickAllowedExternalRedirectHosts();
        checker().verifyEquals("Incorrect access for External Redirect Hosts", canSave,
                isElementPresent(Locator.tagWithText("span","Save")));

        log("Verify permissions for authentication changes");
        goToAdminConsole().goToSettingsSection().clickAuthentication();
        checker().verifyEquals("Incorrect access for authentication", canSave,
                isElementPresent(Locator.button("Save and Finish")));
    }

    // Verifications for GitHub Issue #785 - Troubleshooters should have read access in the root (but not elsewhere)
    @Test
    public void testQueryAccessInRoot()
    {
        goToAdminConsole();
        impersonate(TROUBLESHOOTER_USER);
        testQueryAccess();
        stopImpersonating();

        goToAdminConsole();
        impersonateRole(TROUBLESHOOTER_ROLE);
        testQueryAccess();
        stopImpersonating();
    }

    private void testQueryAccess()
    {
        // Verify that Troubleshooters can access the schema browser and view an arbitrary query in the root
        goToSchemaBrowser(null, false);
        DataRegionTable dataRegionTable = viewQueryData("core", "Modules");
        dataRegionTable.showAll();
        int rowCount = dataRegionTable.getDataRowCount();
        assertTrue(rowCount > 6);

        // Verify that basic summary statistics work
        dataRegionTable.setSummaryStatistic("Name", SummaryStatisticsHelper.BASE_STAT_COUNT, String.valueOf(rowCount));

        // Verify that exports using POST work
        DataRegionExportHelper exportHelper = new DataRegionExportHelper(dataRegionTable);
        exportHelper.exportExcel(AbstractDataRegionExportOrSignHelper.ExcelFileType.XLSX);
        exportHelper.exportScript(DataRegionExportHelper.ScriptExportType.JAVA);

        // Troubleshooters should NOT have read access outside the root
        goToProjectHome();
        waitForText("User does not have permission to perform this operation.");
        goToSchemaBrowser(getProjectName(), true);
    }

    // Troubleshooters don't get the "Go To Module" menu item, so can't use goToSchemaBrowser()
    public void goToSchemaBrowser(@Nullable String container, boolean expectPermissionError)
    {
        beginAt(WebTestHelper.buildRelativeUrl("query", container,"begin"));

        if (expectPermissionError)
        {
            waitForText("User does not have permission to perform this operation.");
        }
        else
        {
            shortWait().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.lk-sb-instructions")));
            waitForElement(Locators.pageSignal("queryTreeRendered"));
        }
    }

    @Override
    protected String getProjectName()
    {
        return "TroubleshooterRoleTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
