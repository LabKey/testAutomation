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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.internal.WrapsDriver;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IssuesHelper extends WebDriverWrapper
{
    protected WrapsDriver _driverWrapper;

    public IssuesHelper(WrapsDriver driverWrapper)
    {
        _driverWrapper = driverWrapper;
    }

    public IssuesHelper(WebDriver driver)
    {
        _driverWrapper = () -> driver;
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _driverWrapper.getWrappedDriver();
    }

    @LogMethod
    public void createNewIssuesList(String name, AbstractContainerHelper containerHelper)
    {
        containerHelper.enableModule("Issues");
        PortalHelper portalHelper = new PortalHelper(getDriver());

        portalHelper.addWebPart("Issue Definitions");
        clickAndWait(Locator.linkWithText("Insert New"));
        setFormElement(Locator.input("quf_Label"), name);
        click(Locator.linkWithText("Submit"));
        clickAndWait(Locator.linkWithText("Yes"));

        portalHelper.addWebPart("Issues Summary");
        clickAndWait(Locator.linkWithText("Submit"));
        portalHelper.addWebPart("Search");
        assertTextPresent("Open");
    }

    public int getHighestIssueId(String containerPath, String issuesQuery)
    {
        Connection connection = createDefaultConnection(true);
        SelectRowsCommand command = new SelectRowsCommand("issues", issuesQuery.toLowerCase().replaceAll(" ", ""));
        command.addSort("IssueId", Sort.Direction.DESCENDING);
        command.setMaxRows(1);
        command.setContainerFilter(ContainerFilter.AllFolders);
        try
        {
            SelectRowsResponse response = command.execute(connection, containerPath);
            if (response.getRows().isEmpty())
                return 0;
            return (Integer) response.getRows().get(0).get("IssueId");
        }
        catch (IOException |CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public void addIssue(Map<String, String> issue, File... attachments)
    {
        goToModule("Issues");
        clickButton("New Issue");

        for (Map.Entry<String, String> field : issue.entrySet())
        {
            String fieldName = field.getKey();
            if (!isElementPresent(Locator.name(fieldName)))
                fieldName = fieldName.toLowerCase();

            if ("select".equals(Locator.name(fieldName).findElement(getDriver()).getTagName()))
                selectOptionByText(Locator.name(field.getKey()), field.getValue());
            else
                setFormElement(Locator.id(fieldName), field.getValue());
        }

        for (int i = 0; i < attachments.length; i++)
        {
            if (i == 0)
                click(Locator.linkWithText("Attach a file"));
            else
                click(Locator.linkWithText("Attach another file"));

            setFormElement(Locator.id(String.format("formFile%02d", i + 1)), attachments[i]);
        }

        clickButton("Save");

        List<String> errors = getTexts(Locators.labkeyError.findElements(getDriver()));

        Assert.assertEquals("Unexpected errors", Collections.<String>emptyList(), errors);
    }

    @LogMethod
    public void setIssueAssignmentList(@Nullable @LoggedParam String group)
    {
        if (group != null)
        {
            Locator.XPathLocator specificGroupSelect = Locator.tagWithClass("select", "assigned-to-group");

            click(Locator.tag("span").withClass("assigned-to-group-specific").withChild(Locator.tag("input")));
            selectOptionByText(specificGroupSelect, group);
        }
        else
            click(Locator.tag("span").withClass("assigned-to-group-project").withChild(Locator.tag("input")));
    }

    @LogMethod
    public void setIssueAssignmentUser(@Nullable @LoggedParam String user)
    {
        if (user != null)
        {
            Locator.XPathLocator specificUserSelect = Locator.tagWithClass("select", "assigned-to-user");

            click(Locator.tag("span").withClass("assigned-to-specific-user").withChild(Locator.tag("input")));
            selectOptionByText(specificUserSelect, user);
        }
        else
            click(Locator.tag("span").withClass("assigned-to-empty").withChild(Locator.tag("input")));
    }

    public void goToAdmin()
    {
        clickButton("Admin");
        waitForText("Configure Fields");
    }
}
