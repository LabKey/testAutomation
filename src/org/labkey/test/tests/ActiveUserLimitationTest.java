package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
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
    private static final String USER1 = "limited_user1@activeuserlimitation.test";
    private static final String USER2 = "limited_user2@activeuserlimitation.test";
    private static final String USER3 = "limited_user3@activeuserlimitation.test";

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
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, USER1, USER2, USER3);
    }

    @Before
    public void deleteUsers() throws Exception
    {
        _userHelper.deleteUsers(false, USER1, USER2, USER3);
    }

    @After
    public void resetUserLimits() throws Exception
    {
        LimitActiveUserPage.resetUserLimits(createDefaultConnection());
    }

    @Test
    public void validateLimitActiveUserPage()
    {
        log("Validating User limit < warning limit");
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        String error = limitActiveUserPage.enableUserWarning(true).enableUserLimit(true)
                .setUserLimitLevel("99")
                .setUserWarningLevel("100")
                .saveExpectingError();

        assertEquals("Invalid error message", "User limit level must be greater than or equal to user warning level.", error);

        log("Validating user limit and warning limit is integer");
        error = limitActiveUserPage.enableUserLimit(true).enableUserWarning(true)
                .setUserLimitLevel("99FA")
                .setUserWarningLevel("GH90")
                .saveExpectingError();
        assertEquals("Invalid error message", "userLimitLevel: Please enter a valid integer value\n" +
                "userWarningLevel: Please enter a valid integer value", error);
    }

    @Test
    public void testUserWarning()
    {
        String warningLevel = String.valueOf(getActiveUsers() + 2);
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.enableUserWarning(true).
                enableUserLimit(false).
                setUserWarningLevel(warningLevel).
                setUserLimitLevel("100").
                setUserWarningMessage("You have been warned..! ${WarningLevel} is the limit and currently server has ${ActiveUsers} users. ");
        limitActiveUserPage.save();

        log("User warning limit is not reached yet");
        _userHelper.createUser(USER1);
        goToSiteUsers();
        assertFalse("Banner should not be present", isBannerPresent());

        log("Verifying the message when user warning limit is reached");
        _userHelper.createUser(USER2);
        String warningMsg = "You have been warned..! " + warningLevel + " is the limit and currently server has " + getActiveUsers() + " users.";
        goToSiteUsers();
        assertEquals("Incorrect warning banner message", warningMsg, getBannerText());
        assertTrue("User should have been created with warning message", isTextPresent(USER2));
    }

    @Test
    public void testUserLimits()
    {
        String limitLevel = String.valueOf(getActiveUsers() + 1);
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.enableUserLimit(true).
                enableUserWarning(false).
                setUserLimitLevel(limitLevel).
                setUserLimitMessage("You cannot do this..! ${LimitLevel} is the limit and currently server has ${ActiveUsers} users " +
                        "You can add or reactivate ${RemainingUsers} more users.");
        limitActiveUserPage.save();

        log("User limit is not reached yet");
        _userHelper.createUser(USER1);
        goToSiteUsers();
        assertTrue("Banner should be present", isBannerPresent());
        assertTrue("User should have been created with banner message", isTextPresent(USER1));

        log("Verifying the message when user limit is reached");
        CreateUserResponse response = _userHelper.createUser(USER2, false, false);
        String errMsg = response.getParsedData().get("htmlErrors").toString();
        String warningMsg = "You cannot do this..! " + limitLevel + " is the limit and currently server has " + getActiveUsers() +
                " users You can add or reactivate " + (Integer.parseInt(limitLevel) - getActiveUsers()) + " more users.";
        goToSiteUsers();
        assertEquals("Incorrect warning banner message", warningMsg, getBannerText());
        assertTrue("User creation failed", errMsg.contains("User limit has been reached so no more users can be added to this deployment."));
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
        limitActiveUserPage.enableUserLimit(true).
                enableUserWarning(false).
                setUserLimitLevel(limitLevel).
                setUserLimitMessage("You cannot do this..! ${LimitLevel} is the limit and currently server has ${ActiveUsers} users");
        limitActiveUserPage.save();

        log("Marking previously created user as system user");
        goToSiteUsers();
        waitAndClickAndWait(Locator.linkWithText(_userHelper.getDisplayNameForEmail(USER1)));
        clickButton("Edit");
        checkCheckbox(Locator.name("quf_System"));
        clickButton("Submit");

        log("Creation of one more user should be be allowed");
        _userHelper.createUser(USER2);
        goToSiteUsers();
        assertTrue("Banner should be present", isBannerPresent());
        assertTrue("User should have been created with banner message", isTextPresent(USER2));

        goToSiteUsers();
        waitAndClickAndWait(Locator.linkWithText(_userHelper.getDisplayNameForEmail(USER1)));
        clickButton("Edit");
        uncheckCheckbox(Locator.name("quf_System"));
        clickButton("Submit", 0);
        assertEquals("Incorrect error message", "User limit has been reached so you can't clear the System field.", Locator.tagWithClass("font", "labkey-error").findElement(getDriver()).getText());
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
        limitActiveUserPage.enableUserLimit(true).
                enableUserWarning(false).
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
        assertTrue("Missing error message", isTextPresent("Failed to activate " + _userHelper.getDisplayNameForEmail(USER2) + ": User limit has been reached so no more users can be reactivated on this deployment."));

        goToSiteUsers();
        assertTrue(USER1 + " should have been reactivated", isTextPresent(USER1));
    }

    @Test
    public void testWarningAndUserLimitBothSet()
    {
        log("Setting both user limit and warning limit");
        String limitLevel = String.valueOf(getActiveUsers() + 1);
        LimitActiveUserPage limitActiveUserPage = LimitActiveUserPage.beginAt(this);
        limitActiveUserPage.enableUserLimit(true).
                enableUserWarning(true).
                setUserLimitLevel(limitLevel).
                setUserWarningLevel(limitLevel).
                setUserLimitMessage("You cannot do this..!").
                setUserWarningMessage("You have been warned..!");
        limitActiveUserPage.save();

        log("Create the user");
        _userHelper.createUser(USER1);

        log("Validating only user limit message is displayed");
        goToSiteUsers();
        assertEquals("Incorrect warning banner message", "You cannot do this..!", getBannerText());
        assertTrue("User should have been created with warning message", isTextPresent(USER1));
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

    private boolean isBannerPresent()
    {
        return isElementPresent(Locator.tagWithClassContaining("div", "alert-dismissable"));
    }
}
