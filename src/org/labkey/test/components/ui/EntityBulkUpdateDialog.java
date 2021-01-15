package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.ToggleButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Automates product component src/components/forms/QueryInfoForms, with BulkUpdateForm.d.ts
 */
public class EntityBulkUpdateDialog extends ModalDialog
{
    private final int WAIT_TIMEOUT = 2000;

    public EntityBulkUpdateDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle(" selected from "));
    }

    private EntityBulkUpdateDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    // enable/disable field editable state

    public boolean isFieldEnabled(String columnTitle)
    {
        return elementCache().getToggle(columnTitle).get();
    }

    public EntityBulkUpdateDialog setEditableState(String columnTitle, boolean enable)
    {
        elementCache().getToggle(columnTitle).set(enable);
        return this;
    }

    // interact with selection fields

    public EntityBulkUpdateDialog setSelectionField(String columnTitle, List<String> selectValues)
    {
        setEditableState(columnTitle, true);
        FilteringReactSelect reactSelect = elementCache().getSelect(columnTitle);
        WebDriverWrapper.waitFor(reactSelect::isEnabled,
                "the ["+columnTitle+"] reactSelect did not become enabled in time", WAIT_TIMEOUT);
        selectValues.forEach(s -> {reactSelect.filterSelect(s);});
        return this;
    }

    public List<String> getSelectionFieldValues(String fieldKey)
    {
        return elementCache().getSelect(fieldKey).getSelections();
    }

    public EntityBulkUpdateDialog setTextArea(String fieldKey, String text)
    {
        setEditableState(fieldKey, true);
        Input input = elementCache().textArea(fieldKey);
        WebDriverWrapper.waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the input did not become enabled in time", WAIT_TIMEOUT);
        input.set(text);
        return this;
    }

    public String getTextArea(String fieldKey)
    {
        return elementCache().textArea(fieldKey).get();
    }

    // get/set text fields with ID

    public EntityBulkUpdateDialog setTextField(String fieldKey, String value)
    {
        Input input = elementCache().textInput(fieldKey);
        setEditableState(fieldKey, true);
        WebDriverWrapper.waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the input did not become enabled in time", WAIT_TIMEOUT);
        input.set(value);
        return this;
    }

    public String getTextField(String fieldKey)
    {
        return elementCache().textInput(fieldKey).get();
    }

    public EntityBulkUpdateDialog setNumericField(String fieldKey, String value)
    {
        Input input = elementCache().numericInput(fieldKey);
        setEditableState(fieldKey, true);
        WebDriverWrapper.waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the input did not become enabled in time", WAIT_TIMEOUT);
        input.set(value);
        return this;
    }

    public String getNumericField(String fieldKey)
    {
        return elementCache().numericInput(fieldKey).get();
    }

    public EntityBulkUpdateDialog setDateField(String fieldKey, String dateString)
    {
        setEditableState(fieldKey, true);
        ReactDatePicker input = elementCache().dateInput("sampleDate");
        getWrapper().waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the checkbox did not become enabled in time", 2000);
        input.set(dateString);
        return this;
    }

    public String getDateField(String fieldKey)
    {
        return elementCache().dateInput(fieldKey).get();
    }

    public EntityBulkUpdateDialog setBooleanField(String fieldKey, boolean checked)
    {
        setEditableState(fieldKey, true);
        Checkbox box = getCheckBox(fieldKey);
        getWrapper().waitFor(()-> box.getComponentElement().getAttribute("disabled")==null,
                "the checkbox did not become enabled in time", 2000);
        box.set(checked);
        return this;
    }

    public boolean getBooleanField(String fieldKey)
    {
        return getCheckBox(fieldKey).get();
    }

    private Checkbox getCheckBox(String fieldKey)
    {
        WebElement row = elementCache().formRow(fieldKey);
        return new Checkbox(elementCache().checkBoxLoc.findElement(row));
    }

    public List<String> getColumns()
    {
        List<WebElement> labels = Locator.tagWithClass("label", "control-label").withAttribute("for")
                .findElements(this);
        List<String> columns = new ArrayList<>();
        labels.stream().forEach(a -> columns.add(a.getAttribute("for")));
        return columns;
    }

    // dismiss the dialog

    public void clickEditWithGrid()
    {
        dismiss("Edit with Grid");
    }

    public boolean isUpdateButtonEnabled()
    {
        WebElement btn = elementCache().updateButton.findElement(this);
        return btn.getAttribute("disabled") == null;
    }

    public void clickUpdate()
    {
        String updateButtonText = getUpdateButtonText();
        if (!isUpdateButtonEnabled())
        {
            getWrapper().log("the ["+updateButtonText+"] button cannot be clicked, it is disabled");
        }
        dismiss(updateButtonText);
    }

    private String getUpdateButtonText()
    {
         WebElement btn = elementCache().updateButton.waitForElement(this, 2000);
         return btn.getText();
    }

    public void clickCancel()
    {
        dismiss("Cancel");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        public WebElement formRow(String fieldKey)
        {
            return Locator.tagWithClass("div", "row")
                    .withChild(Locator.tagWithAttribute("label", "for", fieldKey))
                    .waitForElement(this, WAIT_TIMEOUT);
        }

        public ToggleButton getToggle(String fieldKey)
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).waitFor(formRow(fieldKey));
        }

        public FilteringReactSelect getSelect(String fieldKey)
        {
            return FilteringReactSelect.finder(getDriver()).waitFor(formRow(fieldKey));
        }

        public Input textInput(String fieldKey)
        {
            WebElement inputEl = textInputLoc.waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public Input textArea(String fieldKey)
        {
            WebElement inputEl = Locator.textarea(fieldKey).waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public Input numericInput(String fieldKey)
        {
            WebElement inputEl = numberInputLoc.waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public ReactDatePicker dateInput(String fieldKey)
        {
            return new ReactDatePicker.ReactDateInputFinder(getDriver())
                    .withInputId(fieldKey).waitFor(formRow(fieldKey));
        }

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator numberInputLoc = Locator.tagWithAttribute("input", "type", "number");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");

        Locator updateButton = Locator.tagWithClass("button", "test-loc-submit-button");
    }

}
