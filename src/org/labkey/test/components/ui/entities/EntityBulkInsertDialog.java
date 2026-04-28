package org.labkey.test.components.ui.entities;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldKey;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * `fieldIdentifier` arguments accept field names or {@link FieldKey}s
 */
public class EntityBulkInsertDialog extends EntityBulkDialog
{
    public EntityBulkInsertDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle("Bulk Add"));
    }

    protected EntityBulkInsertDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    /**
     * Option at the top of the dialog to make the samples derive from the identified parents.
     *
     * @return A reference to this dialog.
     */
    public EntityBulkInsertDialog selectDerivativesOption()
    {
        elementCache().derivativesOption.check();
        return this;
    }

    /**
     * Option at the top of the dialog to make the samples pool from the identified parents.
     *
     * @return A reference to this dialog.
     */
    public EntityBulkInsertDialog selectPooledOption()
    {
        elementCache().poolOption.check();
        return this;
    }

    /**
     * Option at the top of the dialog to make the samples aliquoted from the identified parent.
     *
     * @return A reference to this dialog.
     */
    public EntityBulkInsertDialog selectAliquotOption()
    {
        elementCache().aliquotOption.check();
        return this;
    }

    /**
     * Check to see if the creation type options are displayed.
     *
     * @return True if either option is visible, false otherwise.
     */
    public boolean creationTypeOptionsVisible()
    {
        // Unlikely one option would be visible without the other.
        return elementCache().poolOption.isDisplayed() || elementCache().derivativesOption.isDisplayed();
    }

    /**
     * Get the text of the currently selected creation type. If the options are not present return an empty string.
     *
     * @return The text of the current selected creation type.
     */
    public String getCreationTypeSelected()
    {
        String option = "";

        if(elementCache().derivativesOption.isDisplayed())
        {
            if(elementCache().derivativesOption.isChecked())
            {
                option = elementCache().derivativesOption.getComponentElement().getDomAttribute("value");
            }
            else
            {
                option = elementCache().poolOption.getComponentElement().getDomAttribute("value");
            }
        }

        return option;
    }

    public EntityBulkInsertDialog setQuantity(int quantity)
    {
        getWrapper().setFormElement(elementCache().quantity, Integer.toString(quantity));
        return this;
    }

    public String getQuantity()
    {
        return getWrapper().getFormElement(elementCache().quantity);
    }

    /**
     * Get a list of the displayed fields that can be set for this entity. Note if a field is marked as required
     * the '*' will be returned as part of the field name.
     *
     * @return A list of the fields for the entity.
     */
    public List<String> getDisplayedFieldLabels()
    {
        // Could be shortened to do something like this, but wanted to clean up the values returned.
        // return elementCache().fieldLabels().stream().map(el -> el.getText().trim()).collect(Collectors.toList());

        List<String> fieldLabels = new ArrayList<>();

        for(WebElement el : elementCache().fieldLabels())
        {
            String temp = el.getText();
            fieldLabels.add(temp.replace("*", "").trim());
        }

        return fieldLabels;
    }

    /**
     * Get the label next to the quantity text box. This will change depending upon the creation option selected.
     *
     * @return The text of the label next to the quantity box.
     */
    public String getQuantityLabel()
    {
        return elementCache().quantityLabel.getText();
    }

    public EntityBulkInsertDialog setDescription(String description)
    {
        getWrapper().setFormElement(elementCache().description, description);
        return this;
    }

    public String getDescription()
    {
        return getWrapper().getFormElement(elementCache().description);
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkInsertDialog setTextArea(CharSequence fieldIdentifier, String value)
    {
        elementCache().textArea(fieldIdentifier).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkInsertDialog setTextField(CharSequence fieldIdentifier, String value)
    {
        elementCache().textInput(fieldIdentifier).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getTextField(CharSequence fieldIdentifier)
    {
        return elementCache().textInput(fieldIdentifier).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkInsertDialog setNumericField(CharSequence fieldIdentifier, String value)
    {
        elementCache().textInput(fieldIdentifier).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param selectValues values to select
     * @return this component
     */
    public EntityBulkInsertDialog setSelectionField(CharSequence fieldIdentifier, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = elementCache().selectionField(fieldIdentifier);
        selectValues.forEach(reactSelect::filterSelect);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public List<String> getSelectionField(CharSequence fieldIdentifier)
    {
        return elementCache().selectionField(fieldIdentifier).getSelections();
    }

    /**
     * Clear the value(s) from a field that is a drop down selection field.
     *
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return This insert dialog.
     */
    public EntityBulkInsertDialog clearSelectionField(CharSequence fieldIdentifier)
    {
        elementCache().selectionField(fieldIdentifier).clearSelection();
        return this;
    }

    // For use when the field is of an unknown type, as can occur in fuzz tests
    public void setValue(FieldDefinition field, Object newValue)
    {
        if (field.getType() == FieldDefinition.ColumnType.TextChoice || field.getLookup() != null)
            setSelectionField(field.getName(), newValue instanceof String ? List.of((String) newValue) : (List<String>) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Integer || field.getType() == FieldDefinition.ColumnType.Decimal || field.getType() == FieldDefinition.ColumnType.Double)
            setNumericField(field.getName(), String.valueOf(newValue));
        else if (field.getType() == FieldDefinition.ColumnType.Date || field.getType() == FieldDefinition.ColumnType.DateAndTime || field.getType() == FieldDefinition.ColumnType.Time)
            setDateTimeField(field.getName(), newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Boolean)
            setBooleanField(field.getName(), (Boolean) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.MultiLine)
            setTextArea(field.getName(), (String) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.File)
            attachFile(field.getName(), (File) newValue);
        else
            setTextField(field.getName(), (String) newValue);
    }

    public void setInsertFieldValues(List<FieldDefinition> fields, Map<String, Object> data)
    {
        for (FieldDefinition field : fields)
        {
            Object value = data.get(field.getEffectiveLabel());
            if (value == null)
                continue;

            setValue(field, value);
        }
    }

    /**
     * Can be used to set a DateTime, Date-only or Time-only field. Pass in a LocalDateTime, LocalDate or LocalTime
     * object to use the picker to set the field. If a text value is passed in it is used as a literal and jut typed
     * into the textbox.
     *
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param dateTime A LocalDateTime, LocalDate, LocalTime or String.
     * @return A reference to this page.
     */
    public EntityBulkInsertDialog setDateTimeField(CharSequence fieldIdentifier, Object dateTime)
    {
        ReactDateTimePicker dateTimePicker = elementCache().dateInput(fieldIdentifier);
        dateTimePicker.select(dateTime);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getDateTimeField(CharSequence fieldIdentifier)
    {
        return elementCache().dateInput(fieldIdentifier).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param checked value to set
     * @return this component
     */
    public EntityBulkInsertDialog setBooleanField(CharSequence fieldIdentifier, boolean checked)
    {
        elementCache().checkBox(fieldIdentifier).set(checked);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param file file to attach
     * @return this component
     */
    public EntityBulkInsertDialog attachFile(CharSequence fieldIdentifier, File file)
    {
        elementCache().fileUploadField(fieldIdentifier).attachFile(file);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return this component
     */
    public EntityBulkInsertDialog removeFile(CharSequence fieldIdentifier)
    {
        elementCache().fileUploadField(fieldIdentifier).removeFile();
        return this;
    }

    /**
     * Finds a validation/error message in the dialog, if one exists.
     * @return the optional element containing the message
     */
    public Optional<WebElement> validationMessage()
    {
        return elementCache().validationMessage.findOptionalElement(this);
    }

    /**
     * Gets the text value of an error/warning message in the dialog, if it exists within 2 seconds.
     * Treats non-existence of the error message as a failure
     * @return The text of the error message, if it appears.
     */
    public String waitForValidationError()
    {
        WebDriverWrapper.waitFor(()-> validationMessage().isPresent(),
                "Field validation error did not appear", 2000);
        return validationMessage().get().getText();
    }

    public void clickAddRows()
    {
        elementCache().addRowsButton.click();
        waitForClose();

        // todo: maybe wait for the grid in the entityinsertPanel to update

        try
        {
            if (BootstrapLocators.errorBanner.findElement(getComponentElement()).isDisplayed())
                throw new IllegalStateException("Error message on dialog: '" + BootstrapLocators.errorBanner.findElement(getComponentElement()).getText() + "'.");
        }
        catch(StaleElementReferenceException stale)
        {
            // Do nothing if stale.
        }
    }

    /**
     * Click the 'Add' button and wait for an alert (error) message to be shown on the dialog.
     *
     * @return The text displayed in the alert.
     */
    public String clickAddRowsExpectError()
    {
        elementCache().addRowsButton.click();
        WebDriverWrapper.waitFor(()->elementCache().alert.isDisplayed(), "Expected alert error was not shown.", 500);

        return elementCache().alert.getText();
    }

    /**
     * Get the text from the 'add' button. Text may change depending upon what is being added.
     *
     * @return Text on the add button.
     */
    public String getAddButtonText()
    {
        return elementCache().addRowsButton.getText();
    }

    public void clickCancel()
    {
        elementCache().cancelButton.click();
        waitForClose();

        try
        {
            if (BootstrapLocators.errorBanner.findElement(getComponentElement()).isDisplayed())
                throw new IllegalStateException("Error message on dialog: '" + BootstrapLocators.errorBanner.findElement(getComponentElement()).getText() + "'.");

        }
        catch (StaleElementReferenceException stale)
        {
            // Do nothing if stale.
        }
    }

    @Override
    protected void waitForReady()
    {
        super.waitForReady();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable( elementCache().addRowsButton ));
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

    protected class ElementCache extends EntityBulkDialog.ElementCache
    {
        public final Locator validationMessage = Locator.tagWithClass("span", "validation-message");

        @Override
        public WebElement formRow(CharSequence fieldIdentifier)
        {
            String fieldKey = FieldKey.fromName(fieldIdentifier).toString();
            return _rows.computeIfAbsent(fieldKey, fk ->
                Locator.tagWithClass("div", "row")
                    // TODO: Shouldn't need to be case-insensitive. Parent/source lookups have weird casing
                    .withDescendant(Locator.tagWithAttributeIgnoreCase("label", "for", fieldKey))
                    .findElement(this));
        }

        public List<WebElement> fieldLabels()
        {
            return fieldLabels.findElements(this);
        }

        WebElement cancelButton = Locator.tagWithClass("button", "test-loc-cancel-button")
                .findWhenNeeded(getComponentElement());

        WebElement addRowsButton = Locator.tagWithClass("button", "test-loc-submit-for-edit-button")
                .findWhenNeeded(getComponentElement());

        WebElement quantityLabel = Locator.tagWithAttribute("label", "for", "numItems")
                .findWhenNeeded(getComponentElement());

        WebElement quantity = Locator.tagWithId("input", "numItems")
                .findWhenNeeded(getComponentElement());

        WebElement description = Locator.tagWithId("textarea", "Description")
                .findWhenNeeded(getComponentElement());

        RadioButton derivativesOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Derive")
                .findWhenNeeded(getComponentElement());

        RadioButton poolOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Pool")
                .findWhenNeeded(getComponentElement());

        RadioButton aliquotOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Aliquot")
                .findWhenNeeded(getComponentElement());

        WebElement alert = Locator.tagWithClassContaining("div", "alert-danger")
                .findWhenNeeded(getComponentElement());

        final Locator fieldLabels = Locator.tag("hr").followingSibling("div").child(Locator.byClass("control-label"));
    }

}