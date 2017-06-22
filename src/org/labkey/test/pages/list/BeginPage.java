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
package org.labkey.test.pages.list;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.list.ManageListsGrid;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class BeginPage extends LabKeyPage<BeginPage.ElementCache>
{
    public BeginPage(WebDriver driver)
    {
        super(driver);
    }

    public static BeginPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static BeginPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("list", containerPath, "begin"));
        return new BeginPage(driver.getDriver());
    }

    public ManageListsGrid getGrid()
    {
        return elementCache().listsGrid;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final ManageListsGrid listsGrid = new ManageListsGrid(getDriver());
    }
}