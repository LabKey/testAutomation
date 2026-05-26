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
package org.labkey.test.pages.study.specimen;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

import static org.labkey.test.components.html.SelectWrapper.Select;

public class TypeSummaryReportPage extends LabKeyPage<TypeSummaryReportPage.ElementCache>
{
    public TypeSummaryReportPage(WebDriver driver)
    {
        super(driver);
    }

    public static TypeSummaryReportPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static TypeSummaryReportPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("specimen", containerPath, "typeSummaryReport"));
        return new TypeSummaryReportPage(webDriverWrapper.getDriver());
    }

    public String getTypeBreakdown()
    {
        return elementCache().typeLevelSelect.getFirstSelectedOption().getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final Select cohortFilterSelect = Select(Locator.name("cohortId")).findWhenNeeded(this);
        private final Select availabilityFilterSelect = Select(Locator.name("statusFilterName")).findWhenNeeded(this);
        private final Select baseViewSelect = Select(Locator.name("baseCustomViewName")).findWhenNeeded(this);
        private final Select typeLevelSelect = Select(Locator.name("typeLevel")).findWhenNeeded(this);
    }
}
