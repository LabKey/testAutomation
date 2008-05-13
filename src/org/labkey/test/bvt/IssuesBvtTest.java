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
import org.labkey.test.drt.IssuesTest;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 2, 2007
 */
public class IssuesBvtTest extends IssuesTest
{
    private static final String[] REQUIRED_FIELDS = {"Title", "AssignedTo", "Type", "Area", "Priority", "Milestone",
                "NotifyList", "String1", "Int1"};

    protected void doTestSteps()
    {
        super.doTestSteps();

        // back to grid view
        clickLinkWithText("view grid");

        requiredFieldsTest();
        viewSelectedDetailsTest();
    }

    private void requiredFieldsTest()
    {
        clickNavButton("Admin");
        setFormElement("int1", "Contract Number");
        setFormElement("string1", "Customer Name");
        clickNavButton("Update Custom Fields");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
            checkRequiredField(field, true);

        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("Admin");

        //setWorkingForm("requiredFieldsForm");
        for (String field : REQUIRED_FIELDS)
        {
            verifyFieldChecked(field);
            checkRequiredField(field, false);
        }
        clickNavButton("Update Required Fields");

        checkRequiredField("Title", true);
        clickNavButton("Update Required Fields");
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        clickNavButton("Submit");

        assertTextPresent("Field Title cannot be null.");
        clickNavButton("View Grid");
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

    private void viewSelectedDetailsTest()
    {
        setFilter("Issues", "Status", "<has any value>");
        clickCheckbox(".toggle", false);
        clickNavButton("View Details");
        assertTextPresent("a bright flash of light");
        assertTextPresent("don't believe the hype");
        clickLinkWithText("view grid");
    }
}
