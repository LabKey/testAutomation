/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.ui.Pager;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * Wrapper for QueryGrid pager and some standard query grid menus
 */
public class GridBar extends WebDriverComponent<GridBar.ElementCache>
{
    private final WebElement _gridBarElement;
    private final QueryGrid _queryGrid;

    protected GridBar(WebElement element, QueryGrid queryGrid)
    {
        _gridBarElement = element;
        _queryGrid = queryGrid;  // The query grid that is associated with this bar.
    }

    @Override
    protected WebDriver getDriver()
    {
        return _queryGrid.getDriver();
    }

    @Override
    public WebElement getComponentElement()
    {
        return _gridBarElement;
    }

    public File exportData(ExportType exportType)
    {
        WebElement exportButton = getExportButton(exportType);
        return getWrapper().doAndWaitForDownload(exportButton::click);
    }

    private WebElement getExportButton(ExportType exportType)
    {
        WebElement downloadBtn = Locator.tagWithClass("span", "fa-download").findElement(this);

        if(!downloadBtn.isDisplayed())
            throw new ElementNotInteractableException("File export button is not visible.");

        downloadBtn.click();

        return Locator.css("span.export-menu-icon").withClass(exportType.buttonCssClass()).findElement(this);
    }

    public TabSelectionExportDialog openExcelTabsModal()
    {
        WebElement exportButton = getExportButton(ExportType.EXCEL);
        exportButton.click();

        return new TabSelectionExportDialog(this.getDriver());
    }

    /**
     * gets the Pager for the current grid, if it exists.
     * If the grid is filtered down to an empty set or if there are no loaded rows, it will not be present
     * @return grid pager
     */
    public Pager pager()
    {
        return new Pager.PagerFinder(getDriver(), _queryGrid).waitFor(this);
    }

    /**
     * says whether or not the grid currently shows a pager (for example, when filtered down to zero
     * or not loaded, the pager will not be present)
     * @return <code>true</code> if grid has a pager
     */
    public boolean hasPager()
    {
        return new Pager.PagerFinder(getDriver(), _queryGrid).findOptional(this).isPresent();
    }

    /**
     * uses the pager to select a page from the pager dropdown list
     * @param page the text of the list item to be clicked
     * @return
     */
    public GridBar jumpToPage(String page) // e.g. "First Page"|"Last Page"
    {
        pager().jumpToPage(page);
        return this;
    }

    /**
     * gets the current page number
     * @return
     */
    public int getCurrentPage()
    {
        return pager().getCurrentPage();
    }

    /**
     * selects the number of rows to be shown per page
     * @param pageSize
     * @return
     */
    public GridBar selectPageSize(String pageSize)
    {
        pager().selectPageSize(pageSize);
        return this;
    }

    public int getPageSize()
    {
        return pager().getPageSize();
    }

    public int getRecordCount()
    {
        try
        {
            return pager().total();
        }
        catch(NoSuchElementException | StaleElementReferenceException nse)
        {
            // If the paging count isn't present return the number of rows in the grid.
            return _queryGrid.getRows().size();
        }
    }

    public boolean isOnFirstPage()
    {
        return !pager().isPreviousEnabled();
    }

    public boolean isOnLastPage()
    {
        return !pager().isNextEnabled();
    }

    /**
     * clicks the 'next' button on the pager associated with this grid and waits for the grid to update
     * @return
     */
    public QueryGrid clickNext()
    {
        pager().clickNext();
        return _queryGrid;
    }

    /**
     * clicks the 'previous' button on the pager and waits for the grid to update
     * @return
     */
    public QueryGrid clickPrevious()
    {
        pager().clickPrevious();
        return _queryGrid;
    }

    /**
     * Click the 'Select All' button in the grid bar.
     *
     * @return This grid bar.
     */
    public GridBar selectAllRows()
    {
        Locator selectBtn = Locator.xpath("//button[contains(text(), 'Select all')]");      // Select all n
        Locator selectedText = Locator.xpath("//span[@class='QueryGrid-right-spacing' and normalize-space(contains(text(), 'selected'))]");   // n of n
        Locator allSelected = Locator.xpath("//span[contains(text(), 'All ')]");            // All n selected
        WebElement btn = selectBtn.waitForElement(_queryGrid, 5_000);
        btn.click();

        WebDriverWrapper.waitFor(() -> allSelected.findOptionalElement(this).isPresent() ||
                        selectBtn.findOptionalElement(this).isEmpty() &&
                                selectedText.findOptionalElement(this).isPresent() ,
                WAIT_FOR_JAVASCRIPT);

        return this;
    }

