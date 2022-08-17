/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.Panel;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.ui.FilterStatusValue;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return elementCache().getFilterStatusFilterValues();
    }

    /**
     * Get the text of the filter(s).
     *
     * @return List of text filters.
     */
    public List<String> getFilterStatusValuesText()
    {
        return getFilterStatusValues()
                .stream().map(FilterStatusValue::getText)
                .collect(Collectors.toList());
    }

    /**
     * Get a map of the filters. Use the filter text as the key, the filter element will be the value.
     * It is possible, but rare, to have two or more filters with the exact same text. For example columns labels could
     * be changed to be identical (and the same filter is applied). If that happens any filters after the first one will
     * have a sequential number attached to their key value.
     *
     * @return Map of filters. Filter text is key.
     */
    public Map<String, FilterStatusValue> getMapOfFilterStatusValues()
    {
        Map<String, FilterStatusValue> filterMap = new HashMap<>();
        Map<String, Integer> filterCount = new HashMap<>();

        List<FilterStatusValue> filters = getFilterStatusValues();

        for(FilterStatusValue fsv : filters)
        {
            String filterText = fsv.getText();

            // Check if this filter text has already been saved. If it has, avoid a duplicate key error by adding a
            // number to key value for the next entry.
            if(filterCount.containsKey(filterText))
            {
                int count = filterCount.get(filterText) + 1;
                filterText = String.format("%s_%d", filterText, count);
                filterCount.replace(filterText, count);
            }
            else
            {
                filterCount.put(filterText, 1);
            }

            filterMap.put(filterText, fsv);
        }

        return filterMap;
    }

    /**
     * Find a filter by the text displayed and remove it. Will wait for the grid to refresh before returning.
     *
     * @param filterText The text of the filter to remove.
     * @return This grid.
     */
    public QueryGrid removeFilter(String filterText)
    {
        doAndWaitForUpdate(()->getMapOfFilterStatusValues().get(filterText).remove());
        return this;
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


    /**
     * Select a view from the 'Views' menu.
     *
     * @param viewName Name of the view to select.
     * @return This grid.
     */
    public QueryGrid selectView(String viewName)
    {
        String currentView = getViewName();

        // Clicking the view already shown will not cause a refresh of the grid.
        if(!currentView.equalsIgnoreCase(viewName))
            doAndWaitForUpdate(() -> elementCache().viewMenu.clickSubMenu(false, viewName));

        return this;
    }

    /**
     * Customize the view. Use the 'Customize Grid VIew' menu option.
     *
     * @return A {@link CustomizeGridDialog}.
     */
    public CustomizeGridDialog customizeView()
    {
        elementCache().viewMenu.clickSubMenu(false, "Customize Grid View");
        return new CustomizeGridDialog(getDriver(), this);
    }

    /**
     * Save the grid view. Use the 'Save Grid View' menu option which will always show the save dialog.
     *
     * @return A {@link SaveGridViewDialog}.
     */
    public SaveGridViewDialog saveView()
    {
        elementCache().viewMenu.clickSubMenu(false, "Save Grid View");
        return new SaveGridViewDialog(getDriver(), this);
    }

    /**
     * Open a {@link ManageGridViewsDialog}. Use the 'Manage Saved Views' menu option.
     *
     * @return A {@link ManageGridViewsDialog}.
     */
    public ManageGridViewsDialog manageViews()
    {
        elementCache().viewMenu.clickSubMenu(false, "Manage Saved Views");
        return new ManageGridViewsDialog(getDriver(), this);
    }

    /**
     * If there are no saved views and the default view has not been changed the Manage Saved Views menu item will not be enabled.
     *
     * @return True if 'Manage Saved Views' menu item is enabled, false otherwise.
     */
    public boolean isManageViewsEnabled()
    {
        List<WebElement> menuItems = elementCache().viewMenu.findVisibleMenuItemsWithClass("disabled");
        for(WebElement menuItem : menuItems)
        {
            // Why does menuItem.getText() return an empty string here?
            if(menuItem.getAttribute("text").contains("Manage Saved Views"))
                return false;
        }

        return true;
    }

    /**
     * Get the Edit Status text shown in the header. If no status is shown an empty string is returned.
     *
     * @return The edit status text.
     */
    public String getEditAlertText()
    {

        // Not sure how reliable this will be. It is possible that the automation can cause two statuses to show up, the
        // 'EDITED' status and the 'SAVED' status can be there at the same time. May need to add a check in the save button
        // and save menu code.
        // If two alerts are show, this will grab the first which may disappear after it's display status is checked but
        // before the text is gathered.
        // The 'SAVED' status will fade away slowly (by design). It is possible, but less likely, that it will disappear after
        // the call to isDisplayed but before the getText.
        WebElement editAlert = Locator.tagWithClass("span", "view-edit-alert").findWhenNeeded(elementCache().panelHeader());

        if(editAlert.isDisplayed())
            return editAlert.getText();
        else
            return "";

    }

    /**
     * Get the name of the current view. The name is in the panel header. If there is no panel header then the view is
     * the default view and an empty string is returned.
     *
     * @return The name of the grid view from the header. Empty string if no header (default view).
     */
    public String getViewName()
    {
        String viewName;

        // Unfortunately the view name in the header is not in a separate element. Getting the text from the panel header
        // will result in the edit alert text and button text being included.
        // There is a possible issue if the alert text is 'UPDATED'. This text will fade out and may return a -1 when
        // calling headerText.indexOf(alertText). Trying to minimize that by first getting the alert text then the header text.
        //
        // If this proves to be unreliable it might be easier/safer to return the panel header text and let the test do
        // a .contains() on it to see if it has the expected header.
        if(elementCache().panelHeader().isDisplayed())
        {
            String alertText = getEditAlertText();
            String headerText = elementCache().panelHeader().getText();
            headerText = headerText.contains(alertText) ? headerText.substring(alertText.length()) : headerText;
            String buttonText = "Undo\nSave";
            headerText = headerText.contains(buttonText) ? headerText.substring(0, headerText.indexOf(buttonText)) : headerText;
            String appButtonText = "UndoSave";
            viewName = headerText.contains(appButtonText) ? headerText.substring(0, headerText.indexOf(appButtonText)) : headerText;
        }
        else
        {
            viewName = "";
        }

        return viewName.trim();
    }

    /**
     * Click the Undo button in the header. Will wait for the grid to update.
     *
     * @return This grid.
     */
    public QueryGrid clickUndoButton()
    {
        WebElement undoButton = Locator.buttonContainingText("Undo").findElement(elementCache().panelHeader());

        // Wait for the grid to update.
        doAndWaitForUpdate(undoButton::click);

        return this;
    }

    /**
     * Is the 'Undo' button visible on the grid.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isUndoButtonVisible()
    {
        return Locator.buttonContainingText("Undo").findWhenNeeded(elementCache().panelHeader()).isDisplayed();
    }

    /**
     * Click the Save (View) button in the grid header. If the grid is showing the default view clicking save will show
     * a {@link SaveGridViewDialog}, otherwise clicking save will save the changes to the view currently applied to the grid.
     *
     * @param expectSaveDialog Indicate if a 'Save View' dialog is expected. Should only happen if you are saving the default view.
     * @return A {@link SaveGridViewDialog} or null if a saved dialog is not expected.
     */
    public SaveGridViewDialog clickSaveButton(boolean expectSaveDialog)
    {
        WebElement saveButton = Locator.buttonContainingText("Save").findElement(elementCache().panelHeader());
        saveButton.click();

        if(expectSaveDialog)
        {
            return new SaveGridViewDialog(getDriver(), this);
        }
        else
        {
            return null;
        }
    }

    /**
     * Click the 'Save as...' drop down button menu option to change the view name. The 'Save as...' option will not be
     * visible if the default view is being changed.
     *
     * @return A {@link SaveGridViewDialog}
     */
    public SaveGridViewDialog clickSaveAsButton()
    {
        BootstrapMenu bootstrapMenu = new BootstrapMenu(getDriver(), elementCache().panelHeader());
        bootstrapMenu.clickSubMenu(false, "Save as...");

        return new SaveGridViewDialog(getDriver(), this);
    }

    /**
     * Is the 'Save' button visible on the grid.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isSaveButtonVisible()
    {
        return Locator.buttonContainingText("Save").findWhenNeeded(elementCache().panelHeader()).isDisplayed();
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

        public List<FilterStatusValue> getFilterStatusFilterValues()
        {
            return new FilterStatusValue.FilterStatusValueFinder(getDriver()).findAll(filterStatusPanel);
        }

        final WebElement removeAllFilters = Locator.tagWithClass("a", "remove-all-filters").refindWhenNeeded(this);

        // The panel header element which will contain the edit status text, the view name and the Save & Undo buttons.
        // If this is the default view this will not be present.
        public WebElement panelHeader()
        {
            return Locator.xpath("preceding-sibling::div[contains(@class,'panel-heading')]").findWhenNeeded(this);
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

}
