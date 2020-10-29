package org.labkey.test.pages.assay.elisa;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * To access this page, click 'run details' on a row in an ELISA assay.
 * It has recently been updated to be rendered in react, so expect it to be different from other assay run details views
 */
public class ElisaRunDetailsPage extends LabKeyPage<ElisaRunDetailsPage.ElementCache>
{
    public ElisaRunDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ElisaRunDetailsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ElisaRunDetailsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayRunDetails"));
        return new ElisaRunDetailsPage(webDriverWrapper.getDriver());
    }



    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Locator panelHeadingLoc = Locator.tagWithClass("div", "panel-default")
                .withChild(Locator.tagWithClass("div", "panel-heading"));
        Locator dataPanelWithTitle(String title)
        {
            return panelHeadingLoc.withText(title);
        }

        WebElement dataSelectionsPanel = dataPanelWithTitle("Data Selections").findWhenNeeded(this);
        WebElement plotOptionsPanel = dataPanelWithTitle("Plot Options").findWhenNeeded(this);

        WebElement curveFitPanel = panelHeadingLoc.containing("Curve Fit:").findWhenNeeded(this);
        Checkbox showCurveFitLineBox = new Checkbox(Locator.checkbox().findWhenNeeded(curveFitPanel));
        
        WebElement calibrationCurvePanel = dataPanelWithTitle("Calibration Curve").findWhenNeeded(this);
    }
}
