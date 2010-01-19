/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.Locator;
import org.labkey.test.drt.SecurityTest;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 11, 2007
 */
public class UserBvtTest extends SecurityTest
{
    private static final String[] REQUIRED_FIELDS = {"FirstName", "LastName", "Phone", "Mobile", "Pager",
                "IM", "Description"};
    private static final String TEST_PASSWORD = "testPassword";

    protected void doTestSteps()
    {
        super.doTestSteps();

        siteUsersTest();
        requiredFieldsTest();
        passwordTest();
    }

    protected void doCleanup()
    {
        super.doCleanup();
        clickNavButton("Preferences");
        checkRequiredField("FirstName", false);
        clickNavButton("Update");
    }

    private void siteUsersTest()
    {
        clickLinkWithText("Site Users");
        assertTextPresent("Last Login");
        assertTextPresent("Last Name");
        assertTextPresent("Active");

        clickLinkWithText("My Account");
        assertTextPresent("User Id");
        assertTextPresent("Last Login");

        impersonate(NORMAL_USER);

        clickLinkWithText("My Account");

        assertTextNotPresent("User Id");
        assertTextNotPresent("Last Login");

        stopImpersonating();
    }

    /**
     * Selects required user information fields and tests to see they are
     * enforced in the user info form.
     */
    private void requiredFieldsTest()
    {
        clickLinkWithText("Site Users");
        clickNavButton("Preferences");

        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickNavButton("Update");
        clickNavButton("Preferences");

        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickNavButton("Update");
        clickNavButton("Preferences");

        checkRequiredField("FirstName", true);
        clickNavButton("Update");

        navigateToUserDetails(NORMAL_USER);
        clickNavButton("Edit");
        clickNavButton("Submit");

//        assertTextPresent("Field firstName cannot be null.");
        assertTextPresent("This field is required");

        clickNavButton("Show All Users");
    }

    private void passwordTest()
    {
        enableModule(PROJECT_NAME, "Dumbster");
        addWebPart("Mail Record");
        checkCheckbox("emailRecordOn");

        clickLinkWithText("Site Users");
        clickLinkWithText(NORMAL_USER);
        pushLocation();
        clickButtonContainingText("Reset Password");
        popLocation();
        // View reset password email.
        clickLinkWithText(PROJECT_NAME);
        clickLinkContainingText("Reset Password Notification", 0); // Expand message.

        clickLinkContainingText("setPassword"); // Set Password URL
        assertTextPresent(NORMAL_USER);
        setFormElement("password", TEST_PASSWORD);
        setFormElement("password2", TEST_PASSWORD);

        clickButton("Set Password", 0);

        waitAndClick(Locator.linkWithText("Sign Out"));
        waitAndClick(Locator.linkWithText("Sign In"));
        waitForPageToLoad();
        setText("email", NORMAL_USER);
        setText("password", TEST_PASSWORD);
        clickButton("Sign In", 0);
        waitForPageToLoad();
        assertTextPresent("Sign Out", NORMAL_USER);
        assertTextNotPresent("Sign In");

        signOut();
        simpleSignIn();
    }

    private void checkRequiredField(String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name);

        if (select)
            checkCheckbox("requiredFields", name);
        else
        {
            if (isChecked(checkBoxLocator))
                click(checkBoxLocator);
        }
    }

    private void verifyFieldChecked(String fieldName)
    {
        if (isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName)))
            return;

        assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    private void navigateToUserDetails(String userName)
    {
        selenium.click("//td[.='" + userName + "']/..//td/a[.='details']");
        selenium.waitForPageToLoad("30000");
    }
}