    /**
     * Click the 'Clear All' button in the grid bar.
     * @return This grid bar.
     */
    public GridBar clearAllSelections()
    {
        // Clear button can have text values of 'Clear', 'Clear both' or 'Clear all ' so just look for clear.
        Locator clearBtn = Locator.xpath("//button[contains(text(), 'Clear')]");

        if(clearBtn.findOptionalElement(this).isPresent())
        {
            WebElement btn = clearBtn.waitForElement(this, 5_000);
            btn.click();

            WebDriverWrapper.waitFor(() -> clearBtn.findOptionalElement(this).isEmpty(),
                    WAIT_FOR_JAVASCRIPT);
        }

        return this;
    }

    /**
     * Click a button on the grid bar with the given text.
     * @param buttonCaption Button caption.
     */
    public void clickButton(String buttonCaption)
    {
        BootstrapLocators.button(buttonCaption).waitForElement(this, 5_000).click();
    }

    public void doMenuAction(String buttonText, List<String> menuActions)
    {
        MultiMenu multiMenu = null;
        boolean found = false;
        int tries = 1;

        // Sometimes the grid and query bar will load, and even the menu button will render but the text will
        // take just a few ms to render, so if at first you don't succeed try again.
        while(!found && tries <= 3)
        {
            try
            {
                multiMenu = elementCache().findMenu(buttonText);
                found = true;
            }
            catch (NoSuchElementException nse)
            {
                getWrapper().log("Couldn't find menu button with caption '" + buttonText + "', trying again.");
                tries++;
                sleep(500);
            }
        }

        if(found)
        {
            multiMenu.doMenuAction(menuActions);
        }
        else
        {
            throw new NoSuchElementException("Couldn't find menu button with caption '" + buttonText + "'.");
        }
    }

    public List<String> getMenuButtonsText()
    {
        // Because this is not a search for a specific menu button let's pause for a moment to give the buttons a
        // chance to render if they haven't done so already.
        sleep(1_500);

        List<MultiMenu> menuButtons = new MultiMenu.MultiMenuFinder(getDriver()).findAll(this);
        List<String> menuButtonText = new ArrayList<>();

        for(MultiMenu multiMenu : menuButtons)
        {
            menuButtonText.add(multiMenu.getButtonText());
        }

        return menuButtonText;
    }

    public List<String> getMenuText(String buttonText)
    {
        MultiMenu multiMenu = null;
        boolean found = false;
        int tries = 1;

        // Sometimes the grid and query bar will load, and even the menu button will render but the text will
        // take just a few ms to render, so if at first you don't succeed try again.
        while(!found && tries <= 3)
        {
            try
            {
                multiMenu = elementCache().findMenu(buttonText);
                found = true;
            }
            catch (NoSuchElementException nse)
            {
                getWrapper().log("Couldn't find menu button with caption '" + buttonText + "', trying again.");
                tries++;
                sleep(500);
            }
        }

        if(found)
            return multiMenu.getMenuText();

        throw new NoSuchElementException("Couldn't find menu button with caption '" + buttonText + "'.");
    }

    /**
     * Private helper function that will get the text of the aliquot view button. This can be used to determine the
     * current view. Asserts that the button is present.
     *
     * @return Text of the aliquot view button.
     */
    private String currentAliquotViewText()
    {
        Assert.assertTrue("There is no 'Aliquot View' button on this grid.",
                elementCache().aliquotView.getComponentElement().isDisplayed());

        return elementCache().aliquotView.getComponentElement().getText();
    }

    /**
     * Get the current view selected in the aliquot view button. This asserts that the button is present.
     *
     * @return A {@link AliquotViewOptions} item.
     */
    public AliquotViewOptions getCurrentAliquotView()
    {
        String text = currentAliquotViewText().toLowerCase();

        if(text.contains("all samples"))
            return AliquotViewOptions.ALL;

        // If the current page is the sources page the text would be 'Derived Samples Only', so this should still work.
        if(text.contains("samples only"))
            return AliquotViewOptions.SAMPLES;

        if(text.contains("aliquots only"))
            return AliquotViewOptions.ALIQUOTS;

        return null;
    }

