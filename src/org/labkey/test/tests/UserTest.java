/*
 * Copyright (c) 2007-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class})
public class UserTest extends SecurityTest
{
    private static final String[] REQUIRED_FIELDS = {"FirstName", "LastName", "Phone", "Mobile"};
    private static final String TEST_PASSWORD = "testPassword";

    /**copied from LoginController.EMAIL_PASSWORDMISMATCH_ERROR, but needs to be broken into multiple separate sentences,
     *  the search function can't handle the line breaks
     */
    public static final String[] EMAIL_PASSWORD_MISMATCH_ERROR =
            {"The e-mail address and password you entered did not match any accounts on file.",
             "Note: Passwords are case sensitive; make sure your Caps Lock is off."};


    //users for change e-mail tests.  Both included at top level so they can be included in the clean up.
    // only one should exist at any one time, but by deleting both we ensure that nothing persists even if
    // the test fails
    protected static final String NORMAL_USER2 = "user2_securitytest@security.test";
    protected static final String NORMAL_USER2_ALTERNATE = "not-user2@security.test";

    @Override
    protected boolean isQuickTest()
    {
        return true;
    }

    @Test @Override
    public void testSteps()
    {
        super.testSteps();

        siteUsersTest();
        requiredFieldsTest();
        simplePasswordResetTest();
        changeUserEmailTest();
        deactivatedUserTest();
        longUserPropertiesTest();
        specialCharactersUserPropertiesTest();
        scriptInjectionUserPropertiesTest();
        addCustomPropertiesTest();
    }


    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        Locator.XPathLocator button = Locator.tagContainingText("span", "Change User Properties");
        waitForElement(button);
        click(button);
        checkRequiredField("FirstName", false);
        clickButton("Save");

        deleteUsers(false, NORMAL_USER2, NORMAL_USER2_ALTERNATE); // Deleted/renamed during test. Only needed during pre-clean
    }

    @LogMethod
    private void siteUsersTest()
    {
        goToSiteUsers();
        assertTextPresent("Last Login");
        assertTextPresent("Last Name");
        assertTextPresent("Active");

        goToMyAccount();
        assertTextPresent("First Name");
        assertTextPresent("Last Login");

        impersonate(NORMAL_USER);

        goToMyAccount();
        assertTextPresent("First Name");
        assertTextNotPresent("Last Login");

        stopImpersonating();
    }

    // Issue 3876: Add more security tests
    @LogMethod
    private void changeUserEmailTest()
    {
        boolean fromAdmin = false;
        //get appropriate user
        String userEmail = getEmailChangeableUser();
        String newUserEmail = NORMAL_USER2_ALTERNATE;

        //change their email address
        changeUserEmail(userEmail, newUserEmail);

        signOut();

        //verify can log in with new address
        signIn(newUserEmail, TEST_PASSWORD, true);

        signOut();

        //verify can't log in with old address
        signInShouldFail(userEmail, TEST_PASSWORD, EMAIL_PASSWORD_MISMATCH_ERROR);

        simpleSignIn();

        deleteUsers(true, newUserEmail);
    }

    @LogMethod
    private void deactivatedUserTest()
    {
        goToSiteUsers();
        DataRegionTable usersTable = new DataRegionTable("Users", this, true, true);
        int row = usersTable.getRow("Email", NORMAL_USER);
        String userId = usersTable.getDataAsText(row, "User Id");
        String adminUserId = usersTable.getDataAsText(usersTable.getRow("Email", PROJECT_ADMIN_USER), "User Id");
        usersTable.checkCheckbox(row);
        clickButton("Deactivate");
        clickButton("Deactivate");
        assertTextNotPresent(NORMAL_USER);

        log("Deactivated users shouldn't show up in issues 'Assign To' list");
        goToProjectHome();
        goToModule("Issues");
        clickAndWait(Locator.linkWithText("New Issue"));
        assertElementNotPresent(createAssignedToOptionLocator(userId));
        assertTextNotPresent(displayNameFromEmail(NORMAL_USER));
        assertElementPresent(createAssignedToOptionLocator(adminUserId));
        assertTextPresent(displayNameFromEmail(PROJECT_ADMIN_USER));

        log("Reactivate user");
        goToSiteUsers();
        assertTextNotPresent(NORMAL_USER);
        clickAndWait(Locator.linkWithText("include inactive users"));
        usersTable = new DataRegionTable("Users", this, true, true);
        row = usersTable.getRow("Email", NORMAL_USER);
        assertEquals(NORMAL_USER + " should not be 'Active'", "false", usersTable.getDataAsText(row, "Active"));
        usersTable.checkCheckbox(row);
        clickButton("Re-Activate");
        clickButton("Re-activate");
        usersTable = new DataRegionTable("Users", this, true, true);
        row = usersTable.getRow("Email", NORMAL_USER);
        assertEquals(NORMAL_USER + " should be 'Active'", "true", usersTable.getDataAsText(row, "Active"));
    }

    private Locator createAssignedToOptionLocator(String username)
    {
        return Locator.xpath("//select[@id='assignedTo']/option[@value='" + username +  "']");
    }

    /**if user NORMAL_USER2 does not exist, create them,
     * give them password TEST_PASSWORD, and sign them in.
     * @return email address of user
     */
    private String getEmailChangeableUser()
    {
        createUserAndNotify(NORMAL_USER2, NORMAL_USER);
        clickProject("Home");
        setInitialPassword(NORMAL_USER2, TEST_PASSWORD);

        return NORMAL_USER2;
    }


    /**
     * Selects required user information fields and tests to see they are
     * enforced in the user info form.
     */
    @LogMethod
    private void requiredFieldsTest()
    {
        goToSiteUsers();
        clickButton("Change User Properties");

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickButton("Save");
        clickButton("Change User Properties", defaultWaitForPage);

        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickButton("Save", 0);
        clickButton("Change User Properties", defaultWaitForPage);

        checkRequiredField("FirstName", true);
        clickButton("Save");

        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");
        setFormElement(Locator.name("quf_FirstName"), "");
        clickButton("Submit");

        assertTextPresent("This field is required");
        clickButton("Cancel");

        clickButton("Show Users");
    }

    @LogMethod
    private void simplePasswordResetTest()
    {
        enableEmailRecorder();

        goToSiteUsers();
        clickAndWait(Locator.linkWithText(displayNameFromEmail(NORMAL_USER)));
        prepForPageLoad();
        clickButtonContainingText("Reset Password", 0);
        assertAlertContains("You are about to clear the user's current password");
        waitForPageToLoad();
        clickAndWait(Locator.linkWithText("Done"));
        // View reset password email.
//        clickProject(PROJECT_NAME);
        goToProjectHome();
        goToModule("Dumbster");
        click(Locator.linkContainingText("Reset Password Notification")); // Expand message.

        clickAndWait(Locator.linkContainingText("setPassword")); // Set Password URL
        assertTextPresent(NORMAL_USER);
        setFormElement(Locator.id("password"), TEST_PASSWORD);
        setFormElement(Locator.id("password2"), TEST_PASSWORD);

        clickButton("Set Password");

        clickUserMenuItem("Sign Out");
        clickAndWait(Locator.linkWithText("Sign In"));
        setFormElement(Locator.id("email"), NORMAL_USER);
        setFormElement(Locator.id("password"), TEST_PASSWORD);
        clickButton("Sign In");
        assertSignOutAndMyAccountPresent();
        assertTextPresent(NORMAL_USER);
        assertTextNotPresent("Sign In");

        signOut();
        simpleSignIn();
    }

    private void checkRequiredField(String name, boolean select)
    {
        Locator fieldLocator = Locator.xpath("//div[@class='gwt-Label' and contains(text(),'" + name + "')]");
        waitForElement(fieldLocator);

        click(fieldLocator);

        String prefix = getPropertyXPath("Field Properties");
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Validators']"));

        Locator checkboxLocator = Locator.xpath(prefix + "//span/input[@name='required']");

        if (isChecked(checkboxLocator) != select)
            click(checkboxLocator);
    }

    private void verifyFieldChecked(String name)
    {
        Locator fieldLocator = Locator.xpath("//div[@class='gwt-Label' and contains(text(),'" + name + "')]");
        waitForElement(fieldLocator);

        click(fieldLocator);

        String prefix = getPropertyXPath("Field Properties");
        click(Locator.xpath(prefix + "//span[contains(@class,'x-tab-strip-text') and text()='Validators']"));

        Locator checkboxLocator = Locator.xpath(prefix + "//span/input[@name='required']");

        assertTrue("Checkbox not set for element: " + name, isChecked(checkboxLocator));
    }

    private void navigateToUserDetails(String userName)
    {
        Locator details = Locator.xpath("//td[.='" + userName + "']/..//td[contains(@class, 'labkey-details')]/a");
        waitForElement(details);
        clickAndWait(details);
    }

    @LogMethod
    public void longUserPropertiesTest()
    {
        goToSiteUsers();
        navigateToUserDetails(PROJECT_ADMIN_USER);
        clickButton("Edit");

        StringBuilder illegalLongProperty = new StringBuilder();
        final int maxFieldLength = 64;
        for (int i = 0; i < maxFieldLength + 1; i++)
            illegalLongProperty.append("X");

        StringBuilder illegalLongDescription = new StringBuilder();
        final int maxDescriptionLength = 255;
        for (int i = 0; i < maxDescriptionLength + 1; i++)
            illegalLongDescription.append("X");

        log("Set illegal properties");
        setFormElement(Locator.name("quf_DisplayName"), illegalLongProperty.toString());
        setFormElement(Locator.name("quf_FirstName"), illegalLongProperty.toString());
        setFormElement(Locator.name("quf_LastName"), illegalLongProperty.toString());
        setFormElement(Locator.name("quf_Phone"), illegalLongProperty.toString());
        setFormElement(Locator.name("quf_Mobile"), illegalLongProperty.toString());

        log("Check error messages");
        clickButton("Submit");
        WebElement errors = Locator.css(".labkey-error").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT);
        String errorText = errors.getText();

        assertTrue("No error for 'Display Name'", errorText.contains(String.format("Value is too long for column 'DisplayName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'First Name'", errorText.contains(String.format("Value is too long for column 'FirstName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Last Name'", errorText.contains(String.format("Value is too long for column 'LastName', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Phone'", errorText.contains(String.format("Value is too long for column 'Phone', a maximum length of %d is allowed.", maxFieldLength)));
        assertTrue("No error for 'Mobile'", errorText.contains(String.format("Value is too long for column 'Mobile', a maximum length of %d is allowed.", maxFieldLength)));

        clickButton("Cancel");

        Locator userInfoPanel = Locator.id("SiteUsers");
        String userInfo = getText(userInfoPanel);
        assertFalse("Illegal text [XXX] present after cancel", userInfo.contains(illegalLongProperty.substring(0, 3)));
    }

    @LogMethod
    public void specialCharactersUserPropertiesTest()
    {
        goToSiteUsers();
        navigateToUserDetails(PROJECT_ADMIN_USER);
        clickButton("Edit");

        setFormElement(Locator.name("quf_FirstName"), TRICKY_CHARACTERS_FOR_PROJECT_NAMES);
        setFormElement(Locator.name("quf_LastName"), TRICKY_CHARACTERS_FOR_PROJECT_NAMES);

        clickButton("Submit");

        assertElementPresent(Locator.css("#SiteUsers td.labkey-form-label + td").withText(TRICKY_CHARACTERS_FOR_PROJECT_NAMES), 2);
    }

    @LogMethod
    public void scriptInjectionUserPropertiesTest()
    {
        goToSiteUsers();
        navigateToUserDetails(PROJECT_ADMIN_USER);
        clickButton("Edit");

        setFormElement(Locator.name("quf_FirstName"), INJECT_CHARS_1);
        setFormElement(Locator.name("quf_LastName"), INJECT_CHARS_1);

        clickButton("Submit");

        assertElementPresent(Locator.css("#SiteUsers td.labkey-form-label + td").withText(INJECT_CHARS_1), 2);
    }

    private static final String PROP_NAME1 = "Institution";
    private static final String PROP_NAME2 = "InstitutionId";

    @LogMethod
    private void addCustomPropertiesTest()
    {
        int defaultUserFieldCount = 7;

        goToSiteUsers();
        clickButton("Change User Properties");

        waitForText("Add Field");
        _listHelper.addField("Field Properties", PROP_NAME1, PROP_NAME1, ListHelper.ListColumnType.String);
        _listHelper.addField("Field Properties", PROP_NAME2, PROP_NAME2, ListHelper.ListColumnType.Integer);

        try {
            clickButton("Save");

            assertTextPresent(PROP_NAME1, PROP_NAME2);

            navigateToUserDetails(NORMAL_USER);
            assertTextPresent(PROP_NAME1, PROP_NAME2);

            clickButton("Edit");
            assertTextPresent(PROP_NAME1, PROP_NAME2);
            clickButton("Cancel");
        }
        finally
        {
            goToSiteUsers();
            clickButton("Change User Properties");

            waitForText("Add Field");

            _listHelper.deleteField("Field Properties", defaultUserFieldCount);
            _listHelper.deleteField("Field Properties", defaultUserFieldCount + 1);

            clickButton("Save");
        }
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
