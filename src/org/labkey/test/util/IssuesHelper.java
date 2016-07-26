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
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.pages.issues.AdminPage;
import org.labkey.test.pages.issues.DetailsPage;
import org.labkey.test.pages.issues.InsertPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.internal.WrapsDriver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.labkey.test.WebTestHelper.buildURL;

public class IssuesHelper extends WebDriverWrapper
{
    public static final String ISSUES_SCHEMA = "issues";
    public static final String ISSUE_LIST_DEF_QUERY = "IssueListDef";
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

    public boolean doesIssueListDefExist(String container, String listDefName)
    {
        Connection cn = createDefaultConnection(false);
        SelectRowsCommand selectCmd = new SelectRowsCommand(ISSUES_SCHEMA, ISSUE_LIST_DEF_QUERY);
        selectCmd.setMaxRows(1);
        selectCmd.addFilter("name", listDefName, Filter.Operator.EQUAL);

        try
        {
            SelectRowsResponse selectResp = selectCmd.execute(cn, container);
            return !selectResp.getRows().isEmpty();
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public IssueListDefDataRegion goToIssueListDefinitions(String container)
    {
        beginAt(buildURL("query", container, "executeQuery", Maps.of("schemaName", ISSUES_SCHEMA, "query.queryName", ISSUE_LIST_DEF_QUERY)));
        return IssueListDefDataRegion.fromExecuteQuery(getDriver());
    }

    @LogMethod
    public void createNewIssuesList(String name, AbstractContainerHelper containerHelper)
    {
        containerHelper.enableModule("Issues");
        PortalHelper portalHelper = new PortalHelper(getDriver());

        portalHelper.addWebPart("Issue Definitions");
        IssueListDefDataRegion.fromWebPart(getDriver()).createIssuesListDefinition(name);

        portalHelper.addWebPart("Issues Summary");
        clickAndWait(Locator.linkWithText("Submit"));
        portalHelper.addWebPart("Search");
        assertTextPresent("Open");
    }

    public void deleteIssueLists(String projectName, LabKeySiteWrapper test)
    {
        test.clickProject(projectName);
        if (isElementPresent(PortalHelper.Locators.webPartTitle("Issue Definitions")))
        {
            PortalHelper portalHelper = new PortalHelper(getDriver());
            DataRegionTable table = new DataRegionTable("IssueListDef", getDriver());
            table.checkAll();
            clickButton("Delete");
            clickButton("Delete");
            test.clickProject(projectName);

            portalHelper.removeWebPart("Issue Definitions");
            portalHelper.removeWebPart("Issues Summary");
            portalHelper.removeWebPart("Search");
        }
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

    public DetailsPage addIssue(String title, String assignedTo)
    {
        return addIssue(title, assignedTo, Collections.emptyMap());
    }

    public DetailsPage addIssue(String title, String assignedTo, Map<String, String> extraFields, File... attachments)
    {
        Map<String, String> fields = new TreeMap<>();
        fields.put("title", title);
        fields.put("assignedTo", assignedTo);
        fields.putAll(extraFields);
        return addIssue(fields, attachments);
    }

    @LogMethod
    public DetailsPage addIssue(Map<String, String> issue, File... attachments)
    {
        goToModule("Issues");
        clickButton("New Issue");
        InsertPage insertPage = new InsertPage(getDriver());

        for (Map.Entry<String, String> field : issue.entrySet())
        {
            insertPage.fieldWithName(field.getKey()).set(field.getValue());
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

        return new DetailsPage(getDriver());
    }

    @LogMethod
    public void setIssueAssignmentList(@Nullable @LoggedParam String group)
    {
        new AdminPage(getDriver()).setIssueAssignmentList(group);
    }

    @LogMethod
    public void setIssueAssignmentUser(@Nullable @LoggedParam String user)
    {
        new AdminPage(getDriver()).setIssueAssignmentUser(user);
    }

    public AdminPage goToAdmin()
    {
        clickButton("Admin");
        waitForText("Configure Fields");
        return new AdminPage(getDriver());
    }
}
