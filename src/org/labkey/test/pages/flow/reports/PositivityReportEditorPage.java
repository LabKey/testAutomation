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