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
package org.labkey.test.components.flow;

import org.labkey.test.Locator;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.flow.reports.PositivityReportEditorPage;
import org.labkey.test.pages.flow.reports.QCReportEditorPage;
import org.labkey.test.pages.flow.reports.ReportEditorPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.labkey.test.Locator.NBSP;

public class FlowReportsWebpart extends BodyWebPart<FlowReportsWebpart.ElementCache>
{
    public FlowReportsWebpart(WebDriver driver)
    {
        super(driver, "Flow Reports");
    }

    public QCReportEditorPage createQCReport()
    {
        getWrapper().clickAndWait(elementCache().createQCReport);
        return new QCReportEditorPage(getDriver());
    }

    public PositivityReportEditorPage createPositivityReport()
    {
        getWrapper().clickAndWait(elementCache().createPositivityReport);
        return new PositivityReportEditorPage(getDriver());
    }

    private <Page extends ReportEditorPage> Page getEditPage(Class<Page> clazz)
    {
        try
        {
            Constructor<Page> constructor = clazz.getConstructor(WebDriver.class);
            return constructor.newInstance(getDriver());
        }
        catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public <Page extends ReportEditorPage> Page editReport(String reportName, Class<Page> clazz)
    {
        getWrapper()._ext4Helper.clickExt4MenuButton(true, elementCache().findManageMenu(reportName), false, "Edit");
        return getEditPage(clazz);
    }

    public LabKeyPage copyReport(String reportName)
    {
        getWrapper()._ext4Helper.clickExt4MenuButton(true, elementCache().findManageMenu(reportName), false, "Copy");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage deleteReport(String reportName)
    {
        getWrapper()._ext4Helper.clickExt4MenuButton(true, elementCache().findManageMenu(reportName), false, "Delete");
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage executeReport(String reportName)
    {
        getWrapper()._ext4Helper.clickExt4MenuButton(true, elementCache().findManageMenu(reportName), false, "Execute");
        return new LabKeyPage(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebPart.ElementCache
    {
        WebElement createQCReport = Locator.lkButton("create qc report").findWhenNeeded(this);
        WebElement createPositivityReport = Locator.lkButton("create positivity report").findWhenNeeded(this);
        WebElement findManageMenu(String reportName)
        {
            return Locator.linkWithText(reportName).parent().followingSibling("td").
                    childTag("a").withClass( "labkey-menu-text-link").withText("manage" + NBSP).parent().findElement(this);
        }
    }
}
