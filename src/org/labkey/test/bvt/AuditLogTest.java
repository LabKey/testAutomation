package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.DataRegionTable;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Mar 13, 2008
 */
public class AuditLogTest extends BaseSeleniumWebTest
{
    public static final String USER_AUDIT_EVENT = "User events";
    public static final String GROUP_AUDIT_EVENT = "Group events";
    public static final String PROJECT_AUDIT_EVENT = "Project and folder events";
    public static final String ASSAY_AUDIT_EVENT = "Copy-to-Study assay events";

    private static final String AUDIT_TEST_USER = "audit_test_user@test.com";
    private static final String AUDIT_TEST_PROJECT = "AuditVerifyTest";

    public static final String COMMENT_COLUMN = "Comment";

    public String getAssociatedModuleDirectory()
    {
        return "audit";
    }

    protected void doCleanup() throws Exception
    {
        deleteUser(AUDIT_TEST_USER);
        deleteProject(AUDIT_TEST_PROJECT);
    }

    protected void doTestSteps() throws Exception
    {
        userAuditTest();
        groupAuditTest();
    }

    protected void userAuditTest() throws Exception
    {
        log("testing user audit events");
        createUser(AUDIT_TEST_USER, null);
        impersonate(AUDIT_TEST_USER);
        signOut();
        signIn();
        deleteUser(AUDIT_TEST_USER);

        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was added to the system", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was impersonated", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " logged out", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was deleted from the system", 10);
    }

    protected void groupAuditTest() throws Exception
    {
        log("testing group audit events");

        createProject(AUDIT_TEST_PROJECT);
        createPermissionsGroup("Testers");
        assertPermissionSetting("Testers", "No Permissions");
        setPermissions("Testers", "Editor");

        clickLinkWithText("manage group", 1);
        setFormElement("names", AUDIT_TEST_USER);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership", "large");
        deleteUser(AUDIT_TEST_USER);
        deleteProject(AUDIT_TEST_PROJECT);

        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "The group: Testers was created", 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, "The permissions for group Testers were changed from No Permissions to Editor", 10);
        verifyAuditEvent(this, GROUP_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was added as a member to Group: Testers", 10);
        verifyAuditEvent(this, USER_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_USER + " was deleted from the system", 10);

        log("testing project audit events");
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was created", 5);
        verifyAuditEvent(this, PROJECT_AUDIT_EVENT, COMMENT_COLUMN, AUDIT_TEST_PROJECT + " was deleted", 5);
    }

    public static void verifyAuditEvent(BaseSeleniumWebTest instance, String eventType, String column, String msg, int rowsToSearch)
    {
        if (!instance.isTextPresent("Audit Log"))
        {
            instance.ensureAdminMode();

            instance.clickLinkWithText("Admin Console");
            instance.clickLinkWithText("audit log");
        }

        if (!instance.getSelectedOptionText("view").equals(eventType))
        {
            instance.selectOptionByText("view", eventType);
            instance.waitForPageToLoad();
        }
        instance.log("searching for audit entry: " + msg);
        DataRegionTable table = new DataRegionTable("audit", instance);
        int i = table.getColumn(column);
        assertTrue("Text '" + msg + "' was not present", findTextInDataRegion(table, i, msg, rowsToSearch));
    }

    public static boolean findTextInDataRegion(DataRegionTable table, int column, String txt, int rowsToSearch)
    {
        for (int row = 0; row < rowsToSearch; row++)
        {
            String value = table.getDataAsText(row, column-1);
            if (value.contains(txt))
                return true;
        }
        return false;
    }
}
