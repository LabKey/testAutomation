/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.pages.issues.AdminPage;
import org.labkey.test.pages.issues.DeleteIssueListPage;
import org.labkey.test.pages.issues.InsertIssueDefPage;
import org.labkey.test.pages.issues.ListPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class IssueListDefDataRegion extends DataRegionTable
{
    public static final String NAME_IN_WEBPART = "IssueListDef";
    public static final String NAME_IN_QUERY = "query";

    public IssueListDefDataRegion(String regionName, WebDriver driver)
    {
        super(regionName, driver);
    }

    public static IssueListDefDataRegion fromWebPart(WebDriver driver)
    {
        return new IssueListDefDataRegion(NAME_IN_WEBPART, driver);
    }

    public static IssueListDefDataRegion fromExecuteQuery(WebDriver driver)
    {
        return new IssueListDefDataRegion(NAME_IN_QUERY, driver);
    }

    public InsertIssueDefPage clickInsert()
    {
        clickInsertNewRow();
        return new InsertIssueDefPage(getDriver(), this);
    }

    public DeleteIssueListPage clickDelete()
    {
        clickHeaderButtonAndWait("Delete");
        return new DeleteIssueListPage(getDriver());
    }

    public AdminPage createIssuesListDefinition(String name)
    {
        return startCreateIssuesListDefinition(name).clickYes();
    }

    public AdminPage createIssuesListDefinition(String name, String kind)
    {
        return startCreateIssuesListDefinition(name, kind).clickYes();
    }

    public ListPage goToIssueList(String name)
    {
        getWrapper().clickAndWait(link(getRowIndex("Label", name), "Label"));
        return new ListPage(getDriver());
    }

    public InsertIssueDefPage.CreateListDefConfirmation startCreateIssuesListDefinition(String name)
    {
        return startCreateIssuesListDefinition(name, "General Issue Tracker");
    }

    public InsertIssueDefPage.CreateListDefConfirmation startCreateIssuesListDefinition(String name, String kind)
    {
        InsertIssueDefPage insertIssueDefPage = clickInsert();
        insertIssueDefPage.setLabel(name);
        insertIssueDefPage.selectKind(kind);
        return insertIssueDefPage.clickSubmit();
    }

    public void selectIssueDefs(String... names)
    {
        for (String name : names)
        {
            checkCheckbox(getRowIndex("Label", name));
        }
    }

    public IssueListDefDataRegion deleteListDefs(String... names)
    {
        selectIssueDefs(names);
        return clickDelete().confirmDelete();
    }
}
