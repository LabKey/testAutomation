package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.util.samplemanagement.SMTestUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WorkflowJobIncludedSamplesPanel extends WebDriverComponent<WorkflowJobIncludedSamplesPanel.ElementCache>
{
    private final WebDriver driver;
    private final WebElement componentElement;

    public WorkflowJobIncludedSamplesPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        componentElement = element;

        getWrapper().waitFor(()-> isPanelLoaded(),
        "The 'Included Samples' panel has not loaded in time.",
                2_000);
    }

    /**
     * Check to see if the grid has data or the 'no data' message. Function could be called in a waitFor function.
     *
     * @return True if grid has data or 'no samples loaded' message, false otherwise.
     */
    public boolean isPanelLoaded()
    {

        boolean isLoaded;

        try
        {
            if(SMTestUtils.isVisible(Locators.noSamplesAdded, this))
            {
                isLoaded = true;
            }
            else if(elementCache().samplesGrid().isLoaded())
            {
                isLoaded = true;
            }
            else
            {
                isLoaded = false;
            }
        }
        catch (NoSuchElementException notThere)
        {
            isLoaded = false;
        }

        return isLoaded;

    }

    /**
     * This is the part of the search panel that has the grid, omni-box, paging controls and add button.
     *
     * @return A QueryGrid object.
     */
    public QueryGrid getSamplesGridPanel()
    {
        if(!SMTestUtils.isVisible(Locators.noSamplesAdded, this))
        {
            return elementCache().samplesGrid();
        }
        else
        {
            return null;
        }
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
        static final Locator noSamplesAdded = Locator.tagWithClassContaining("div", "alert-warning")
                .withText("Use search to add samples of interest, or finish creating this job and add samples later.");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        QueryGrid samplesGrid()
        {
            return new QueryGrid.QueryGridFinder(getDriver()).find(this);
        }

    }

}
