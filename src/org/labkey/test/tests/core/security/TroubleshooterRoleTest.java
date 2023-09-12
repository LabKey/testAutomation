package org.labkey.test.tests.core.security;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PermissionsHelper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({Git.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class TroubleshooterRoleTest extends BaseWebDriverTest
{
    private static final String TROUBLESHOOTER = "troubleshooter@troubleshooter.test";

    @BeforeClass
    public static void setupProject()
    {
        TroubleshooterRoleTest init = (TroubleshooterRoleTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, TROUBLESHOOTER);
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    private void doSetup()
    {
        _userHelper.createUser(TROUBLESHOOTER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(TROUBLESHOOTER,"Troubleshooter", PermissionsHelper.MemberType.user,"/");
        _containerHelper.createProject(getProjectName());
    }

    @Test
    public void testAuditLogsIsAccessible() throws Exception
    {
        // Ensure that there is at least on event to see
        new IntListDefinition("AuditList", "id").create(createDefaultConnection(), getProjectName());

        impersonate(TROUBLESHOOTER);
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

        log("Verify permissions from troubleshooter");
        verifySitePermissionSetting(false);
        stopImpersonating();

        log("Verify the permissions for admin ");
        goToHome();
        verifySitePermissionSetting(true);

    }

    /**
     * Issue 47508: auditLog table visibility is inconsistent
     * Assert broken behavior to prompt a test update once issue is fixed.
     */
    @Test
    public void testAllAuditTableVisibility()
    {
        impersonate(TROUBLESHOOTER);
        ShowAdminPage showAdminPage = goToAdminConsole().goToSettingsSection();

        log("Verify the export file is non empty");
        ShowAuditLogPage auditLogPage = showAdminPage.clickAuditLog();
        auditLogPage.selectView("Group and role events");
        assertTextPresent("You do not have permission to see this data.");
    }

    private void verifySitePermissionSetting(boolean canSave)
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
        goToAdminConsole().goToSettingsSection().clickExternalRedirectHosts();
        checker().verifyEquals("Incorrect access for External Redirect Hosts", canSave,
                isElementPresent(Locator.tagWithText("span","Save")));

        log("Verify permissions for authentication changes");
        goToAdminConsole().goToSettingsSection().clickAuthentication();
        checker().verifyEquals("Incorrect access for authentication", canSave,
                isElementPresent(Locator.button("Save and Finish")));
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
