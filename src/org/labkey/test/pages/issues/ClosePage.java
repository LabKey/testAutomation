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
package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class ClosePage extends BaseUpdatePage<ClosePage.ElementCache>
{
    public ClosePage(WebDriver driver)
    {
        super(driver);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, String issueId)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueId);
    }

    public static ClosePage beginAt(WebDriverWrapper driver, String containerPath, String issueId)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "close", Maps.of("issueId", issueId)));
        return new ClosePage(driver.getDriver());
    }

    @Override
    public ListPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new ListPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends UpdatePage.ElementCache
    {
        protected ElementCache()
        {
            assignedTo = readOnlyItem("Assigned To");
            status = readOnlyItem("Status");
        }
    }
}