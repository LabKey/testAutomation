/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.components.study.specimen;

import org.labkey.test.pages.study.specimen.ShowCreateSpecimenRequestPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class SpecimenDetailGrid extends DataRegionTable
{
    public SpecimenDetailGrid(WebDriver driver)
    {
        super("SpecimenDetail", driver);
    }

    public void viewExistingRequests()
    {
        clickHeaderMenu("Request Options", "View Existing Requests");
    }

    public ShowCreateSpecimenRequestPage createNewRequest()
    {
        clickHeaderMenu("Request Options", "Create New Request");
        return new ShowCreateSpecimenRequestPage(getDriver());
    }

    public void addToExistingRequest()
    {
        clickHeaderMenu("Request Options", "Add To Existing Request");
    }
}
