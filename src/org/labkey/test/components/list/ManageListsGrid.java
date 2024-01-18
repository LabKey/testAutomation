/*
 * Copyright (c) 2017-2018 LabKey Corporation
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
package org.labkey.test.components.list;

import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.list.BeginPage;
import org.labkey.test.pages.list.ConfirmDeletePage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.pages.list.ImportListArchivePage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.List;

public class ManageListsGrid extends DataRegionTable
{
    public ManageListsGrid(WebDriver driver)
    {
        super("query", driver);
    }

    public ImportListArchivePage clickImportArchive()
    {
        clickHeaderButtonAndWait("Import List Archive");
        return new ImportListArchivePage(getDriver());
    }

    public EditListDefinitionPage clickCreateList()
    {
        clickHeaderButtonAndWait("Create New List");
        return new EditListDefinitionPage(getDriver());
    }

    public File exportSelectedLists()
    {
        return getWrapper().doAndWaitForDownload(() ->
            clickHeaderButton("Export List Archive"));
    }

    public BeginPage deleteSelectedLists()
    {
        clickHeaderButtonAndWait("Delete");
        ConfirmDeletePage confirmPage = new ConfirmDeletePage(getDriver());
        return confirmPage.confirmDelete();
    }

    public List<String> getListNames()
    {
        return getColumnDataAsText("Name");
    }

    public DataRegionTable viewListData(String listName)
    {
        getWrapper().clickAndWait(link(getRowIndex("Name", listName), getColumnIndex("Name")));
        return new DataRegionTable("query", getDriver());
    }

    public EditListDefinitionPage viewListDesign(String listName)
    {
        getWrapper().clickAndWait(link(getRowIndex("Name", listName), 0));
        return new EditListDefinitionPage(getDriver());
    }

    public LabKeyPage viewListHistory(String listName)
    {
        getWrapper().clickAndWait(link(getRowIndex("Name", listName), 1));
        return null;
    }
}
