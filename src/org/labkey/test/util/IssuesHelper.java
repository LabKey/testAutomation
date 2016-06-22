/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IssuesHelper
{
    protected WebDriverWrapper _test;

    public IssuesHelper(WebDriverWrapper test)
    {
        _test = test;
    }

    @LogMethod
    public void createNewIssuesList(String name, AbstractContainerHelper containerHelper)
    {
        containerHelper.enableModule("Issues");
        PortalHelper portalHelper = new PortalHelper(_test);

        portalHelper.addWebPart("Issue Definitions");
        _test.clickAndWait(Locator.linkWithText("Insert New"));
        _test.setFormElement(Locator.input("quf_Label"), name);
        _test.click(Locator.linkWithText("Submit"));
        _test.clickAndWait(Locator.linkWithText("Yes"));

        portalHelper.addWebPart("Issues Summary");
        _test.clickAndWait(Locator.linkWithText("Submit"));
        portalHelper.addWebPart("Search");
        _test.assertTextPresent("Open");
    }

    @LogMethod
    public void addIssue(Map<String, String> issue, File... attachments)
    {
        _test.goToModule("Issues");
        _test.clickButton("New Issue");

        for (Map.Entry<String, String> field : issue.entrySet())
        {
            String fieldName = field.getKey();
            if (!_test.isElementPresent(Locator.name(fieldName)))
                fieldName = fieldName.toLowerCase();

            if ("select".equals(Locator.name(fieldName).findElement(_test.getDriver()).getTagName()))
                _test.selectOptionByText(Locator.name(field.getKey()), field.getValue());
            else
                _test.setFormElement(Locator.id(fieldName), field.getValue());
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

        List<String> errors = _test.getTexts(Locators.labkeyError.findElements(_test.getDriver()));

        Assert.assertEquals("Unexpected errors", Collections.<String>emptyList(), errors);
    }

    @LogMethod
    public void setIssueAssignmentList(@Nullable @LoggedParam String group)
    {
        if (group != null)
        {
            Locator.XPathLocator specificGroupSelect = Locator.tagWithClass("select", "assigned-to-group");

            _test.click(Locator.tag("span").withClass("assigned-to-group-specific").withChild(Locator.tag("input")));
            _test.selectOptionByText(specificGroupSelect, group);
        }
        else
            _test.click(Locator.tag("span").withClass("assigned-to-group-project").withChild(Locator.tag("input")));
    }

    @LogMethod
    public void setIssueAssignmentUser(@Nullable @LoggedParam String user)
    {
        if (user != null)
        {
            Locator.XPathLocator specificUserSelect = Locator.tagWithClass("select", "assigned-to-user");

            _test.click(Locator.tag("span").withClass("assigned-to-specific-user").withChild(Locator.tag("input")));
            _test.selectOptionByText(specificUserSelect, user);
        }
        else
            _test.click(Locator.tag("span").withClass("assigned-to-empty").withChild(Locator.tag("input")));
    }

    public void goToAdmin()
    {
        _test.clickButton("Admin");
        _test.waitForText("Configure Fields");
    }
}
