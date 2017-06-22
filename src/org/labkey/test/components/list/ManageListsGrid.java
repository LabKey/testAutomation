/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.pages.list.ImportListArchivePage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

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

    public LabKeyPage clickCreateList()
    {
        clickHeaderButtonAndWait("Create New List");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage exportSelectedLists()
    {
        clickHeaderButtonAndWait("Export List Archive");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage deleteSelectedLists()
    {
        getWrapper().doAndWaitForPageToLoad(() ->
        {
            clickHeaderButton("Delete");
            getWrapper().acceptAlert();
        });
        return new LabKeyPage(getDriver());
    }
}
