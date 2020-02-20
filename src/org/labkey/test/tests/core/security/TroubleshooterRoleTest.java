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

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyC.class})
public class TroubleshooterRoleTest extends BaseWebDriverTest
{
    private static final String TROUBLESHOOTER = "user@troubleshooter.test";

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
        goToSitePermissions();
        apiPermissionsHelper.setUserPermissions(TROUBLESHOOTER, "Troubleshooter");
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

        log("Verify look and feel setting changes cannot be saved");
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        checker().verifyFalse("Save should not be present for troubleshooter under look and feel",
                isElementPresent(Locator.button("Save")));

        log("Verify configure page elements changes cannot be saved");
        goToAdminConsole().goToSettingsSection().clickConfigurePageElements();
        checker().verifyFalse("Save should not be present for troubleshooter under configure page element",
                isElementPresent(Locator.button("Save")));

        log("Verify External Redirect Hosts changes cannot be saved");
        goToAdminConsole().goToSettingsSection().clickExternalRedirectHosts();
        checker().verifyFalse("Save should not be present for troubleshooter under External Redirect Hosts",
                isElementPresent(Locator.button("Save")));

//        log("Verify authentication changes cannot be saved");
//        goToAdminConsole().goToSettingsSection().clickAuthentication();
//        checker().verifyFalse("Save should not be present for troubleshooter under authentication",
//                isElementPresent(Locator.button("Save and Finish")));

        stopImpersonating();

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
