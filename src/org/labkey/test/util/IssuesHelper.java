/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.io.File;
import java.util.Map;

public class IssuesHelper extends AbstractHelper
{
    public IssuesHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod
    public void addIssue(Map<String, String> issue, File... attachments)
    {
        _test.goToModule("Issues");
        _test.clickButton("New Issue");

        for (Map.Entry<String, String> field : issue.entrySet())
        {
            if ("select".equals(Locator.id(field.getKey()).findElement(_test.getDriver()).getTagName()))
                _test.selectOptionByText(Locator.id(field.getKey()), field.getValue());
            else
                _test.setFormElement(Locator.id(field.getKey()), field.getValue());
        }

        for (int i = 0; i < attachments.length; i++)
        {
            if (i == 0)
                _test.click(Locator.linkWithText("Attach a file"));
            else
                _test.click(Locator.linkWithText("Attach another file"));

            _test.setFormElement(Locator.id(String.format("formFile%02d", i + 1)), attachments[i]);
        }

        _test.clickButton("Save");
    }

    @LogMethod
    public void setIssueAssignmentList(@Nullable @LoggedParam String group)
    {
        if (group != null)
        {
            _test.checkRadioButton(Locator.radioButtonByNameAndValue("assignedToMethod", "Group"));
            _test.selectOptionByText(Locator.name("assignedToGroup"), group);
        }
        else
            _test.checkRadioButton(Locator.radioButtonByNameAndValue("assignedToMethod", "ProjectUsers"));

        _test.clickButton("Update");
    }

    public void goToAdmin()
    {
        _test.clickButton("Admin");
    }

    @LogMethod
    public void addPickListOption(@LoggedParam String field, @LoggedParam String option)
    {
        _test.setFormElement(Locator.css(String.format("form[name=add%s] input[name=keyword]", field.toLowerCase())), option);
        _test.clickButton("Add " + field);

        if (!option.isEmpty())
            _test.assertElementPresent(Locator.css("#form" + field.toLowerCase() + " td").withText(option));
        else
            _test.assertElementPresent(Locator.css(".labkey-error").withText("Enter a value in the text box before clicking any of the \"Add <Keyword>\" buttons"));
    }
}
