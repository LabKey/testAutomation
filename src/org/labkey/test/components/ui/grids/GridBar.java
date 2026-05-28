/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.ui.Pager;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return elementCache().exportMenu.exportData(exportType);
    }

    public File exportData(ExportType exportType, int index)
    {
        return elementCache().exportMenu.exportData(exportType, index);
    }

    public TabSelectionExportDialog openExcelTabsModal()
    {
        return elementCache().exportMenu.openExcelTabsModal();
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
     */
    public GridBar jumpToPage(String page) // e.g. "First Page"|"Last Page"
    {
        pager().jumpToPage(page);
        return this;
    }

    /**
     * gets the current page number
     */
    public int getCurrentPage()
    {
        return pager().getCurrentPage();
    }

    /**
     * selects the number of rows to be shown per page
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
     */
    public QueryGrid clickNext()
    {
        pager().clickNext();
        return _queryGrid;
    }

    /**
     * clicks the 'previous' button on the pager and waits for the grid to update
     */
    public QueryGrid clickPrevious()
    {
        pager().clickPrevious();
        return _queryGrid;
    }

    /**
     * Click a button on the grid bar with the given text.
     * @param buttonText Button text.
     */
    public void clickButton(String buttonText)
    {
        var btn = BootstrapLocators.button(buttonText).waitForElement(this, 5_000);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(btn));   // for cases when a disabled button
        btn.click();                                                                    // awaits being enabled, for example by selecting grid items
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
                tries++;
                sleep(500);
            }
        }

        // If the button still wasn't found try the 'More' button.
        if(!found)
        {
            multiMenu = elementCache().findMenu("More");
            Assert.assertTrue(String.format("Could not find a menu button '%s' or 'More', don't know what to click.", buttonText),
                    multiMenu.getComponentElement().isDisplayed());
            getWrapper().log(String.format("Couldn't find menu button '%s', clicking the 'More' menu button.", buttonText));
        }

        if (menuActions.size() == 1)
            multiMenu.doMenuAction(menuActions.get(0));
        else if (menuActions.size() == 2)
            multiMenu.doMenuAction(menuActions.get(0), menuActions.get(1));
        else
            throw new IllegalArgumentException("There should be either 1 or 2 menu actions, but was:" + menuActions);
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
                getWrapper().log("Couldn't find menu button with text '" + buttonText + "', trying again.");
                tries++;
                sleep(500);
            }
        }

        if(found)
            return multiMenu.getMenuText();

        throw new NoSuchElementException("Couldn't find menu button with text '" + buttonText + "'.");
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
        if (elementCache().clearSearchButton.isDisplayed())
        {
            _queryGrid.doAndWaitForUpdate(() ->
            {
                elementCache().clearSearchButton.click();
            });
        }
        else if (!elementCache().searchBox.get().isEmpty())
        {
            // Sometimes the search box has something in it, but the filter isn't set, so the clear button isn't visible
            // This happens when we use beginAt to navigate to the page we're already on.
            elementCache().searchBox.set("");
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
        private final ExportMenu exportMenu = ExportMenu.finder(getDriver()).findWhenNeeded(this);

        private final Map<String, MultiMenu> menus = new HashMap<>();
        protected MultiMenu findMenu(String buttonText)
        {
            if (!menus.containsKey(buttonText))
                menus.put(buttonText, new MultiMenu.MultiMenuFinder(getDriver()).withText(buttonText).find(this));

            return menus.get(buttonText);
        }

        protected final MultiMenu aliquotView = new MultiMenu.MultiMenuFinder(getDriver()).withButtonClass("aliquot-view-selector").findWhenNeeded(this);

        protected final Input searchBox = Input.Input(Locator.tagWithClass("input", "grid-panel__search-input"), getDriver()).findWhenNeeded(this);
        protected final WebElement clearSearchButton = Locator.byClass("fa-remove").findWhenNeeded(this);
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
