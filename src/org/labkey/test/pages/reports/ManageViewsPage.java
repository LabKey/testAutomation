package org.labkey.test.pages.reports;

import org.labkey.test.Locator;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.components.ext4.Window.Window;

public class ManageViewsPage extends LabKeyPage
{
    public ManageViewsPage(WebDriver driver)
    {
        super(driver);
    }

    public void clickAddReport(String reportType)
    {
        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,reportType);
    }

    @LogMethod
    public void deleteReport(String reportName)
    {
        final Locator report = Locator.xpath("//tr").withClass("x4-grid-row").containing(reportName);

        // select the report and click the delete button
        waitForElement(report, 10000);
        click(report);

        click(Locator.linkWithText("Delete Selected"));

        Window(getDriver()).withTitle("Delete")
                .waitFor()
                .clickButton("OK", true);

        // make sure the report is deleted
        waitFor(() -> !isElementPresent(report),
                "Failed to delete report: " + reportName, WAIT_FOR_JAVASCRIPT);
    }

}
