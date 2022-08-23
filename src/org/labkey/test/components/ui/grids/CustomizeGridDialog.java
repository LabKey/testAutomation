package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps EntityFieldFilterModal.tsx in UI components.
 */
public class CustomizeGridDialog extends ModalDialog
{
    private final UpdatingComponent linkedComponent;

    private static final String FIELD_NOT_AVAILABLE = "Field name '%s' is not visible in the 'Available Fields' list.";
    private static final String FIELD_NOT_IN_GRID = "Field with label '%s' is not visible in the 'Shown in Grid' list.";

    public CustomizeGridDialog(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialogFinder(driver));
        this.linkedComponent = linkedComponent;
        waitForReady();
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()->
                {
                    return !BootstrapLocators.loadingSpinner.isDisplayed(this) &&
                            elementCache().contentPanelLocator.findElements(this).size() == 2;
                },
                "Customize Grid dialog did not render in time.", 1_500);
    }

    /**
     * Check or uncheck the 'Show all system and user-defined fields' checkbox checked?
     *
     * @param checked Set to true to check the box, false to uncheck it.
     * @return True if checked false otherwise.
     */
    public CustomizeGridDialog setShowAll(boolean checked)
    {
        elementCache().checkbox.set(checked);
        return this;
    }

    /**
     * Is the 'Show all system and user-defined fields' checkbox checked?
     *
     * @return True if checked false otherwise.
     */
    public boolean isShowAllChecked()
    {
        return elementCache().checkbox.isChecked();
    }

    /**
     * Get the list of visible fields from the 'Available Fields' panel. Children of an expanded filed only have the name
     * in the list, this des not include the path.
     *
     * @return The list of field names.
     */
    public List<String> getAvailableFields()
    {
        List<WebElement> listItemElements = elementCache().getListItemNameElements(elementCache().availableFieldsPanel);
        return listItemElements.stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Check to see if the field listed is shown as added, has a checkmark, in the 'Available Fields' panel.
     *
     * @param fieldName Can be an individual field or a path to a nested field.
     * @return True if row has the checkmark, false otherwise.
     */
    public boolean isAvailableFieldAdded(String... fieldName)
    {
        StringBuilder fieldKey = new StringBuilder();

        Iterator<String> iterator = Arrays.stream(fieldName).iterator();

        while(iterator.hasNext())
        {
            fieldKey.append(iterator.next().replace(" ", ""));

            // If this isn't the last item in the collection don't expand it or add a "/" to the expected data-fieldkey value.
            if(iterator.hasNext())
            {
                if(!isFieldKeyExpanded(fieldKey.toString()))
                    expandOrCollapseByFieldKey(fieldKey.toString(), true);

                fieldKey.append("/");
            }

        }

        WebElement listItem = elementCache().getListItemElementByFieldKey(fieldKey.toString());

        return Locator.tagWithClass("i", "fa-check").findWhenNeeded(listItem).isDisplayed();
    }

    /**
     * Add a field to the 'Shown in Grid' list. If more than one value is passed in it is assumed to be an expandable path.
     *
     * @param fieldName Either an individual field or the path to a field to add.
     * @return This dialog.
     */
    public CustomizeGridDialog addAvailableField(String... fieldName)
    {
        StringBuilder fieldKey = new StringBuilder();

        Iterator<String> iterator = Arrays.stream(fieldName).iterator();

        while(iterator.hasNext())
        {
            fieldKey.append(iterator.next().replace(" ", ""));

            // If this isn't the last item in the collection don't expand it or add a "/" to the expected data-fieldkey value.
            if(iterator.hasNext())
            {
                // If the field is already expanded don't try to expand it.
                if(!isFieldKeyExpanded(fieldKey.toString()))
                    expandOrCollapseByFieldKey(fieldKey.toString(), true);

                fieldKey.append("/");
            }

        }

        return addFieldByFieldKeyToGrid(fieldKey.toString());
    }

    /**
     * Private helper to add a field to the 'Shown in Grid' list. Use the data-fieldkey value to identify the item.
     *
     * @param fieldKey The value in the data-fieldkay attribute for the row.
     * @return This dialog.
     */
    private CustomizeGridDialog addFieldByFieldKeyToGrid(String fieldKey)
    {
        WebElement listItem = elementCache().getListItemElementByFieldKey(fieldKey);

        Assert.assertTrue(String.format(FIELD_NOT_AVAILABLE, fieldKey),
                listItem.isDisplayed());

        WebElement addIcon = Locator.tagWithClass("div", "view-field__action")
                .withChild(Locator.tagWithClass("i", "fa-plus"))
                .findElement(listItem);

        addIcon.click();

        return this;
    }

    /**
     * Expand a field or a hierarchy of fields. If a single field is passed in only it will be expanded. If multiple values
     * are passed in it is assumed to be a path and all fields will be expanded to the last field.
     *
     * @param fields The list of fields to expand.
     * @return This dialog.
     */
    public CustomizeGridDialog expandAvailableFields(String... fields)
    {
        StringBuilder fieldKey = new StringBuilder();

        Iterator<String> iterator = Arrays.stream(fields).iterator();

        while(iterator.hasNext())
        {
            fieldKey.append(iterator.next().replace(" ", ""));

            expandOrCollapseByFieldKey(fieldKey.toString(), true);

            // If this isn't the last item in the collection don't add a "/" to the expected data-fieldkey value.
            if(iterator.hasNext())
            {
                fieldKey.append("/");
            }

        }

        return this;
    }

    /**
     * Collapse a top level field in 'Available Fields' panel.
     *
     * @param fieldName Name of the field to collapse.
     * @return This dialog.
     */
    public CustomizeGridDialog collapseAvailableField(String fieldName)
    {
        expandOrCollapseByFieldKey(fieldName.replace(" ", ""), false);
        return this;
    }

    /**
     * Private helper function that will expand or collapse a row in the 'Available Fields' panel.
     *
     * @param fieldKey The data-fieldkey value of the field to expand.
     * @param expand True to expand false to collapse.
     */
    private void expandOrCollapseByFieldKey(String fieldKey, boolean expand)
    {
        WebElement listItem = elementCache().getListItemElementByFieldKey(fieldKey);

        Assert.assertTrue(String.format("Field with data-fieldkey attribute '%s' is not visible in the 'Available Fields' panel.", fieldKey),
                listItem.isDisplayed());

        String iconClass = expand ? "fa-plus-square" : "fa-minus-square";

        WebElement expandIcon = Locator.tagWithClass("div", "field-expand-icon")
                .withChild(Locator.tagWithClass("i", iconClass))
                .findElement(listItem);

        String errorMessage;

        if(expand)
            errorMessage = String.format("There is no expand icon for field with data-fieldkey attribute '%s' in the 'Available Fields' panel.", fieldKey);
        else
            errorMessage = String.format("There is no collapse icon for field with data-fieldkey attribute '%s' in the 'Available Fields' panel.", fieldKey);

        Assert.assertTrue(errorMessage, listItem.isDisplayed());

        expandIcon.click();

    }

    /**
     * Private helper to see if the row with the given data-fieldkey value has a checkmark or not.
     *
     * @param fieldKey The data-fieldkey value of the row/field to check.
     * @return True if row is expanded.
     */
    private boolean isFieldKeyExpanded(String fieldKey)
    {
        WebElement listItem = elementCache().getListItemElementByFieldKey(fieldKey);

        // As long as there is no plus/expand icon then this field is expanded.
        return   !Locator.tagWithClass("div", "field-expand-icon")
                .withChild(Locator.tagWithClass("i", "fa-plus-square"))
                .findWhenNeeded(listItem).isDisplayed();
    }

    /**
     * Get the list of columns in the 'Shown in Grid' panel.
     *
     * @return The list of columns from the 'Shown in Grid' panel.
     */
    public List<String> getShownInGridColumns()
    {
        List<WebElement> listItemElements = elementCache().getListItemNameElements(elementCache().shownInGridPanel);
        return listItemElements.stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Get the column that is selected in the 'Sown in Grid' panel. If no column is selected an empty string is returned.
     *
     * @return Text of selected column. Empty string if none is selected.
     */
    public String getSelectedShownInGridColumn()
    {
        WebElement active = Locator.tagWithClass("div", "list-group-item")
                .withClass("active")
                .findWhenNeeded(this);

        if(active.isDisplayed())
        {
            return Locator.tagWithClass("div", "field-name").findElement(active).getText();
        }
        else
        {
            return "";
        }
    }

    /**
     * Click on a column in the 'Shown in Grid' panel. Fields added from the Available Fields panel will be added
     * underneath the selected column in the Shown in Grid panel.
     *
     * @param column The column name to click on.
     * @return This dialog.
     */
    public CustomizeGridDialog selectShownInGridColumn(String column)
    {
        return selectShownInGridColumn(column, 0);
    }

    /**
     * Click on a column in the 'Shown in Grid' panel. If multiple columns have the same name the index parameter will
     * identify which one to click on.
     *
     * @param column The column to click on.
     * @param index If multiple columns have the same name this will identify which one to click.
     * @return This dialog.
     */
    public CustomizeGridDialog selectShownInGridColumn(String column, int index)
    {
        getShownInGridListItems(column).get(index).click();
        return this;
    }

    /**
     * Remove all the columns with the given name from the grid.
     *
     * @param columns List of columns to remove.
     * @return This dialog.
     */
    public CustomizeGridDialog removeColumns(List<String> columns)
    {
        for(String column : columns)
        {
            removeColumn(column, 0);
        }

        return this;
    }

    /**
     * Remove the given column from the 'Shown in Grid' list.
     *
     * @param column The column to remove.
     * @return This dialog.
     */
    public CustomizeGridDialog removeColumn(String column)
    {
        return removeColumn(column, 0);
    }

    /**
     * Remove the given column from the 'Shown in Grid' list. If multiple columns have the same value the index parameter
     * will identify which one to remove.
     *
     * @param column The column to remove.
     * @param index If multiple columns have the same value this identifies which one to remove.
     * @return This dialog.
     */
    public CustomizeGridDialog removeColumn(String column, int index)
    {
        WebElement listItem = getShownInGridListItems(column).get(index);
        WebElement removeIcon = Locator.tagWithClass("span", "view-field__action").findWhenNeeded(listItem);
        removeIcon.click();

        WebDriverWrapper.waitFor(()->!removeIcon.isDisplayed(),
                String.format("Column '%s' was not removed from list.", column), 500);

        return this;
    }

    /**
     * Remove all the columns in the 'SHown in Grid' panel.
     *
     * @return This dialog.
     */
    public CustomizeGridDialog clearColumns()
    {
        List<WebElement> allItems = elementCache().getListItemElements(elementCache().shownInGridPanel);

        for(WebElement listItem : allItems)
        {
            WebElement removeIcon = Locator.tagWithClass("span", "view-field__action").findWhenNeeded(listItem);
            removeIcon.click();
        }

        WebDriverWrapper.waitFor(()-> getShownInGridColumns().isEmpty(),
                "Did not remove all of the 'Shown in Grid' columns.", 500);

        return this;
    }

    /**
     * Update the given column to a new value.
     *
     * @param currentColumnLabel The column to be updated.
     * @param newColumnLabel The new value to set the column label to.
     * @return This dialog.
     */
    public CustomizeGridDialog setColumnLabel(String currentColumnLabel, String newColumnLabel)
    {
        return setColumnLabel(currentColumnLabel, 0, newColumnLabel);
    }

    /**
     * Update the given column to a new label. If there are multiple columnss with the same label in the list the index
     * parameter identifies which one to update.
     *
     * @param currentColumnLabel The column to be updated.
     * @param index If multiple columns have the save label this identifies which one in the list to update.
     * @param newColumnLabel The new value to set the column label to.
     * @return This dialog.
     */
    public CustomizeGridDialog setColumnLabel(String currentColumnLabel, int index, String newColumnLabel)
    {

        WebElement listItem = getShownInGridListItems(currentColumnLabel).get(index);
        WebElement updateIcon = Locator.tagWithClass("span", "edit-inline-field__toggle").findWhenNeeded(listItem);
        updateIcon.click();

        WebDriverWrapper.waitFor(()->elementCache().columnLabelEdit.isDisplayed(),
                String.format("Input for column '%s' was not shown.", currentColumnLabel), 1_500);

        // Unfortunately using setFormElement doesn't work in this case. That method calls WebElement.clear which clears
        // the current text but also causes the focus to the input control to be lost. When the focus is lost the input
        // goes away. Need to do update the control using the selenium actions.

        // Select the current text, type in the new value then change focus (tab) to commit the change.
        Actions replaceCurrentText = new Actions(getDriver());
        replaceCurrentText.sendKeys(Keys.END)
                .keyDown(Keys.SHIFT)
                .sendKeys(Keys.HOME)
                .keyUp(Keys.SHIFT)
                .sendKeys(newColumnLabel)
                .sendKeys(Keys.TAB)
                .perform();

        WebDriverWrapper.waitFor(()->!elementCache().columnLabelEdit.isDisplayed() &&
                        elementCache().getListItemElement(elementCache().shownInGridPanel, newColumnLabel).isDisplayed(),
                String.format("New column label '%s' is not in the list.", newColumnLabel), 500);

        return this;
    }

    /**
     * Click the 'Undo Edits' button.
     *
     * @return This dialog.
     */
    public CustomizeGridDialog clickUndoEdits()
    {
        elementCache().undoEditsButton.click();
        return this;
    }

    /**
     * Is the 'Undo Edits' button enabled.
     *
     * @return True if enabled false otherwise.
     */
    public boolean isUndoEditsEnabled()
    {
        // It looks like .isEnabled is not accurate if disabled-action-text class attribute is used to disable a tag.

        return !elementCache().undoEditsButton
                .getAttribute("class").toLowerCase()
                .contains("disabled-action-text");
    }

    private List<WebElement> getShownInGridListItems(String columnLabel)
    {
        List<WebElement> listItems = elementCache().getListItemElements(elementCache().shownInGridPanel, columnLabel);

        Assert.assertFalse(String.format(FIELD_NOT_IN_GRID, columnLabel),
                listItems.isEmpty());

        return listItems;
    }

    /**
     * Click the 'Update Grid' button and wait for the grid to update and the dialog to close before returning.
     */
    public void clickUpdateGrid()
    {
        linkedComponent.doAndWaitForUpdate(() -> {
            elementCache().updateGridButton.click();
            waitForClose();
        });
    }

    /**
     * Is the 'Update Grid' button enabled.
     *
     * @return True if enabled false otherwise.
     */
    public boolean isUpdateGridEnabled()
    {
        return elementCache().updateGridButton.isEnabled();
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
        protected final Locator contentPanelLocator = Locator.byClass("field-modal__col-content");

        protected WebElement availableFieldsPanel = contentPanelLocator.index(0).findWhenNeeded(this);

        protected WebElement shownInGridPanel = contentPanelLocator.index(1).findWhenNeeded(this);

        // This is present to items in both panels.
        protected final Locator listItemName = Locator.tagWithClass("div", "field-name");

        protected final WebElement undoEditsButton = Locator.tagWithText("span", "Undo edits")
                .refindWhenNeeded(this);

        // The checkbox has no id or name.
        protected final Checkbox checkbox = new Checkbox(Locator.tagWithAttribute("input", "type", "checkbox")
                .findWhenNeeded(this));

        protected final WebElement updateGridButton = Locator.button("Update Grid")
                .findWhenNeeded(this);

        // The 'pencil' to edit a column label. Only in the Shown in Grid panel.
        protected final WebElement columnLabelEdit = Locator.tagWithClass("input", "form-control")
                .refindWhenNeeded(shownInGridPanel);

        // Will get all the list items that match the fieldName.
        protected List<WebElement> getListItemElements(WebElement panel, String fieldName)
        {
            return Locator.tagWithClass("div", "list-group-item")
                    .withDescendant(Locator.tagWithClass("div", "field-name").withText(fieldName))
                    .findElements(panel);
        }

        // Will get the first list item that matches the fieldName.
        protected WebElement getListItemElement(WebElement panel, String fieldName)
        {
            return Locator.tagWithClass("div", "list-group-item")
                    .withDescendant(Locator.tagWithClass("div", "field-name").withText(fieldName))
                    .findElement(panel);
        }

        // The data-fieldkey attribute is only present in items in the Available Fields panel.
        // Similar value to field-name (no spaces, but casing is the same). For child fields it will contain the parent path.
        protected WebElement getListItemElementByFieldKey(String fieldKey)
        {
            return Locator.tagWithClass("div", "list-group-item")
                    .withAttributeIgnoreCase("data-fieldkey", fieldKey)
                    .findElement(availableFieldsPanel);
        }

        // Get the displayed names/labels of list items in the given panel.
        protected List<WebElement> getListItemNameElements(WebElement panel)
        {
            return listItemName.findElements(panel);
        }

        // Get the list-item web element for the given panel.
        protected List<WebElement> getListItemElements(WebElement panel)
        {
            return Locator.tagWithClass("div", "list-group-item").findElements(panel);
        }
    }

}
