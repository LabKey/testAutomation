/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.components.issues.IssueListDefDataRegion;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class DeleteIssueListPage extends LabKeyPage
{
    public DeleteIssueListPage(WebDriver driver)
    {
        super(driver);
    }

    public IssueListDefDataRegion confirmDelete()
    {
        clickAndWait(Locator.lkButton("Delete"));
        return IssueListDefDataRegion.fromExecuteQuery(getDriver());
    }

    public IssueListDefDataRegion cancelDelete()
    {
        clickAndWait(Locator.lkButton("Cancel"));
        return IssueListDefDataRegion.fromExecuteQuery(getDriver());
    }
}