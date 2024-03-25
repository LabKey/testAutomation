package org.labkey.test.components.ui.entities;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EntityBulkInsertDialog extends ModalDialog
{
    public EntityBulkInsertDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle("Bulk"));
    }

    private EntityBulkInsertDialog(ModalDialogFinder finder)
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
                option = elementCache().derivativesOption.getComponentElement().getAttribute("value");
            }
            else
            {
                option = elementCache().poolOption.getComponentElement().getAttribute("value");
            }
        }

        return option;
    }

    public EntityBulkInsertDialog setQuantity(int quantity)
    {
        return setQuantity(Integer.toString(quantity));
    }

    public EntityBulkInsertDialog setQuantity(String quantity)
    {
        getWrapper().setFormElement(elementCache().quantity, quantity);
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
    public List<String> getDisplayedFieldNames()
    {
        // Could be shortened to do something like this, but wanted to clean up the values returned.
        // return elementCache().fieldNames().stream().map(el -> el.getText().trim()).collect(Collectors.toList());

        List<String> fieldNames = new ArrayList<>();

        for(WebElement el : elementCache().fieldNames())
        {
            String temp = el.getText();
            fieldNames.add(temp.replace("*", "").trim());
        }

        return fieldNames;
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

    public EntityBulkInsertDialog setTextField(String fieldKey, String value)
    {
        elementCache().textInput(fieldKey).set(value);
        return this;
    }

    public String getTextField(String fieldKey)
    {
        return elementCache().textInput(fieldKey).get();
    }

    public EntityBulkInsertDialog setNumericField(String fieldKey, String value)
    {
        elementCache().numericInput(fieldKey).set(value);
        return this;
    }

    public String getNumericField(String fieldKey)
    {
        return elementCache().numericInput(fieldKey).get();
    }

    public EntityBulkInsertDialog setSelectionField(String fieldCaption, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = elementCache().selectionField(fieldCaption);
        selectValues.forEach(reactSelect::filterSelect);
        return this;
    }

    public List<String> getSelectionField(String fieldCaption)
    {
        FilteringReactSelect reactSelect = elementCache().selectionField(fieldCaption);
        return reactSelect.getSelections();
    }

    /**
     * Clear the value(s) from a field that is a drop down selection field.
     *
     * @param fieldCaption Caption for the field.
     * @return This insert dialog.
     */
    public EntityBulkInsertDialog clearSelectionField(String fieldCaption)
    {
        FilteringReactSelect reactSelect = elementCache().selectionField(fieldCaption);
        reactSelect.clearSelection();
        return this;
    }

    public EntityBulkInsertDialog setFieldWithId(String id, String value)
    {
        getWrapper().setFormElement(Locator.tagWithId("input", id), value);
        return this;
    }

    public String getFieldWithId(String id)
    {
        return getWrapper().getFormElement(Locator.tagWithId("input", id));
    }

    public EntityBulkInsertDialog setDateField(String fieldKey, String dateString)
    {
        ReactDateTimePicker input = elementCache().dateInput(fieldKey);
        input.set(dateString);
        return this;
    }

    public String getDateField(String fieldKey)
    {
        return elementCache().dateInput(fieldKey).get();
    }

    public EntityBulkInsertDialog setBooleanField(String fieldKey, boolean checked)
    {
        Checkbox box = elementCache().checkBox(fieldKey);
        box.set(checked);
        return this;
    }

    public boolean getBooleanField(String fieldKey)
    {
        return elementCache().checkBox(fieldKey).get();
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

    protected class ElementCache extends ModalDialog.ElementCache
    {
        public Locator validationMessage = Locator.tagWithClass("span", "validation-message");

        public WebElement formRow(String fieldKey)
        {
            return Locator.tagWithClass("div", "row")
                    .withChild(Locator.tagWithAttribute("label", "for", fieldKey))
                    .findElement(this);
        }

        public FilteringReactSelect selectionField(String fieldCaption)
        {
            return FilteringReactSelect.finder(getDriver()).followingLabelWithSpan(fieldCaption).find(this);
        }

        public Checkbox checkBox(String fieldKey)
        {
            WebElement row = elementCache().formRow(fieldKey);
            return new Checkbox(checkBoxLoc.findElement(row));
        }

        public Input textInput(String fieldKey)
        {
            WebElement inputEl = textInputLoc.findElement(elementCache().formRow(fieldKey));
            return new Input(inputEl, getDriver());
        }

        public Input numericInput(String fieldKey)
        {
            WebElement inputEl = numberInputLoc.findElement(formRow(fieldKey));
            return new Input(inputEl, getDriver());
        }

        public ReactDateTimePicker dateInput(String fieldKey)
        {
            return new ReactDateTimePicker.ReactDateInputFinder(getDriver())
                    .withInputId(fieldKey).find(formRow(fieldKey));
        }

        public List<WebElement> fieldNames()
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

        RadioButton derivativesOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Derivatives")
                .findWhenNeeded(getComponentElement());

        RadioButton poolOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Pooled Samples")
                .findWhenNeeded(getComponentElement());

        RadioButton aliquotOption = new RadioButton.RadioButtonFinder().withNameAndValue("creationType", "Aliquots")
                .findWhenNeeded(getComponentElement());

        WebElement alert = Locator.tagWithClassContaining("div", "alert-danger")
                .findWhenNeeded(getComponentElement());

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator numberInputLoc = Locator.tagWithAttribute("input", "type", "number");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");
        final Locator fieldLabels = Locator.tag("hr").followingSibling("div").childTag("label");
    }

}
