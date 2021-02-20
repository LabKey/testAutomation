/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import com.sun.istack.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.react.Pager;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

public class GridBar extends WebDriverComponent<GridBar.ElementCache>
{
    final private WebElement _gridBarElement;
    final private WebElement _containerElement;
    final private WebDriver _driver;
    private final ResponsiveGrid _responsiveGrid;

    protected GridBar(WebDriver driver, WebElement container, ResponsiveGrid responsiveGrid)
    {
        _gridBarElement = Locators.gridBar().findWhenNeeded(container);
        _containerElement = container;
        _responsiveGrid = responsiveGrid;  // The responsive grid that is associated with this bar.
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _gridBarElement;
    }

    public File exportData(ExportType exportType)
    {

        WebElement downloadBtn = Locator.tagWithClass("span", "fa-download").findElement(this);

        if(!downloadBtn.isDisplayed())
            throw new ElementNotVisibleException("File export button is not visible.");

        downloadBtn.click();

        // QueryGridPanel (deprecated)
        Locator.CssLocator queryGridButton = Locator.css("li > a > span").withClass(exportType.buttonCssClass());
        // GridPanel
        Locator.CssLocator gridPanelButton = Locator.css("span.export-menu-icon").withClass(exportType.buttonCssClass());
        WebElement exportButton = Locator.CssLocator.union(queryGridButton, gridPanelButton).findElement(this);
        return getWrapper().doAndWaitForDownload(()->exportButton.click());
    }

    /**
     * gets the Pager for the current grid, if it exists.
     * If the grid is filtered down to an empty set or if there are no loaded rows, it will not be present
     * @return
     */
    public Pager pager()
    {
        return new Pager.PagerFinder(getDriver(), _responsiveGrid).waitFor(this);
    }

    /**
     * says whether or not the grid currently shows a pager (for example, when filtered down to zero
     * or not loaded, the pager will not be present)
     * @return
     */
    public boolean hasPager()
    {
        return new Pager.PagerFinder(getDriver(), _responsiveGrid).findOptional(this).isPresent();
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
            return _responsiveGrid.getRows().size();
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
    public ResponsiveGrid clickNext()
    {
        pager().clickNext();
        return _responsiveGrid;
    }

    /**
     * clicks the 'previous' button on the pager and waits for the grid to update
     * @return
     */
    public ResponsiveGrid clickPrevious()
    {
        pager().clickPrevious();
        return _responsiveGrid;
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
        WebElement btn = selectBtn.waitForElement(_containerElement, 5_000);
        btn.click();

        getWrapper().waitFor(() -> allSelected.findOptionalElement(this).isPresent() ||
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

        if(!clearBtn.findOptionalElement(this).isEmpty())
        {
            WebElement btn = clearBtn.waitForElement(this, 5_000);
            btn.click();

            getWrapper().waitFor(() -> clearBtn.findOptionalElement(this).isEmpty(),
                    WAIT_FOR_JAVASCRIPT);
        }

        return this;
    }

    /**
     * Click a button on the grid bar with the given text.
     * @param buttonCaption Button caption.
     * @param doAction The action to perform after the click. Can be null.
     */
    public void clickButton(String buttonCaption, @Nullable Runnable doAction)
    {
        Locator button = Locator.xpath("//button[contains(text(), '" + buttonCaption + "')]");

        if(!button.findOptionalElement(this).isEmpty())
        {
            WebElement btn = button.waitForElement(this, 5_000);
            getWrapper().scrollIntoView(btn);
            btn.click();

            if(doAction != null)
            {
                doAction.run();
            }

        }

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
                menus.put(buttonText, new MultiMenu.MultiMenuFinder(_driver).withText(buttonText).find(this));

            return menus.get(buttonText);
        }
    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator gridBar()
        {
            // QueryGridModel grid uses query-grid-bar, QueryModel grid uses grid-panel__button-bar
            return Locator.XPathLocator.union(Locator.tagWithClassContaining("div", "query-grid-bar"),
                    Locator.tagWithClassContaining("div", "grid-panel__button-bar"));
        }

        static final Locator.XPathLocator viewSelectorButtonGroup = Locator.tagWithClass("div", "dropdown")
                .withChild(Locator.button("Grid Views"));
        static final Locator.XPathLocator viewSelectorToggleButton = Locator.button("Grid Views");
        static final Locator viewSelectorMenu = Locator.tagWithAttributeContaining("ul", "aria-labelledby", "viewselector");
    }

    public static class GridBarFinder extends WebDriverComponentFinder<GridBar, GridBarFinder>
    {
        private Locator _locator;
        private WebElement _container;
        private ResponsiveGrid _responsiveGrid;

        /**
         * At this time (Feb 2020) a grid bar will not exist without a grid panel, and a responsive grid. Rather
         * than take a responsive grid and search up the html chain for a the correct grid bar, take a container
         * element and search for the grid bar in it.
         *
         * @param driver A reference to a WebDriver
         * @param containerPanel The panel / html element containing the grid bar.
         * @param responsiveGrid The responsive grid associated with this grid bar.
         */
        public GridBarFinder(WebDriver driver, WebElement containerPanel, ResponsiveGrid responsiveGrid)
        {
            super(driver);
            _locator= Locators.gridBar();
            _responsiveGrid = responsiveGrid;
            _container = containerPanel;
        }

        @Override
        protected GridBar construct(WebElement el, WebDriver driver)
        {
            return new GridBar(driver, _container, _responsiveGrid);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

    public enum ExportType
    {
        CSV("fa-file-o"),
        EXCEL("fa-file-excel-o"),
        TSV("fa-file-text-o");

        private final String _cssClass;

        ExportType(String cssClass)
        {
            _cssClass = cssClass;
        }
        public String buttonCssClass()
        {
            return _cssClass;
        }
    }
}
