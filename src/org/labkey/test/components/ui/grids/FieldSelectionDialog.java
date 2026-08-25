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

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.util.TextUtils.normalizeSpace;
import static org.labkey.test.util.selenium.WebElementUtils.getTextContent;

/**
 * Wraps ColumnSelectionModal.tsx in UI components.
 */
public class FieldSelectionDialog extends ModalDialog
{
    private final UpdatingComponent linkedComponent;

    private static final String FIELD_NOT_AVAILABLE = "Field name '%s' is not visible in the 'Available Fields' list.";
    private static final String FIELD_NOT_SELECTED = "Field with label '%s' is not visible in the selected fields list.";

    public FieldSelectionDialog(WebDriver driver, UpdatingComponent linkedComponent)
    {
        super(new ModalDialogFinder(driver));
        this.linkedComponent = linkedComponent;
        waitForReady();
    }

    public FieldSelectionDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver));
        waitForReady();
        this.linkedComponent = null;
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> !BootstrapLocators.loadingSpinner.isDisplayed(this) &&
                            elementCache().contentPanelLocator.findElements(this).size() == 2,
                "Customize Grid dialog did not render in time.", 1_500);
    }

    public boolean isShowAllVisible()
    {
        return elementCache().checkbox.isDisplayed();
    }

    /**
     * Check or uncheck the 'Show all system and user-defined fields' checkbox.
     *
     * @param checked Set to true to check the box, false to uncheck it.
     * @return This dialog
     */
    public FieldSelectionDialog setShowAll(boolean checked)
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
     * Get the list of visible fields from the 'Available Fields' panel. Children of an expanded field only have the name
     * in the list, this does not include the path.
     *
     * @return The list of field names.
     */
    public List<String> getAvailableFieldLabels()
    {
        List<WebElement> listItemElements = elementCache().getListItemNameElements(elementCache().availableFieldsPanel);
        return listItemElements.stream().map(WebElementUtils::getTextContent).collect(Collectors.toList());
    }

    /**
     * Check to see if the available field listed is shown as selected, has a checkmark, in the 'Available Fields' panel.
     *
     * @param fieldNameParts Can be an individual field or a path to a nested field.
     * @return True if row has the checkmark, false otherwise.
     */
    public boolean isAvailableFieldSelected(String... fieldNameParts)
    {
        return isAvailableFieldSelected(FieldKey.fromParts(fieldNameParts));
    }

    public boolean isAvailableFieldSelected(FieldKey fieldKey)
    {
        WebElement listItem = getAvailableFieldElement(fieldKey);
        return Locator.tagWithClass("i", "fa-check").findWhenNeeded(listItem).isDisplayed();
    }

    public boolean isFieldAvailable(String... fieldNameParts)
    {
        return isFieldAvailable(FieldKey.fromParts(fieldNameParts));
    }

    public boolean isFieldAvailable(FieldKey fieldKey)
    {
        try
        {
            getAvailableFieldElement(fieldKey);
            return true;
        }
        catch (NoSuchElementException e)
        {
            return false;
        }
    }

    /**
     * Select a field the list of available fields. If more than one value is passed in it is assumed to be an expandable path.
     *
     * @param fieldNameParts Either an individual field or the path to a field to add.
     * @return This dialog.
     */
    public FieldSelectionDialog selectAvailableField(String... fieldNameParts)
    {
        return selectAvailableField(FieldKey.fromParts(fieldNameParts));
    }

    public FieldSelectionDialog selectAvailableField(FieldKey fieldKey)
    {
        WebElement listItem = getAvailableFieldElement(fieldKey);

        Assert.assertTrue(String.format(FIELD_NOT_AVAILABLE, fieldKey),
            listItem.isDisplayed());

        WebElement addIcon = Locator.tagWithClass("button", "view-field__action")
                .withChild(Locator.tagWithClass("i", "fa-plus"))
                .findElement(listItem);

        addIcon.click();

        // avoid Selected-list hover popover
        getWrapper().mouseOver(ModalDialog.Locators.title.findElement(this));

        return this;
    }

    public WebElement getAvailableFieldElement(String... fieldNameParts)
    {
        return getAvailableFieldElement(FieldKey.fromParts(fieldNameParts));
    }

    /**
     * Expand available field tree to the specified field
     *
     * @param fieldKey FieldKey for the target field
     * @return row element for the specified field
     */
    public WebElement getAvailableFieldElement(FieldKey fieldKey)
    {
        Iterator<FieldKey> iterator = fieldKey.getIterator();

        while(iterator.hasNext())
        {
            fieldKey = iterator.next();

            // If this isn't the last item in the collection keep expanding and building the expected data-fieldkey value.
            if(iterator.hasNext())
            {
                // If the field is already expanded don't try to expand it.
                if(!isFieldKeyExpanded(elementCache().findAvailableField(fieldKey)))
                    expandOrCollapseByFieldKey(fieldKey, true);
            }

        }

        return elementCache().findAvailableField(fieldKey);
    }

    /**
     * Private helper function that will expand or collapse a row in the 'Available Fields' panel.
     *
     * @param fieldKey The data-fieldkey value of the field to expand.
     * @param expand True to expand false to collapse.
     */
    private void expandOrCollapseByFieldKey(FieldKey fieldKey, boolean expand)
    {

        WebElement listItem = elementCache().findAvailableField(fieldKey);

        // Check to see if row is already in the desired state. If so don't do anything.
        if((expand && isFieldKeyExpanded(listItem) || (!expand && !isFieldKeyExpanded(listItem))))
            return;

        Assert.assertTrue(String.format("Field with data-fieldkey attribute '%s' is not visible in the 'Available Fields' panel.", fieldKey),
                listItem.isDisplayed());

        String iconClass = expand ? "fa-chevron-right" : "fa-chevron-down";
        String oppositeIconClass = !expand ? "fa-chevron-right" : "fa-chevron-down";

        WebElement expandIcon = Locator.tagWithClass("div", "field-expand-icon")
                .withChild(Locator.tagWithClass("button", iconClass))
                .findElement(listItem);
        Locator oppositeIcon = Locator.tagWithClass("div", "field-expand-icon")
                .withChild(Locator.tagWithClass("button", oppositeIconClass));


        String errorMessage;

        if(expand)
            errorMessage = String.format("There is no expand icon for field with data-fieldkey attribute '%s' in the 'Available Fields' panel.", fieldKey);
        else
            errorMessage = String.format("There is no collapse icon for field with data-fieldkey attribute '%s' in the 'Available Fields' panel.", fieldKey);

        Assert.assertTrue(errorMessage, listItem.isDisplayed());
        getWrapper().doAndWaitForElementToRefresh(expandIcon::click, () -> oppositeIcon.findElement(listItem), new WebDriverWait(getDriver(), Duration.ofSeconds(1)));
    }

    /**
     * Private helper to see if the row has been expanded.
     *
     * @param listItem A web element of the row/field to check.
     * @return True if row is expanded.
     */
    private boolean isFieldKeyExpanded(WebElement listItem)
    {
        // As long as there is no expand icon then this field is expanded.
        return   !Locator.tagWithClass("div", "field-expand-icon")
                .withChild(Locator.tagWithClass("button", "fa-chevron-right"))
                .findWhenNeeded(listItem).isDisplayed();
    }

    /**
     * Get the list of fields that have been selected.
     *
     * @return The list of selected fields.
     */
    public List<String> getSelectedFieldLabels()
    {
        List<WebElement> listItemElements = elementCache().getListItemNameElements(elementCache().selectedFieldsPanel);
        return listItemElements.stream().map(WebElementUtils::getTextContent).collect(Collectors.toList());
    }

    /**
     * Get the field that is highlighted (active) from the fields selected. If no field is highlighted an empty string is returned.
     *
     * @return Text of highlighted (active) selected field. Empty string if none is highlighted.
     */
    public String getActiveSelectedFieldLabel()
    {
        WebElement active = Locator.tagWithClass("div", "list-group-item")
                .withClass("active")
                .findWhenNeeded(this);

        if(active.isDisplayed())
        {
            return getTextContent(Locator.tagWithClass("div", "field-caption").findElement(active));
        }
        else
        {
            return "";
        }
    }

    /**
     * Click on a field in the list of selected fields. Fields added from the Available Fields panel will be added
     * underneath the highlighted field.
     *
     * @param field The field name to click on.
     * @return This dialog.
     */
    public FieldSelectionDialog highlightFieldInSelectedFields(String field)
    {
        return highlightFieldInSelectedFields(field, 0);
    }

    /**
     * Click on a field in the list of selected fields. If multiple fields have the same name the index parameter will
     * identify which one to click on.
     *
     * @param field The field to click on.
     * @param index If multiple fields have the same name this will identify which one to click.
     * @return This dialog.
     */
    public FieldSelectionDialog highlightFieldInSelectedFields(String field, int index)
    {
        getSelectedListItems(field).get(index).click();
        return this;
    }

    /**
     * Check if a field can be removed from the list of selected fields.
     * @param field Field name / label to remove.
     * @return True if the field can be removed, false otherwise.
     */
    public boolean canFieldBeRemoved(String field)
    {
        return canFieldBeRemoved(field, 0);
    }

    /**
     * Check if a field can be removed from the list of selected fields. If there are multiple fields with the same
     * name use the index to identify the field.
     * @param field Field name / label to remove.
     * @param index Index for duplicate fields.
     * @return True if the field can be removed, false otherwise.
     */
    public boolean canFieldBeRemoved(String field, int index)
    {
        WebElement listItem = getSelectedListItems(field).get(index);
        WebElement removeIcon = Locator.tagWithClass("button", "view-field__action").findWhenNeeded(listItem);
        return removeIcon.isDisplayed();
    }

    /**
     * Remove the fields from the list of selected fields.
     *
     * @param fields List of fields to remove.
     * @return This dialog.
     */
    public FieldSelectionDialog removeFieldsFromSelected(List<String> fields)
    {
        for(String field : fields)
        {
            removeFieldFromSelected(field, 0);
        }

        return this;
    }

    /**
     * Remove the given field from the list of selected fields.
     *
     * @param field The field to remove.
     * @return This dialog.
     */
    public FieldSelectionDialog removeFieldFromSelected(String field)
    {
        return removeFieldFromSelected(field, 0);
    }

    /**
     * Remove the given field from the list of selected fields. If multiple fields have the same name the index parameter
     * will identify which one to remove.
     *
     * @param field The field to remove.
     * @param index If multiple fields have the same value this identifies which one to remove.
     * @return This dialog.
     */
    public FieldSelectionDialog removeFieldFromSelected(String field, int index)
    {
        WebElement listItem = getSelectedListItems(field).get(index);
        WebElement removeIcon = Locator.byClass("view-field__action").findElement(listItem);
        getWrapper().mouseOver(removeIcon);
        removeIcon.click();

        // Move the mouse over the dialog title.
        getWrapper().mouseOver(ModalDialog.Locators.title.findElement(this));

        getWrapper().shortWait()
                .withMessage(String.format("Field '%s' was not removed from list.", field))
                .until(ExpectedConditions.stalenessOf(listItem));

        return this;
    }

    /**
     * Remove all the fields from the list of selected fields.
     *
     * @return This dialog.
     */
    public FieldSelectionDialog removeAllSelectedFields()
    {
        List<WebElement> allItems = elementCache().getListItemElements(elementCache().selectedFieldsPanel);
        boolean removedAll = true;

        for (WebElement listItem : allItems)
        {
            getWrapper().log(String.format("Removing field '%s' from selected fields.", listItem.getText()));

            WebElement removeIcon = Locator.tagWithClass("button", "view-field__action").findWhenNeeded(listItem);

            // In some usages there may be fields that are not removable.
            if (!removeIcon.isDisplayed())
            {
                removedAll = false;
                continue;
            }

            getWrapper().mouseOver(removeIcon);
            removeIcon.click();
        }

        // If a non-removable field is encountered, then skip check to see if all fields are removed.
        if (removedAll)
        {
            WebDriverWrapper.sleep(500);
            WebDriverWrapper.waitFor(() -> getSelectedFieldLabels().isEmpty(), "Did not remove all of the selected fields.", 1_500);
        }

        return this;
    }

    /**
     * Update the given field label to a new value.
     *
     * @param fieldName The field to be updated.
     * @param newFieldLabel The new value to set the label to.
     * @return This dialog.
     */
    public FieldSelectionDialog setFieldLabel(String fieldName, String newFieldLabel)
    {
        return setFieldLabel(FieldKey.fromParts(fieldName), newFieldLabel);
    }

    /**
     * Update the given field to a new label.
     *
     * @param fieldKey The field to be updated.
     * @param newFieldLabel The new value to set the label to.
     * @return This dialog.
     */
    public FieldSelectionDialog setFieldLabel(FieldKey fieldKey, String newFieldLabel)
    {
        WebElement listItem = elementCache().findSelectedField(fieldKey);
        WebElement updateIcon = Locator.byClass( "edit-inline-field__toggle").findWhenNeeded(listItem);
        updateIcon.click();

        WebDriverWrapper.waitFor(()->elementCache().fieldLabelEdit.isDisplayed(),
                String.format("Input for field '%s' was not shown.", fieldKey), 1_500);

        // Unfortunately using setFormElement doesn't work in this case. That method calls WebElement.clear which clears
        // the current text but also causes the focus to the input control to be lost. When the focus is lost the input
        // goes away. Need to do update the control using the selenium actions.

        // Select the current text, type in the new value then change focus (tab) to commit the change.
        getWrapper().actionClear(elementCache().fieldLabelEdit);
        Actions replaceCurrentText = new Actions(getDriver());
        replaceCurrentText.sendKeys(newFieldLabel)
                .sendKeys(Keys.TAB)
                .perform();

        getWrapper().mouseOver(elementCache().title); // Dismiss tooltip

        WebDriverWrapper.waitFor(()->!elementCache().fieldLabelEdit.isDisplayed(),
                String.format("New field label '%s' is not in the list.", newFieldLabel), 500);
        Assert.assertEquals("Label after update", normalizeSpace(newFieldLabel), elementCache().getFieldLabel(fieldKey));

        return this;
    }

    /**
     * Click the 'Undo Edits' button.
     *
     * @return This dialog.
     */
    public FieldSelectionDialog clickUndoEdits()
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

    private List<WebElement> getSelectedListItems(String fieldLabel)
    {
        List<WebElement> listItems = elementCache().getListItemElements(elementCache().selectedFieldsPanel, fieldLabel);

        Assert.assertFalse(String.format(FIELD_NOT_SELECTED, fieldLabel),
                listItems.isEmpty());

        return listItems;
    }

    /**
     * Helper function to reposition a field in the selected list.
     *
     * @param fieldToMove The name / label of the field to move.
     * @param targetField The name / label of the field currently occuping the desired position.
     * @param beforeTarget Will the field being moved go before (above) or after (below) the target field.
     * @return This dialog.
     */
    public FieldSelectionDialog repositionField(FieldKey fieldToMove, FieldKey targetField, boolean beforeTarget)
    {
        List<String> order = selectedFieldKeys();
        int from = indexOfFieldKey(order, fieldToMove);
        int target = indexOfFieldKey(order, targetField);
        int to = beforeTarget ? (from < target ? target - 1 : target) : (from < target ? target : target + 1);

        keyboardReorder(elementCache().findDragHandle(fieldToMove), to - from);

        WebDriverWrapper.waitFor(() -> {
            List<String> now = selectedFieldKeys();
            return indexOfFieldKey(now, fieldToMove) - indexOfFieldKey(now, targetField) == (beforeTarget ? -1 : 1);
        }, "Field '" + fieldToMove + "' was not repositioned as expected", 5_000);

        return this;
    }

    /**
     * Reorder a row via the keyboard controls: focus the handle, Space to lift, one Arrow per
     * step, Space to drop. (Mouse drag is unreliable with the library's sensor.)
     *
     * @param dragHandle The row's drag handle.
     * @param steps Positions to move; negative moves up, positive moves down.
     */
    private void keyboardReorder(WebElement dragHandle, int steps)
    {
        getWrapper().scrollIntoView(dragHandle);
        getWrapper().executeScript("arguments[0].focus();", dragHandle);

        Actions drag = new Actions(getDriver()).sendKeys(Keys.SPACE).pause(Duration.ofMillis(400)); // lift
        Keys arrow = steps < 0 ? Keys.ARROW_UP : Keys.ARROW_DOWN;
        for (int i = 0; i < Math.abs(steps); i++)
            drag.sendKeys(arrow).pause(Duration.ofMillis(300));
        drag.sendKeys(Keys.SPACE).perform(); // drop
    }

    /**
     * Get the 'data-fieldkey' values of the selected fields, in display order.
     *
     * @return The encoded field keys.
     */
    private List<String> selectedFieldKeys()
    {
        return elementCache().getListItemElements(elementCache().selectedFieldsPanel).stream()
                .map(el -> el.getDomAttribute("data-fieldkey"))
                .collect(Collectors.toList());
    }

    /**
     * Get the index of a field in the given list, matched case-insensitively (as findFieldRow locates rows).
     *
     * @param fieldKeys Encoded field keys, in display order.
     * @param fieldKey Field to locate.
     * @return The index, or -1 if not present.
     */
    private static int indexOfFieldKey(List<String> fieldKeys, FieldKey fieldKey)
    {
        for (int i = 0; i < fieldKeys.size(); i++)
        {
            if (fieldKey.toString().equalsIgnoreCase(fieldKeys.get(i)))
                return i;
        }
        return -1;
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

    public void clickCancel()
    {
        elementCache().cancelButton.click();
        waitForClose();
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

    /**
     * Update the tool-tip view.
     */
    public void clickUpdateView()
    {
        dismiss("Update");
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

        protected WebElement selectedFieldsPanel = contentPanelLocator.index(1).findWhenNeeded(this);

        // This is present to items in both panels.
        protected final Locator listItemName = Locator.tagWithClass("div", "field-caption");

        protected final WebElement undoEditsButton = Locator.tagWithText("span", "Undo edits")
                .refindWhenNeeded(this);

        // The checkbox has no id or name.
        protected final Checkbox checkbox = new Checkbox(Locator.tagWithAttribute("input", "type", "checkbox")
                .findWhenNeeded(this));

        protected final WebElement updateGridButton = Locator.button("Update Grid")
                .findWhenNeeded(this);

        protected final WebElement cancelButton = Locator.button("Cancel")
                .findWhenNeeded(this);

        // The 'pencil' to edit a field label. Only in the Shown in Grid panel.
        protected final WebElement fieldLabelEdit = Locator.tagWithClass("input", "form-control")
                .refindWhenNeeded(selectedFieldsPanel);

        // Will get all the list items that match the fieldLabel.
        protected List<WebElement> getListItemElements(WebElement panel, String fieldLabel)
        {
            return Locator.tagWithClass("div", "list-group-item")
                    .withDescendant(Locator.tagWithClass("div", "field-caption").withText(fieldLabel))
                    .findElements(panel);
        }

        protected String getFieldLabel(FieldKey fieldKey)
        {
            return Locator.tagWithClass("div", "field-caption")
                .findElement(findFieldRow(fieldKey, selectedFieldsPanel))
                .getText();
        }

        protected WebElement findSelectedField(FieldKey fieldKey)
        {
            return findFieldRow(fieldKey, selectedFieldsPanel);
        }

        protected WebElement findDragHandle(FieldKey fieldKey)
        {
            return Locator.tagWithAttribute("div", "role", "button").findElement(findSelectedField(fieldKey));
        }

        protected WebElement findAvailableField(FieldKey fieldKey)
        {
            return findFieldRow(fieldKey, availableFieldsPanel);
        }

        protected WebElement findFieldRow(FieldKey fieldKey, WebElement panel)
        {
            return Locator.tagWithClass("div", "list-group-item")
                .withAttributeIgnoreCase("data-fieldkey", fieldKey.toString())
                .findElement(panel);
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
