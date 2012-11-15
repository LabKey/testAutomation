package org.labkey.test.tests;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DevModeOnlyTest;
import org.labkey.test.util.ListHelper;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 11/14/12
 */
public class HiddenEmailTest extends BaseWebDriverTest implements DevModeOnlyTest
{
    private static final String TEST_GROUP = "HiddenEmail Test group";
    private static final String ADMIN_USER = "experimental_admin@experimental.test";
    private static final String IMPERSONATED_USER = "experimental_user@experimental.test";
    private static final String CHECKED_USER = "experimental_user2@experimental.test";
    private static final String EMAIL_TEST_LIST = "My Users";
    private static final String EMAIL_VIEW = "emailView";


    @Override
    protected String getProjectName()
    {
        return "Hidden Email Test";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);

        deleteUsers(afterTest, IMPERSONATED_USER, CHECKED_USER, ADMIN_USER);
        try{deleteGroup(TEST_GROUP);}catch(Throwable t){/**/}
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {

        doHiddenEmailTest();
    }

    private void doHiddenEmailTest()
    {
        setupHiddenEmailTest();
        verifyHiddenEmailTest();

    }
    private void setupHiddenEmailTest()
    {
        // Create users and groups
        createUser(ADMIN_USER, null);
        addUserToGroup("Site Administrators", ADMIN_USER);
        impersonate(ADMIN_USER); // Use created user to ensure we have a known 'Modified by' column for created users
        createGlobalPermissionsGroup(TEST_GROUP, IMPERSONATED_USER, CHECKED_USER);
        _containerHelper.createProject(getProjectName(), null);
        setSiteGroupPermissions(TEST_GROUP, "Reader");
        clickButton("Save and Finish");
        stopImpersonating();
        impersonate(CHECKED_USER);
        goToMyAccount();
        clickButton("Edit");
        setFormElement(Locator.name("quf_FirstName"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        stopImpersonating();

        // Create list
        // impersonate(ADMIN_USER); // TODO: 16513: Experimental email hiding doesn't hide List.ModifiedBy
        ListHelper.ListColumn userColumn = new ListHelper.ListColumn("user", "user", ListHelper.ListColumnType.String, "", new ListHelper.LookupInfo(getProjectName(), "core", "Users"));
        _listHelper.createList(getProjectName(), EMAIL_TEST_LIST, ListHelper.ListColumnType.AutoInteger, "Key", userColumn);
        clickButton("Done");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(CHECKED_USER));
        clickButton("Submit");
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_user"), displayNameFromEmail(ADMIN_USER));
        clickButton("Submit");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("user/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("user/ModifiedBy/Email", "Email");
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);
//        stopImpersonating(); // TODO: 16513: Experimental email hiding doesn't hide List.ModifiedBy

        // Create query webpart
        clickFolder(getProjectName());
        addWebPart("Query");
        selectOptionByValue(Locator.name("schemaName"), "core");
        clickRadioButtonById("selectQueryContents");
        selectOptionByValue(Locator.name("queryName"), "Users");
        submit();
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ModifiedBy/Email", "Email");
        _customizeViewsHelper.saveCustomView(EMAIL_VIEW, true);

        // Verify user permissions for hidden emails
        goToSiteGroups();
        _ext4Helper.clickExt4Tab("Permissions");
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        assertElementNotPresent(Locator.permissionButton(TEST_GROUP, "SeeEmailAddresses"));
        assertElementNotPresent(Locator.permissionButton(IMPERSONATED_USER, "SeeEmailAddresses"));
        clickButton("Save and Finish");
    }

    private void verifyHiddenEmailTest()
    {
        impersonate(IMPERSONATED_USER);
        clickFolder(getProjectName());

        log("Verify that emails cannot be seen in query webpart");
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        log("Verify that emails cannot be seen in list via lookup");
        clickLinkWithText(EMAIL_TEST_LIST);
        clickMenuButton("Views", EMAIL_VIEW);
        assertTextNotPresent(CHECKED_USER, ADMIN_USER);

        stopImpersonating();
    }
}
