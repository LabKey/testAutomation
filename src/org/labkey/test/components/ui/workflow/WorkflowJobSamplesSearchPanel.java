package org.labkey.test.components.ui.workflow;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class WorkflowJobSamplesSearchPanel extends WebDriverComponent<WorkflowJobSamplesSearchPanel.ElementCache>
{

    private final WebDriver driver;
    private final WebElement componentElement;

    public WorkflowJobSamplesSearchPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        componentElement = element;
    }

    public boolean isSearchFilterExpanded()
    {
        // if the toggle contains an i.fa-chevron-right, it is collapsed.  If down, it is expanded
        return Locator.tagWithClass("i", "fa-shevron-down").existsIn(elementCache().searchFilterToggle());
    }

    public String getToggleText()
    {
        return elementCache().searchFilterToggle().getText();
    }

    public WorkflowJobSamplesSearchPanel showSearchFilters()
    {
        if(!isSearchFilterExpanded())
        {
            elementCache().searchFilterToggle().click();
        }

        return this;
    }

    public WorkflowJobSamplesSearchPanel hideSearchFilters()
    {
        if(isSearchFilterExpanded())
        {
            elementCache().searchFilterToggle().click();
        }

        return this;
    }
    public WorkflowJobSamplesSearchPanel setQuerySearch(String searchString)
    {
        getWrapper().setFormElement(elementCache().querySearchBox(), searchString);
        return this;
    }

    public String getQuerySearch()
    {
        return getWrapper().getFormElement(elementCache().querySearchBox());
    }

    public WorkflowJobSamplesSearchPanel setOfSampleType(String sampleType)
    {
        showSearchFilters();
        elementCache().ofSampleType().select(sampleType);

        return this;
    }

    public String getOfSampleType()
    {
        showSearchFilters();
        return elementCache().ofSampleType().getSelections().get(0);
    }

    public WorkflowJobSamplesSearchPanel clearOfSampleType()
    {
        showSearchFilters();
        elementCache().ofSampleType().clearSelection();
        return this;
    }

    public WorkflowJobSamplesSearchPanel setCreatedBy(String createdBy)
    {
        showSearchFilters();
        elementCache().createdBy().select(createdBy);
        return this;
    }

    public String getCreatedBy()
    {
        showSearchFilters();
        return elementCache().createdBy().getSelections().get(0);
    }

    public WorkflowJobSamplesSearchPanel clearCreatedBy()
    {
        showSearchFilters();
        elementCache().createdBy().clearSelection();
        return this;
    }

    public WorkflowJobSamplesSearchPanel setFromParent(String parent)
    {
        showSearchFilters();
        elementCache().fromParent().select(parent);
        return this;
    }

    public String getFromParent()
    {
        showSearchFilters();
        return elementCache().fromParent().getSelections().get(0);
    }

    public WorkflowJobSamplesSearchPanel clearFromParent()
    {
        showSearchFilters();
        elementCache().fromParent().clearSelection();
        return this;
    }

    public WorkflowJobSamplesSearchPanel setFromSource(String source)
    {
        showSearchFilters();
        elementCache().fromSource().select(source);
        return this;
    }

    public String getFromSource()
    {
        showSearchFilters();
        return elementCache().fromSource().getSelections().get(0);
    }

    public List<String> getReactSelectList(ReactSelect reactSelect)
    {
        showSearchFilters();
        return reactSelect.getOptions();
    }

    public List<String> getFromParentList()
    {
        showSearchFilters();
        return getReactSelectList(elementCache().fromParent());
    }

    public List<String> getFromSourceList()
    {
        showSearchFilters();
        return getReactSelectList(elementCache().fromSource());
    }

    public WorkflowJobSamplesSearchPanel setDateFrom(String dateFrom)
    {
        showSearchFilters();
        getWrapper().setFormElement(elementCache().dateFrom(), dateFrom);
        return this;
    }

    public String getDateFrom()
    {
        showSearchFilters();
        return getWrapper().getFormElement(elementCache().dateFrom());
    }

    public WorkflowJobSamplesSearchPanel setDateTo(String dateFrom)
    {
        showSearchFilters();
        getWrapper().setFormElement(elementCache().dateTo(), dateFrom);
        return this;
    }

    public String getDateTo()
    {
        showSearchFilters();
        return getWrapper().getFormElement(elementCache().dateTo());
    }

    /**
     * This is the part of the search panel that has the grid, omni-box, paging controls and add button.
     *
     * @return A QueryGrid object.
     */
    public QueryGrid getSamplesGridPanel()
    {
        if(!Locators.noSampleFoundYet.existsIn(this))
        {
            return elementCache().samplesGrid();
        }
        else
        {
            return null;
        }
    }

    public WorkflowJobSamplesSearchPanel clickSearchButton()
    {
        showSearchFilters();
        elementCache().searchButton().click();

        // Wait for the grid. Even if the search did not return any samples it should cause an empty grid to render.
        getWrapper().waitFor(()->
        {
            try
            {
                return elementCache().samplesGrid().isLoaded();
            }
            catch (NoSuchElementException nse)
            {
                return false;
            }

        }, "The 'Search for Samples' grid did not load.",1_000);

        return this;
    }

    // TODO Should add a clickSearchButtonExpectingError however I'm not really sure how the error will be communicated.
    //  If it is on the page that may be a little harder to get at, if it is in the panel that should be easier.
    //  Other than DB access errors I'm not really sure how to cause an error to happen.

    public String getPanelInfoBanner()
    {
        return waitForBanner(BootstrapLocators.infoBanner, 750);
    }

    public String getPanelSuccessBanner()
    {
        return waitForBanner(BootstrapLocators.successBanner, 1_000);
    }

    public String getPanelWarningBanner()
    {
        return waitForBanner(BootstrapLocators.warningBanner, 750);
    }

    public String getPanelErrorBanner()
    {
        return waitForBanner(BootstrapLocators.errorBanner, 1_000);
    }

    public WebElement anyBanner()
    {
        return Locator.findAnyElementOrNull(this,
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
    }

    private String waitForBanner(Locator bannerLocator, int waitTime)
    {
        try
        {
            getWrapper().waitFor(() -> bannerLocator.existsIn(this), waitTime);
            return  bannerLocator.findElement(this).getText();
        }
        catch(TimeoutException | NoSuchElementException nse)
        {
            return "";
        }
    }

    /**
     * Dismiss a banner message regardless of it's type.
     * If there are multiple banner messages this will delete the first one.
     */
    public void dismissBannerMessage()
    {
        if(anyBanner() != null)
        {
            WebElement alert = Locator.tagWithClass("div" , "alert").findElement(this);
            Locator.xpath("//i[contains(@class, 'fa-times-circle')]").findElement(alert).click();
        }
    }

    public boolean isClearAllVisible()
    {
        try
        {
            return elementCache().clearAll().isDisplayed();
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
    }

    public WorkflowJobSamplesSearchPanel clickClearAll()
    {
        elementCache().clearAll().click();
        return this;
    }

    @Override
    public WebElement getComponentElement()
    {
        return componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    private static class Locators
    {
        static final Locator noSampleFoundYet = Locator.tagWithClassContaining("div", "alert-warning")
                .withText("No samples found yet. Use search to find samples of interest.");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        private WebElement reactSelectOnPanel(String label)
        {
            // xpath example: //div[contains(text(), 'From Source')]/following-sibling::div//div[contains(@class,'Select--single')]

            WebElement containingDiv = Locator
                    .tagContainingText("div", label)
                    .followingSibling("div")
                    .findElement(this);

            return Locator
                    .tagWithClassContaining("div", "Select--single")
                    .findElement(containingDiv);
        }

        WebElement searchFilterToggle()
        {
            return Locator.tagWithClass("div", "workflow-show-hide-filter-toggle").findElement(this);
        }

        WebElement searchButton()
        {
            return Locator.button("Search").findElement(this);
        }

        ReactSelect ofSampleType()
        {
            return new ReactSelect(reactSelectOnPanel("Of Sample Type"), getDriver());
        }

        ReactSelect createdBy()
        {
            return new ReactSelect(reactSelectOnPanel("Created By"), getDriver());
        }

        ReactSelect fromParent()
        {
            return new ReactSelect(reactSelectOnPanel("From Parent"), getDriver());
        }

        ReactSelect fromSource()
        {
            return new ReactSelect(reactSelectOnPanel("From Source"), getDriver());
        }

        WebElement dateFrom()
        {
            return Locator.tagWithName("input","startDate").findElement(this);
        }

        WebElement dateTo()
        {
            return Locator.tagWithName("input","endDate").findElement(this);
        }

        QueryGrid samplesGrid()
        {
            return new QueryGrid.QueryGridFinder(getDriver()).find(this);
        }

        WebElement panelAlert()
        {
            return Locator.tagWithClassContaining("div", "alert").findElement(this);
        }

        WebElement clearAll()
        {
            return Locator.tagWithClass("span", "filter-clear-all").findElement(this);
        }

        WebElement querySearchBox()
        {
            return Locator.tagWithClassContaining("input", "workflow-sample-search-input").findElement(this);
        }
    }

}
