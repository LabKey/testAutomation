package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.DropdownButtonGroup;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

public class DashboardInsightsPanel extends WebDriverComponent<DashboardInsightsPanel.ElementCache>
{
    private WebDriver driver;
    final WebElement panelElement;

    public DashboardInsightsPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        panelElement = element;
    }

    /**
     * This DropdownButton provides the ability to switch between assay and sample view
     * @return
     */
    public DropdownButtonGroup insightsOptionsBtn()
    {
        return new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver()).withButtonId("sample-set-chart-menu")
                .waitFor(this);
    }

    /**
     * This dropdown button provides the ability to filter results (of assay, or of sample) selected with insightsOptionsBtn
     * options are: "All", "In the Last Year", "In the Last Month", "In the Last Week", and "Today"
     * @return
     */
    public DropdownButtonGroup filterOptionsBtn()
    {
        return new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver()).withButtonId("sample-set-selected-chart-menu")
                .waitFor(this);
    }

    /**
     * Selecting a value on this button-group dropdown toggles what is shown- assays or samples.
     * options are: "Sample Count by Sample Type" and "Assay Run Count by Assay"
     * @param type
     * @return
     */
    public DashboardInsightsPanel selectInsightType(String type)
    {
        insightsOptionsBtn().clickSubMenu(type);
        return this;
    }

    /**
     * the Create Samples button appears only when the selected insight type is "Sample count by Sample Type".
     * If "Assay Run Count by Assay" is selected, this element is not present
     * @return
     */
    public Optional<WebElement> createSamplesBtn()
    {
        return Locator.tagWithText("a", "Create Samples").findOptionalElement(panelElement);
    }

    /**
     * Selects the Sample insights type, if it is not selected
     * Waits for the CreateSamplesButton to be present
     * Clicks it.  This will cause navigation away from the current view.
     */
    public void clickCreateSamples()
    {
        selectInsightType("Sample Count by Sample Type");
        WebDriverWrapper.waitFor(()-> createSamplesBtn().isPresent(), 2000);
        createSamplesBtn().get().click();
    }

    @Override
    public WebElement getComponentElement()
    {
        return panelElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    public boolean isPanelLoaded()
    {

        // There are slightly different error messages based on user roles.
        try
        {
            if (Locator.tagContainingText("div", "No sample types have been created.").findElement(panelElement).isDisplayed())
                return true;
        }
        catch (NoSuchElementException nse)
        {
            // Do nothing if this element is not there.
        }

        try
        {
            if (Locator.tagContainingText("div", "No samples have been created.").findElement(panelElement).isDisplayed())
                return true;
        }
        catch (NoSuchElementException nse)
        {
            // Do nothing if this element is not there.
        }

        try
        {
            return elementCache().svgGraph.isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        WebElement svgGraph = Locator.tag("svg").findWhenNeeded(panelElement);
    }

    public static class DashboardInsightsPanelFinder extends WebDriverComponentFinder<DashboardInsightsPanel, DashboardInsightsPanelFinder>
    {

        private Locator _locator;

        public DashboardInsightsPanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.xpath("//div[contains(@class, 'section-panel--title-medium')][text()='Dashboard Insights']/ancestor::div[@class='panel-body']");
        }

        @Override
        protected DashboardInsightsPanel construct(WebElement element, WebDriver driver)
        {
            return new DashboardInsightsPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
