/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

    protected void doTestSteps()
    {
        super.doTestSteps();

        siteUsersTest();
        requiredFieldsTest();
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
        assertTextPresent("User Id");
        assertTextPresent("Last Login");

        clickLinkWithText("My Account");
        assertTextPresent("User Id");
        assertTextPresent("Last Login");

        impersonate(NORMAL_USER);

        clickLinkWithText("My Account");

        assertTextNotPresent("User Id");
        assertTextNotPresent("Last Login");

        signIn();
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

        clickNavButton("Show Grid");
    }

    private void checkRequiredField(String name, boolean select)
    {
        Locator checkBoxLocator = Locator.checkboxByNameAndValue("requiredFields", name, false);

        if (select)
            checkCheckbox("requiredFields", name, false);
        else
        {
            if (isChecked(checkBoxLocator))
                click(checkBoxLocator);
        }
    }

    private void verifyFieldChecked(String fieldName)
    {
        if (isChecked(Locator.checkboxByNameAndValue("requiredFields", fieldName, false)))
            return;

        assertFalse("Checkbox not set for element: " + fieldName, false);
    }

    private void navigateToUserDetails(String userName)
    {
        selenium.click("//td[.='" + userName + "']/..//a[.='[Details]']");
        selenium.waitForPageToLoad("30000");
    }
}
