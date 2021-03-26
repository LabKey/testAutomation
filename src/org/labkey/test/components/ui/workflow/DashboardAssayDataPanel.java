package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.DropdownButtonGroup;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class DashboardAssayDataPanel extends WebDriverComponent<DashboardAssayDataPanel.ElementCache>
{
    private WebDriver driver;
    final WebElement panelElement;

    public DashboardAssayDataPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        panelElement = element;
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
        try
        {
            if (Locator.tagContainingText("div", "No assays have been created").findElement(panelElement).isDisplayed())
                return true;
        }
        catch(NoSuchElementException nse)
        {
            // Do nothing if this element is not there.
        }

        try
        {
            return elementCache().assayGrid.isLoaded();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    /**
     * this selects the specified assay from the Recent Assay Data section,
     * or alternatively sets it to select "All Assays"
     * @param assaySelection the intended value to select in the dropdown- either "All Assays", or the name of an assay
     * @return the current page
     */
    public DashboardAssayDataPanel selectRecentAssay(String assaySelection)
    {
        WebDriverWrapper.waitFor(()-> assayPickerBtn().isPresent(),
                "the assay select button group was not present in time", WAIT_FOR_JAVASCRIPT);
        String originalValue = assayPickerBtn().get().getButtonText();

        if (!originalValue.equals(assaySelection))
        {
            WebDriverWrapper.waitFor(() -> optionalGrid().isPresent(),
                    "the assay grid was not present in time", WAIT_FOR_JAVASCRIPT);
            ResponsiveGrid grid = optionalGrid().get();
            assayPickerBtn().get().clickSubMenu(assaySelection);

            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(grid.getComponentElement()));
        }
        return this;
    }

    /**
     * this dropdownButtonGroup, located in the 'Recent Assay Data' panel, filters the contents of the accompanying recentAssaysGrid
     * to be either 'All Assays', or a particular one.
     * When a particular assay is selected, the related 'Import Data' button is shown
     *
     * If no assays exist in the project, this picker button will not be shown, so call to isPresent() can be used to test this
     * @return
     */
    public Optional<DropdownButtonGroup> assayPickerBtn()
    {
        return new DropdownButtonGroup.DropdownButtonGroupFinder(getDriver()).withButtonId("recent-assays-dropdown")
                .findOptional(this);
    }

    /**
     * wraps the grid that contains assay information for the selected assay(s)
     * if no assays exist, it won't be present.  You can use this to test that.
     * @return
     */
    public Optional<ResponsiveGrid> optionalGrid()
    {
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).withGridId("recent-assays-undefined")
                .findOptional(this);
    }

    public ResponsiveGrid grid()
    {
        return optionalGrid().get();
    }

    /**
     * this button appears when an assay is selected in the assayPickerBtn buttongroup.  It will not be present if
     * "All Assays" is selected.
     * @return
     */
    public Optional<WebElement> importAssayDataBtn()
    {
        return Locator.tagWithClass("a", "recent-assays_import-btn").findOptionalElement(this);
    }

    /**
     * Selects the desired assay to upload to from the dropdown picker if it is not already selected,
     * awaits the appearance of the Import Data button, then clicks it.
     * This will result in a navigation of some sort
     * @param assayName
     */
    public void goToImportData(String assayName)
    {
        selectRecentAssay(assayName);
        WebDriverWrapper.waitFor(()-> importAssayDataBtn().isPresent(), 2000);
        importAssayDataBtn().get().click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        ResponsiveGrid assayGrid = new ResponsiveGrid.ResponsiveGridFinder(driver).findWhenNeeded(panelElement);
    }

    public static class DashboardAssayDataPanelFinder extends WebDriverComponentFinder<DashboardAssayDataPanel, DashboardAssayDataPanelFinder>
    {

        private Locator _locator;

        public DashboardAssayDataPanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.xpath("//div[contains(@class, 'section-panel--title-medium')][text()='Recent Assay Data']/ancestor::div[@class='panel-body']");
        }

        @Override
        protected DashboardAssayDataPanel construct(WebElement element, WebDriver driver)
        {
            return new DashboardAssayDataPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
