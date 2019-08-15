/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class DetailsListPage extends LabKeyPage
{
    public DetailsListPage(WebDriver driver)
    {
        super(driver);
    }

    public static DetailsListPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static DetailsListPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "detailsList", Maps.of("issueDefName", issueDefName.toLowerCase())));
        return new DetailsListPage(driver.getDriver());
    }
}
