package org.labkey.test.tests.core.security;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyC;
import org.labkey.test.pages.core.admin.ShowAuditLogPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyC.class})
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
    }

    private void doSetup()
    {
        _userHelper.createUser(TROUBLESHOOTER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(TROUBLESHOOTER,"Troubleshooter", PermissionsHelper.MemberType.user,"/");
    }

    @Test
    public void testAuditLogsIsAccessible()
    {
        impersonate(TROUBLESHOOTER);
        goToAdminConsole().goToSettingsSection();

        log("Verifying audit log link is present");
        checker().verifyTrue("Audit log is not present for troubleshooter",
                isElementPresent(Locator.linkWithText("audit log")));
        clickAndWait(Locator.linkWithText("audit log"));

        log("Verify the export file is non empty");
        ShowAuditLogPage auditLogPage = new ShowAuditLogPage(getDriver());
        File exportedFile = auditLogPage.exportExcelxlsx();
        checker().verifyTrue("Empty downloaded [" + exportedFile.getName() + "]", exportedFile.length() > 0);

        log("Verify permissions from troubleshooter");
        verifySitePermissionSetting(false);
        stopImpersonating();

        log("Verify the permissions for admin ");
        goToHome();
        verifySitePermissionSetting(true);

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
