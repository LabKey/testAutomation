/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.DataRegionTable;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 11, 2007
 */
public class UserTest extends SecurityTest
{
    private static final String[] REQUIRED_FIELDS = {"FirstName", "LastName", "Phone", "Mobile", "Pager",
                "IM", "Description"};
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


    protected void doTestSteps()
    {
        super.doTestStepsSetDetph(true);

        siteUsersTest();
        requiredFieldsTest();
        simplePasswordResetTest();
        changeUserEmailTest();
        deactivatedUserTest();
        addCustomPropertiesTest();
    }


    protected void doCleanup(boolean afterTest)
    {
        super.doCleanup(afterTest);
        clickButton("Change User Properties");
        checkRequiredField("FirstName", false);
        clickButton("Save");

        deleteUsers(false, NORMAL_USER2, NORMAL_USER2_ALTERNATE);
    }

    private void siteUsersTest()
    {
        goToSiteUsers();
        assertTextPresent("Last Login");
        assertTextPresent("Last Name");
        assertTextPresent("Active");

        goToMyAccount();
        assertTextPresent("User Id");
        assertTextPresent("Last Login");

        impersonate(NORMAL_USER);

        goToMyAccount();
        assertTextPresent("User Id");
        assertTextNotPresent("Last Login");

        stopImpersonating();
    }

    // Issue 3876: Add more security tests
    private void changeUserEmailTest()//boolean fromAdmin)
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

        deleteUser(newUserEmail);
    }

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
        clickLinkWithText(getProjectName());
        goToModule("Issues");
        clickLinkWithText("New Issue");
        assertElementNotPresent(Locator.css("#assignedTo option[value="+userId+"]"));
        assertTextNotPresent(displayNameFromEmail(NORMAL_USER));
        assertElementPresent(Locator.css("#assignedTo option[value="+adminUserId+"]"));
        assertTextPresent(displayNameFromEmail(PROJECT_ADMIN_USER));

        log("Reactivate user");
        goToSiteUsers();
        assertTextNotPresent(NORMAL_USER);
        clickLinkWithText("include inactive users");
        usersTable = new DataRegionTable("Users", this, true, true);
        row = usersTable.getRow("Email", NORMAL_USER);
        Assert.assertEquals(NORMAL_USER + " should not be 'Active'", "false", usersTable.getDataAsText(row, "Active"));
        usersTable.checkCheckbox(row);
        clickButton("Re-Activate");
        clickButton("Re-activate");
        usersTable = new DataRegionTable("Users", this, true, true);
        row = usersTable.getRow("Email", NORMAL_USER);
        Assert.assertEquals(NORMAL_USER + " should be 'Active'", "true", usersTable.getDataAsText(row, "Active"));
    }

    /**if user NORMAL_USER2 does not exist, create them,
     * give them password TEST_PASSWORD, and sign them in.
     * @return email address of user
     */
    private String getEmailChangeableUser()
    {
        createUserAndNotify(NORMAL_USER2, NORMAL_USER);
        clickLinkContainingText("Home");
        setInitialPassword(NORMAL_USER2, TEST_PASSWORD);

        return NORMAL_USER2;
    }


    /**
     * Selects required user information fields and tests to see they are
     * enforced in the user info form.
     */
    private void requiredFieldsTest()
    {
        goToSiteUsers();
        clickButton("Change User Properties");

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickButton("Save");
        clickButton("Change User Properties");

        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickButton("Save");
        clickButton("Change User Properties");

        checkRequiredField("FirstName", true);
        clickButton("Save");

        navigateToUserDetails(NORMAL_USER);
        clickButton("Edit");
        clickButton("Submit");

        assertTextPresent("This field is required");
        clickButton("Cancel");

        clickButton("Show All Users");
    }

    private void simplePasswordResetTest()
    {
        enableEmailRecorder();

        goToSiteUsers();
        clickLinkWithText(displayNameFromEmail(NORMAL_USER));
        selenium.chooseOkOnNextConfirmation();
        clickButtonContainingText("Reset Password", 0);
        Assert.assertTrue(selenium.getConfirmation().startsWith("You are about to clear the user's current password"));
        waitForPageToLoad();
        clickLinkWithText("Done");
        // View reset password email.
        clickLinkWithText(PROJECT_NAME);
        goToModule("Dumbster");
        clickLinkContainingText("Reset Password Notification", false); // Expand message.

        clickLinkContainingText("setPassword"); // Set Password URL
        assertTextPresent(NORMAL_USER);
        setFormElement("password", TEST_PASSWORD);
        setFormElement("password2", TEST_PASSWORD);

        clickButton("Set Password");

        clickUserMenuItem("Sign Out");
        clickLinkWithText("Sign In");
        setText("email", NORMAL_USER);
        setText("password", TEST_PASSWORD);
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

        Assert.assertTrue("Checkbox not set for element: " + name, isChecked(checkboxLocator));
    }

    private void navigateToUserDetails(String userName)
    {
        Locator details = Locator.xpath("//td[.='" + userName + "']/..//td[contains(@class, 'labkey-details')]/a");
        waitForElement(details);
        click(details);

        waitForPageToLoad(30000);
    }

    private static final String PROP_NAME1 = "Institution";
    private static final String PROP_NAME2 = "InstitutionId";

    private void addCustomPropertiesTest()
    {
        goToSiteUsers();
        clickButton("Change User Properties");

        waitForText("Add Field");
        int firstIdx = findLastUserCustomField();
        addField("Field Properties", firstIdx++, PROP_NAME1, PROP_NAME1, ListHelper.ListColumnType.String);
        addField("Field Properties", firstIdx, PROP_NAME2, PROP_NAME2, ListHelper.ListColumnType.Integer);

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

            deleteField("Field Properties", firstIdx--);
            deleteField("Field Properties", firstIdx);

            clickButton("Save");
        }
    }

    /**
     * Helper to find the last custom property in the section, just so we don't stomp on any that were added manually.
     */
    private int findLastUserCustomField()
    {
        for (int i = 15; i > 6; i--)
        {
            String prefix = getPropertyXPath("Field Properties");
            Locator field = Locator.xpath(prefix + "//input[@name='ff_name" + i + "']");

            if (isElementPresent(field))
                return i+1;
        }
        // there are currently 6 default non-editable fields, without any customizations, the first
        // field would be 7th.
        return 7;
    }
}
