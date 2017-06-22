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
package org.labkey.test.pages.flow.reports;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;

public class PositivityReportEditorPage extends ReportEditorPage<PositivityReportEditorPage>
{
    protected static final String reportType = "Flow.PositivityReport";

    public PositivityReportEditorPage(WebDriver driver)
    {
        super(driver);
    }

    public static PositivityReportEditorPage beginCreate(WebDriverWrapper driver)
    {
        return beginCreate(driver, driver.getCurrentContainerPath());
    }

    public static PositivityReportEditorPage beginCreate(WebDriverWrapper driver, String containerPath)
    {
        ReportEditorPage.beginCreate(driver, containerPath, reportType);
        return new PositivityReportEditorPage(driver.getDriver());
    }

    public static PositivityReportEditorPage beginEdit(WebDriverWrapper driver, String reportId)
    {
        return beginEdit(driver, driver.getCurrentContainerPath(), reportId);
    }

    public static PositivityReportEditorPage beginEdit(WebDriverWrapper driver, String containerPath, String reportId)
    {
        ReportEditorPage.beginEdit(driver, containerPath, reportType, reportId);
        return new PositivityReportEditorPage(driver.getDriver());
    }

    @Override
    protected Locator.XPathLocator getSubsetInput()
    {
        return Locator.tagWithName("input", "subset");
    }
}