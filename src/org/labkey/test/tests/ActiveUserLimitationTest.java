package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.LimitActiveUserPage;
import org.labkey.test.pages.user.ShowUsersPage;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class ActiveUserLimitationTest extends BaseWebDriverTest
{
    private static final String USER1 = "user1@activeuserlimitation.test";
    private static final String USER1_DISPLAY_NAME = "user1";
    private static final String USER2 = "user2@activeuserlimitation.test";
    private static final String USER3 = "user3@activeuserlimitation.test";

    @BeforeClass
    public static void setupProject()
    {
        ActiveUserLimitationTest init = (ActiveUserLimitationTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Active User Limitation Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, USER1, USER2, USER3);
    }

    @Before
    public void deleteUsers()
    {
        _userHelper.deleteUsers(false, USER1, USER2, USER3);
        resetLimits();
    }

    @Test
    public void testUserWarning()
    {
        String warningLevel = String.valueOf(getActiveUsers() + 2);
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.userWarning("Yes").
                limitActiveUsers("No").
                setUserWarningLevel(warningLevel).
                setUserWarningMessage("You have been warned..! ${WarningLevel} is the limit and currently server has ${ActiveUsers} users");
        limitActiveUserPage.save();

        log("User warning limit is not reached yet");
        _userHelper.createUser(USER1);
        goToSiteUsers();
        assertFalse("Banner should not be present", isBannerPresent());

        log("Verifying the message when user warning limit is reached");
        _userHelper.createUser(USER2);
        String warningMsg = "You have been warned..! " + warningLevel + " is the limit and currently server has " + getActiveUsers() + " users";
        goToSiteUsers();
        assertEquals("Incorrect warning banner message", warningMsg, getBannerText());
        assertTrue("User should have been created with warning message", isTextPresent(USER2));
    }

    @Test
    public void testUserLimits()
    {
        String limitLevel = String.valueOf(getActiveUsers() + 1);
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.limitActiveUsers("Yes").
                userWarning("No").
                setUserLimitLevel(limitLevel).
                setUserLimitMessage("You cannot do this..! ${LimitLevel} is the limit and currently server has ${ActiveUsers} users");
        limitActiveUserPage.save();

        log("User limit is not reached yet");
        _userHelper.createUser(USER1);
        goToSiteUsers();
        assertTrue("Banner should be present", isBannerPresent());
        assertTrue("User should have been created with banner message", isTextPresent(USER1));

        log("Verifying the message when user limit is reached");
        _userHelper.createUser(USER2, false, false);
        String warningMsg = "You cannot do this..! " + limitLevel + " is the limit and currently server has " + getActiveUsers() + " users";
        goToSiteUsers();
        assertEquals("Incorrect warning banner message", warningMsg, getBannerText());
        assertFalse("User should not have been created", isTextPresent(USER2));
    }

    @Test
    public void testSystemUserLimitation()
    {
        log("Create the user before the limits are set");
        _userHelper.createUser(USER1);

        log("Setting the limit same as current active user");
        String limitLevel = String.valueOf(getActiveUsers());
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.limitActiveUsers("Yes").
                userWarning("No").
                setUserLimitLevel(limitLevel).
                setUserLimitMessage("You cannot do this..! ${LimitLevel} is the limit and currently server has ${ActiveUsers} users");
        limitActiveUserPage.save();

        log("Marking previously created user as system user");
        goToSiteUsers();
        waitAndClickAndWait(Locator.linkWithText(USER1_DISPLAY_NAME));
        clickButton("Edit");
        checkCheckbox(Locator.name("quf_System"));
        clickButton("Submit");

        log("Creation of one more user should be be allowed");
        _userHelper.createUser(USER2);
        goToSiteUsers();
        assertTrue("Banner should be present", isBannerPresent());
        assertTrue("User should have been created with banner message", isTextPresent(USER2));
    }

    @Test
    public void testActivateAndDeactivateUserWithLimits()
    {
        log("Creating the 2 users before the limits are set");
        _userHelper.createUser(USER1);
        _userHelper.createUser(USER2);

        log("Setting the limit same as current active user");
        String limitLevel = String.valueOf(getActiveUsers());
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.limitActiveUsers("Yes").
                userWarning("No").
                setUserLimitLevel(limitLevel).
                setUserLimitMessage("You cannot do this..! ${LimitLevel} is the limit and currently server has ${ActiveUsers} users");
        limitActiveUserPage.save();

        log("Deactivate the 2 users");
        ShowUsersPage usersPage = goToSiteUsers();
        usersPage.getUsersTable().setFilter("Email", "Equals One Of (example usage: a;b;c)", USER1 + ";" + USER2);
        usersPage.getUsersTable().checkCheckbox(0);
        usersPage.getUsersTable().checkCheckbox(1);
        usersPage.getUsersTable().clickHeaderButton("Deactivate");
        waitForElement(Locator.tagWithText("span", "Deactivate"));
        clickButton("Deactivate");

        log("Create another user");
        _userHelper.createUser(USER3);
        goToSiteUsers();
        assertTrue("User should have been created with banner message", isTextPresent(USER3));

        log("Reactivate the user");
        usersPage = goToSiteUsers();
        usersPage.includeInactiveUsers();
        usersPage.getUsersTable().setFilter("Email", "Equals One Of (example usage: a;b;c)", USER1 + ";" + USER2);
        usersPage.getUsersTable().checkCheckbox(0);
        usersPage.getUsersTable().checkCheckbox(1);
        usersPage.getUsersTable().clickHeaderButton("Reactivate");
        waitForElement(Locator.tagWithText("span", "Reactivate"));
        clickButton("Reactivate");
        assertTrue("Missing error message", isTextPresent("Failed to activate user2: User limit has been reached so no more users can be reactivated on this deployment."));
    }

    private int getActiveUsers()
    {
        return _userHelper.getUsers().getUsersInfo().size();
    }

    private String getBannerText()
    {
        if (isBannerPresent())
            return getText(Locator.tagWithClassContaining("div", "lk-dismissable-warn"));
        else
            return "";
    }

    private void resetLimits()
    {
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.limitActiveUsers("No").
                userWarning("No");
        limitActiveUserPage.save();
    }

    private boolean isBannerPresent()
    {
        return isElementPresent(Locator.tagWithClassContaining("div", "alert-dismissable"));
    }
}
