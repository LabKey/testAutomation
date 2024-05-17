/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import java.util.HashMap;
import java.util.Map;

public class ShowAuditLogPage extends LabKeyPage<ShowAuditLogPage.ElementCache>
{
    public ShowAuditLogPage(WebDriver driver)
    {
        super(driver);
    }

    public static ShowAuditLogPage beginAt(WebDriverWrapper driver, String eventType)
    {
        return beginAt(driver, eventType, null);
    }

    public static ShowAuditLogPage beginAt(WebDriverWrapper driver, String eventType, Integer rowIdCutoff)
    {
        Map<String, Object> params = new HashMap<>();
        params.put("view", eventType);
        if (rowIdCutoff != null)
        {
            params.put("query.RowId~gt", rowIdCutoff);
        }
        driver.beginAt(WebTestHelper.buildURL("audit", "showAuditLog", params));
        return new ShowAuditLogPage(driver.getDriver());
    }

    public ShowAuditLogPage selectView(String viewName)
    {
        if (!viewName.equals(elementCache().viewSelect.getFirstSelectedOption().getText()))
        {
            doAndWaitForPageToLoad(() -> {
                elementCache().viewSelect.selectByVisibleText(viewName);
            });
            clearCache();
        }
        return this;
    }

    public DataRegionTable getLogTable()
    {
        return new DataRegionTable("query", getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Select viewSelect = SelectWrapper.Select(Locator.tagWithName("select", "view")).timeout(4000)
                .findWhenNeeded(this);
    }
}