    /**
     * Set the aliquot view.
     *
     * @param view A {@link AliquotViewOptions} enum value.
     */
    public void setAliquotView(AliquotViewOptions view)
    {
        // Need to identify where we are. The menu text is contextual to the page.
        String url = getDriver().getCurrentUrl().toLowerCase();
        boolean onSourcesPage = url.contains("#/sources/");
        boolean onSamplePage = url.contains("#/samples/");

        String currentButtonText = currentAliquotViewText();
        String menuChoice = "";

        switch (view)
        {
            case ALL:
                if(onSourcesPage)
                {
                    if(url.endsWith("assays"))
                    {
                        menuChoice = "Derived Samples or Aliquots";
                    }
                    else if(url.endsWith("jobs"))
                    {
                        menuChoice = "Samples or Aliquots";
                    }
                    else
                    {
                        // This is the 'Samples' page for a source.
                        menuChoice = "Samples and Aliquots";
                    }
                }
                else if (onSamplePage)
                {
                    menuChoice = "Sample or Aliquots";
                }
                else
                {
                    menuChoice = "Samples and Aliquots";
                }
                break;
            case SAMPLES:
                if(onSourcesPage && url.endsWith("assays"))
                {
                    menuChoice = "Derived Samples Only";
                }
                else if(onSamplePage)
                {
                    menuChoice = "Sample Only";
                }
                else
                {
                    menuChoice = "Samples Only";
                }
                break;
            case ALIQUOTS:
                menuChoice = "Aliquots Only";
                break;
        }

        doMenuAction(currentButtonText, Arrays.asList(menuChoice));

    }

    public GridBar searchFor(String searchStr)
    {

        clearSearch();

        _queryGrid.doAndWaitForUpdate(()->
        {
            elementCache().searchBox.set(searchStr);
            elementCache().searchBox.getComponentElement().sendKeys(Keys.ENTER);
        });
        return this;
    }

    public GridBar clearSearch()
    {
        if(!elementCache().searchBox.get().isEmpty())
        {
            _queryGrid.doAndWaitForUpdate(()->
            {
                elementCache().searchBox.set("");
                elementCache().searchBox.getComponentElement().sendKeys(Keys.ENTER);
            });
        }
        return this;
    }

    public String getSearchExpression()
    {
        return elementCache().searchBox.get();
    }

    public GridFilterModal openFilterDialog()
    {
        clickButton("Filters");
        return new GridFilterModal(getDriver(), _queryGrid);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {

        private final Map<String, MultiMenu> menus = new HashMap<>();
        protected MultiMenu findMenu(String buttonText)
        {
            if (!menus.containsKey(buttonText))
                menus.put(buttonText, new MultiMenu.MultiMenuFinder(getDriver()).withText(buttonText).find(this));

            return menus.get(buttonText);
        }

        protected final BootstrapMenu aliquotView = BootstrapMenu.finder(getDriver()).locatedBy(
                Locator.tagWithAttributeContaining("button", "id", "aliquotviewselector").parent()).findWhenNeeded(this);

        protected final Input searchBox = Input.Input(Locator.tagWithClass("input", "grid-panel__search-input"), getDriver()).findWhenNeeded(this);
    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator gridBar()
        {
            return Locator.tagWithClassContaining("div", "grid-panel__button-bar");
        }
    }

    public static class GridBarFinder extends ComponentFinder<QueryGrid, GridBar, GridBarFinder>
    {
        @Override
        protected GridBar construct(WebElement el)
        {
            return new GridBar(el, getContext());
        }

        @Override
        protected Locator locator()
        {
            return Locators.gridBar();
        }
    }

    public enum ExportType
    {
        CSV("fa-file-o", ','),
        EXCEL("fa-file-excel-o", null),
        TSV("fa-file-text-o", '\t');

        private final String _cssClass;
        private final Character _separator;

        ExportType(String cssClass, Character separator)
        {
            _cssClass = cssClass;
            _separator = separator;
        }
        public String buttonCssClass()
        {
            return _cssClass;
        }

        public Character getSeparator()
        {
            return _separator;
        }
    }

    public enum AliquotViewOptions
    {
        ALL,
        SAMPLES,
        ALIQUOTS
    }
}
