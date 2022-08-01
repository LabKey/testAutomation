/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.bootstrap.Panel;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.ui.FilterStatusValue;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * The 'grid' element that contains two components.
 * <p>The first component is a grid header bar which contains the search input, the paging
 * controls, 'Select All' controls, filter status display, etc...
 * </p>
 * <p>The second component is the responsive grid which is the grid data.</p>
 */
public class QueryGrid extends ResponsiveGrid<QueryGrid>
{
    final private WebDriver _driver;
    final private WebElement _queryGridPanel;

    private QueryGrid(WebElement element, WebDriver driver)
    {
        super(element, driver);
        _queryGridPanel = element;
        _driver = driver;
    }

    protected QueryGrid(QueryGrid wrappedGrid)
    {
        this(wrappedGrid.getComponentElement(), wrappedGrid.getDriver());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _queryGridPanel;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    // get rowMaps

    /**
     * Returns the first row with a column text equivalent to the supplied text
     * @param text column text to search for
     * @return row data
     */
    public Map<String, String> getRowMap(String text)
    {
        return getRow(text).getRowMap();
    }

    /**
     * Returns the first row with the supplied text in the specified column
     * @param columnLabel    The text in the column header cell
     * @param text text in the data cell
     * @return row data
     */
    public Map<String, String> getRowMap(String columnLabel, String text)
    {
        GridRow row = getRow(columnLabel, text);
        return row.getRowMap();
    }

    /**
     * returns the first row with matching text in the specified column(s)
     * @param partialMap Map where keys are columnText, values are full text
     * @return row data
     */
    public Map<String, String> getRowMap(Map<String, String> partialMap)
    {
        return getRow(partialMap).getRowMap();
    }

    /**
     * returns the first row with a descendant matching the supplied locator
     * @param containing Locator for an element in the row
     * @return row data
     */
    public Map<String, String> getRowMap(Locator.XPathLocator containing)
    {
        return getRow(containing).getRowMap();
    }

    // row selection

    /**
     * Selects or un-selects the first row with the specified text in the specified column
     * @param columnLabel The exact text of the column header
     * @param text The full text of the cell to match
     * @param checked   whether or not to check the box
     * @return this grid
     */
    @Override
    public QueryGrid selectRow(String columnLabel, String text, boolean checked)
    {
        getRow(columnLabel, text).select(checked);
        return this;
    }

    // subcomponent getters

    public GridBar getGridBar()
    {
        return elementCache().gridBar;
    }

    public List<FilterStatusValue> getFilterStatusValues()
    {
        return getFilterStatusValues(false);
    }

    public List<FilterStatusValue> getFilterStatusValues(boolean includeView)
    {
        if (includeView)
            return elementCache().getAllFilterStatusValues();
        else
            return elementCache().getFilterStatusFilterValues();
    }

    // record count

    public int getRecordCount()
    {
        return getGridBar().getRecordCount();
    }

    public QueryGrid waitForRecordCount(int expectedCount)
    {
        return waitForRecordCount(expectedCount, WAIT_FOR_JAVASCRIPT);
    }

    public QueryGrid waitForRecordCount(int expectedCount, int milliseconds)
    {
        WebDriverWrapper.waitFor(()-> getRecordCount() == expectedCount,
                "did not get to the expected record count ["+expectedCount+"] in time",  milliseconds);
        return this;
    }

    @Override
    public void doAndWaitForUpdate(Runnable func)
    {
        Optional<WebElement> optionalStatus = elementCache().selectionStatusContainerLoc.findOptionalElement(elementCache());

        func.run();

        optionalStatus.ifPresent(el -> getWrapper().shortWait().until(ExpectedConditions.stalenessOf(el)));

        waitForLoaded();
        clearElementCache();
    }


    // search, sort and filter methods

    /**
     * searches the grid, from the GridBar search input and waits for the grid to refresh
     * if the searchTerm is different from the current searchTerm
     */
    public QueryGrid search(String searchTerm)
    {
        String currentSearchExp = getGridBar().getSearchExpression();
        if (!searchTerm.equals(currentSearchExp))
            doAndWaitForUpdate(()-> getGridBar().searchFor(searchTerm));

        return this;
    }

    /**
     * removes any search filter applied to the grid by removing the search term, which will update the grid
     */
    public QueryGrid clearSearch()
    {
        String currentSearchExp = getGridBar().getSearchExpression();
        if (!currentSearchExp.isBlank())
            doAndWaitForUpdate(()-> getGridBar().clearSearch());

        return this;
    }

    /**
     * clears grid filter expressions added by the user (i.e. from the filter modal)
     */
    public QueryGrid clearFilters()
    {
        List<FilterStatusValue> valueItems = elementCache().getFilterStatusFilterValues();
        for (int i = valueItems.size() - 1 ; i >= 0; i--) // dismiss from the right first;
        {
            FilterStatusValue obValue = valueItems.get(i);
            doAndWaitForUpdate(obValue::remove);
        }

        Assert.assertEquals("not all of the filter values were cleared", 0, elementCache().getFilterStatusFilterValues().size());
        return this;
    }

    public boolean hasRemoveAllButton()
    {
        return elementCache().removeAllFilters.isDisplayed();
    }

    public QueryGrid clickRemoveAllButton()
    {
        doAndWaitForUpdate(()->elementCache().removeAllFilters.click());
        return this;
    }

    public boolean hasSelectAllButton()
    {
        return elementCache().selectAllBtnLoc.findWhenNeeded(this).isDisplayed();
    }

    /**
     *  Selects all rows in the target domain, including those on other pages, if there are any
     */
    public QueryGrid selectAllRows()
    {
        if (isGridPanel())
        {
            WebElement selectAllBtn = elementCache().selectAllBtnLoc.findWhenNeeded(elementCache());
            if (selectAllBtn.isDisplayed())
            {
                doAndWaitForUpdate(selectAllBtn::click);
            }
            else
            {
                ReactCheckBox selectAll = selectAllBox();
                if (selectAll.isIndeterminate() || !selectAll.isChecked())
                {
                    doAndWaitForUpdate(() -> selectAllOnPage(true, null));
                }
            }
        }
        else
        {
            doAndWaitForUpdate(() ->
                    getGridBar().selectAllRows());
        }

        return this;
    }

    public boolean hasItemsSelected()
    {
        return Locator.tagWithClass("span", "selection-status__count").existsIn(elementCache());
    }

    public String getSelectionStatusCount()
    {   // note: this element is only present when some number of rows in the set are selected
        WebElement selectionStatus = Locator.tagWithClass("span", "selection-status__count")
                .waitForElement(this, 4000);
        return selectionStatus.getText();
    }

    public QueryGrid clearAllSelections()
    {
        if(hasItemsSelected())
        {
            if (isGridPanel())
            {
                WebElement clearBtn = elementCache().clearBtnLoc.findWhenNeeded(elementCache());
                if (clearBtn.isDisplayed())
                {
                    doAndWaitForUpdate(clearBtn::click);
                }
                else
                {
                    doAndWaitForUpdate(() -> selectAllOnPage(false));
                }
            }
            else
            {
                doAndWaitForUpdate(() ->
                        getGridBar().clearAllSelections());
            }
        }

        return this;
    }


    // select view
    public QueryGrid selectView(String viewName)
    {
        doAndWaitForUpdate(() -> elementCache().viewMenu.clickSubMenu(false, viewName));
        return this;
    }

    /**
     * Get the Edit Status text shown in the header. If no status is shown an empty string is returned.
     *
     * @return The edit status text.
     */
    public String getEditAlertText()
    {
        WebElement editAlert = Locator.tagWithClass("span", "view-edit-alert").findWhenNeeded(elementCache().panelHeader());

        if(editAlert.isDisplayed())
            return editAlert.getText();
        else
            return "";

    }

    /**
     * Click the Undo button in the header. Will wait for the grid to update.
     *
     * @return This grid.
     */
    public QueryGrid clickUndo()
    {
        WebElement undoButton = Locator.buttonContainingText("Undo").findElement(elementCache().panelHeader());

        // Wait for the grid to update.
        doAndWaitForUpdate(undoButton::click);

        return this;
    }

    /**
     * Click the Save button. Will show a {@link SaveViewDialog}.
     *
     * @return A {@link SaveViewDialog}
     */
    public SaveViewDialog clickSave()
    {
        WebElement saveButton = Locator.buttonContainingText("Save").findElement(elementCache().panelHeader());
        saveButton.click();

        return new SaveViewDialog(getDriver());
    }

    /**
     * possible this is either a GridPanel, or a QueryGridPanel (QGP is to be deprecated).
     * use this to test which one so we can fork behavior until QGP is gone
     */
    private boolean isGridPanel()
    {
        return elementCache().selectionStatusContainerLoc.existsIn(elementCache());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ResponsiveGrid<QueryGrid>.ElementCache
    {
        final GridBar gridBar = new GridBar.GridBarFinder().findWhenNeeded(QueryGrid.this);

        final BootstrapMenu viewMenu = new MultiMenu.MultiMenuFinder(getDriver()).withText("Views").findWhenNeeded(this);

        final Locator.XPathLocator selectionStatusContainerLoc = Locator.tagWithClass("div", "selection-status");
        final Locator selectAllBtnLoc = selectionStatusContainerLoc.append(Locator.tagWithClass("span", "selection-status__select-all")
                .child(Locator.buttonContainingText("Select all")));
        final Locator clearBtnLoc = selectionStatusContainerLoc.append(Locator.tagWithClass("span", "selection-status__clear-all")
                .child(Locator.tagContainingText("button", "Clear")));

        final WebElement filterStatusPanel = Locator.css("div.grid-panel__filter-status").findWhenNeeded(this);

        public List<FilterStatusValue> getAllFilterStatusValues()
        {
            return new FilterStatusValue.FilterStatusValueFinder(getDriver()).findAll(filterStatusPanel);
        }

        public List<FilterStatusValue> getFilterStatusFilterValues()
        {
            return new FilterStatusValue.FilterStatusValueFinder(getDriver()).findAll(filterStatusPanel)
                    .stream().filter(FilterStatusValue::isFilter).toList();
        }

        final WebElement removeAllFilters = Locator.tagWithClass("a", "remove-all-filters").refindWhenNeeded(this);

        // The panel header element which will contain the Save and Undo buttons.
        public WebElement panelHeader()
        {
            return Locator.xpath("preceding-sibling::div[contains(@class,'panel-heading')]").findElement(this);
        }

    }

    public static class QueryGridFinder extends WebDriverComponentFinder<QueryGrid, QueryGridFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "grid-panel__body")
                .withDescendant(ResponsiveGrid.Locators.responsiveGrid());
        private Locator _locator;

        /**
         * Find the first div with a class of grid-panel and assume the grid panel is in there.
         *
         * @param driver Reference to a WebDriver.
         */
        public QueryGridFinder(WebDriver driver)
        {
            super(driver);
            _locator = _baseLocator;
        }

        public QueryGridFinder inPanelWithHeaderText(String panelHeading)
        {
            _locator = new Panel.PanelFinder(getDriver()).withTitle(panelHeading).buildLocator()
                    .append(_baseLocator);
            return this;
        }

        @Override
        protected QueryGrid construct(WebElement el, WebDriver driver)
        {
            return new QueryGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

    /**
     * Dialog that will allow a user to save a grid view.
     */
    public static class SaveViewDialog extends ModalDialog
    {
        private final Input viewNameInput = Input.Input(Locator.name("gridViewName"), getDriver())
                .findWhenNeeded(this);

        Checkbox checkbox = new Checkbox(Locator.input("setDefaultView").findWhenNeeded(this));

        public SaveViewDialog(WebDriver driver)
        {
            super(new ModalDialogFinder(driver).withTitle("Save Grid View"));
        }

        /**
         * Set the view name.
         *
         * @param viewName View name.
         * @return This dialog.
         */
        public SaveViewDialog setViewName(String viewName)
        {
            viewNameInput.set(viewName);
            return this;
        }

        /**
         * Get the value of the View Name field.
         *
         * @return Value of View Name field.
         */
        public String getViewName()
        {
            return viewNameInput.get();
        }

        /**
         * Check if the View Name field is enabled. It should be disabled if the 'Make default for all' checkbox is checked.
         *
         * @return True if enabled, false otherwise.
         */
        public boolean isViewNameEnabled()
        {
            return viewNameInput.getComponentElement().isEnabled();
        }

        /**
         * Check or uncheck the 'Make default view' checkbox.
         *
         * @param checked True to check, false to uncheck.
         * @return This dialog.
         */
        public SaveViewDialog setMakeDefaultForAll(boolean checked)
        {
            checkbox.set(checked);
            return this;
        }

        /**
         * Get the checked status of the 'Make default for all' checkbox.
         *
         * @return True if it is checked, false otherwise.
         */
        public boolean isMakeDefaultForAllChecked()
        {
            return checkbox.isChecked();
        }

        /**
         * Save the view.
         */
        public void saveView()
        {
            dismiss("Save", 1);
        }

        /**
         * Click the 'Save' button but expect an error.
         *
         * @return The text of the error banner.
         */
        public String saveViewExpectingError()
        {
            dismiss("Save", 0);
            WebElement errorEl = BootstrapLocators.errorBanner.waitForElement(this, 5000);
            return errorEl.getText();
        }

    }

}
