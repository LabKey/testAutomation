/*
 * Copyright (c) 2022-2026 LabKey Corporation
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

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Dialog used to save a grid view in an app.
 */
public class SaveViewDialog extends ModalDialog
{
    private static final int FILTER_SECTION = 0;
    private static final int SORT_SECTION = 1;

    QueryGrid grid;

    public SaveViewDialog(WebDriver driver, QueryGrid grid)
    {
        super(new ModalDialogFinder(driver).withTitle("Save Grid View"));
        this.grid = grid;
    }

    /**
     * Set the view name.
     *
     * @param viewName View name.
     * @return This dialog.
     */
    public SaveViewDialog setViewName(String viewName)
    {
        elementCache().viewNameInput.set(viewName);
        return this;
    }

    /**
     * Get the value of the View Name field.
     *
     * @return Value of View Name field.
     */
    public String getViewName()
    {
        return elementCache().viewNameInput.get();
    }

    /**
     * Check if the View Name field is visible. It should not be displayed if the 'Make default for all' radio option is chosen.
     * @return True if displayed, false otherwise
     */
    public boolean isViewNameInputDisplayed()
    {
        return elementCache().viewNameInput.getComponentElement().isDisplayed();
    }

    /**
     * Check if the View Name field is enabled. It should be disabled if the 'Make default for all' checkbox is checked.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isViewNameEnabled()
    {
        return elementCache().viewNameInput.getComponentElement().isEnabled();
    }

    /**
     * Check the 'Make default view' radio button.
     *
     * @return This dialog.
     */
    public SaveViewDialog setMakeDefault()
    {
        elementCache().makeDefault.check();
        return this;
    }

    /**
     * Check the 'Make custom view' radio button.
     *
     * @return This dialog.
     */
    public SaveViewDialog setMakeCustom()
    {
        elementCache().makeCustom.check();
        return this;
    }

    /**
     * Get the checked status of the 'Make default for all' checkbox.
     *
     * @return True if it is checked, false otherwise.
     */
    public boolean isMakeDefaultChecked()
    {
        return elementCache().makeDefault.isChecked();
    }

    /**
     * Is the 'Make default for all'' checkbox visible.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isMakeDefaultVisible()
    {
        return elementCache().makeDefault.isDisplayed();
    }

    /**
     * Check or uncheck the 'Make this grid available in child folders' checkbox.
     *
     * @param checked True to check, false to uncheck.
     * @return This dialog.
     */
    public SaveViewDialog setMakeAvailable(boolean checked)
    {
        elementCache().makeAvailable.set(checked);
        return this;
    }

    /**
     * Get the checked status of the 'Make this grid available in child folders' checkbox.
     *
     * @return True if it is checked, false otherwise.
     */
    public boolean isMakeAvailableChecked()
    {
        return elementCache().makeAvailable.isChecked();
    }

    /**
     * Is the 'Make this grid available in child folders' checkbox visible.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isMakeAvailableVisible()
    {
        return elementCache().makeAvailable.isDisplayed();
    }


    /**
     * Check or uncheck the 'Make this grid available in child folders' checkbox.
     *
     * @param checked True to check, false to uncheck.
     * @return This dialog.
     */
    public SaveViewDialog setMakeShared(boolean checked)
    {
        elementCache().makeShared.set(checked);
        return this;
    }

    /**
     * Get the checked status of the 'Make this grid available in child folders' checkbox.
     *
     * @return True if it is checked, false otherwise.
     */
    public boolean isMakeSharedChecked()
    {
        return elementCache().makeShared.isChecked();
    }

    /**
     * Is the 'Make this grid available in child folders' checkbox visible.
     *
     * @return True if visible, false otherwise.
     */
    public boolean isMakeSharedVisible()
    {
        return elementCache().makeShared.isDisplayed();
    }

    /**
     * Get the text of the filter pills listed in the dialog.
     *
     * @return Filter pill text, empty if the dialog shows the 'no filters' message instead.
     */
    public List<String> getFilterValues()
    {
        return getSectionValues(FILTER_SECTION);
    }

    /**
     * Get the message shown in place of the filter pills when the view will be saved with no filters.
     *
     * @return The message, or an empty string if filter pills are shown.
     */
    public String getNoFiltersMessage()
    {
        return getSectionMessage(FILTER_SECTION);
    }

    /**
     * Get the text of the sort pills listed in the dialog. A sort pill shows only the column label; use
     * {@link #getSortDirection(String)} for the direction.
     *
     * @return Sort pill text, empty if the dialog shows the 'no sort' message instead.
     */
    public List<String> getSortValues()
    {
        return getSectionValues(SORT_SECTION);
    }

    /**
     * Get the message shown in place of the sort pills when the view will be saved with no sorts.
     *
     * @return The message, or an empty string if sort pills are shown.
     */
    public String getNoSortsMessage()
    {
        return getSectionMessage(SORT_SECTION);
    }

    /**
     * Get the direction of a sort pill. The pill conveys direction only through its icon.
     *
     * @param sortValue Text of the sort pill, as returned by {@link #getSortValues()}.
     * @return The direction the column will be sorted.
     */
    public SortDirection getSortDirection(String sortValue)
    {
        WebElement pill = Locator.tagWithClass("div", "filter-status-value").withText(sortValue)
                .waitForElement(getSection(SORT_SECTION), 5_000);
        String iconClass = Locator.tagWithClass("i", "symbol").findElement(pill).getAttribute("class");
        return iconClass.contains("fa-sort-amount-desc") ? SortDirection.DESC : SortDirection.ASC;
    }

    private List<String> getSectionValues(int section)
    {
        return Locator.tagWithClass("div", "filter-status-value")
                .findElements(getSection(section))
                .stream().map(WebElement::getText)
                .toList();
    }

    private String getSectionMessage(int section)
    {
        WebElement message = Locator.tagWithClass("div", "save-view-modal__no-action-values")
                .findElementOrNull(getSection(section));
        return message == null ? "" : message.getText();
    }

    private WebElement getSection(int section)
    {
        return Locator.tagWithClass("div", "save-view-modal__action-values").findElements(this).get(section);
    }

    /**
     * Save the view.
     */
    public void saveView()
    {
        grid.doAndWaitForUpdate(()-> dismiss("Save", 1));
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

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {

        protected final Input viewNameInput = Input.Input(Locator.name("gridViewName"), getDriver())
                .findWhenNeeded(this);

        protected RadioButton makeDefault = new RadioButton(Locator.id("defaultView").findWhenNeeded(this));
        protected RadioButton makeCustom = new RadioButton(Locator.id("customView").findWhenNeeded(this));
        protected Checkbox makeAvailable = new Checkbox(Locator.input("setInherit").findWhenNeeded(this));
        protected Checkbox makeShared = new Checkbox(Locator.input("setShared").findWhenNeeded(this));

    }

}
