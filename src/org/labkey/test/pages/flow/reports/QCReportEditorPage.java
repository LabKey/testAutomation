package org.labkey.test.pages.flow.reports;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;

public class QCReportEditorPage extends ReportEditorPage<QCReportEditorPage>
{
    protected static final String reportType = "Flow.QCControlReport";

    public QCReportEditorPage(WebDriver driver)
    {
        super(driver);
    }

    public static QCReportEditorPage beginCreate(WebDriverWrapper driver)
    {
        return beginCreate(driver, driver.getCurrentContainerPath());
    }

    public static QCReportEditorPage beginCreate(WebDriverWrapper driver, String containerPath)
    {
        ReportEditorPage.beginCreate(driver, containerPath, reportType);
        return new QCReportEditorPage(driver.getDriver());
    }

    public static QCReportEditorPage beginEdit(WebDriverWrapper driver, String reportId)
    {
        return beginEdit(driver, driver.getCurrentContainerPath(), reportId);
    }

    public static QCReportEditorPage beginEdit(WebDriverWrapper driver, String containerPath, String reportId)
    {
        ReportEditorPage.beginEdit(driver, containerPath, reportType, reportId);
        return new QCReportEditorPage(driver.getDriver());
    }

    @Override
    protected Locator.XPathLocator getSubsetInput()
    {
        return Locator.tagWithName("input", "statistic_subset");
    }

    public QCReportEditorPage setStatistic(Stat stat)
    {
        _extHelper.selectComboBoxItem(Locator.input("statistic_stat").parent(), stat.getLabel());
        return this;
    }

    public enum Stat
    {
        Count,
        Freq_Of_Parent
                {
                    @Override
                    public String getLabel()
                    {
                        return "Frequency of Parent";
                    }
                };

        public String getLabel()
        {
            return name();
        }
    }
}
