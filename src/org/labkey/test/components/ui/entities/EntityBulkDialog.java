package org.labkey.test.components.ui.entities;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.util.Pair;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.components.ui.files.FileAttachmentContainer;
import org.labkey.test.params.FieldKey;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Abstract base for {@link EntityBulkInsertDialog} and {@link EntityBulkUpdateDialog}.
 */
public abstract class EntityBulkDialog extends ModalDialog
{
    protected int _changeCounter = 0;

    protected EntityBulkDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getTextArea(CharSequence fieldIdentifier)
    {
        return elementCache().textArea(fieldIdentifier).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getNumericField(CharSequence fieldIdentifier)
    {
        return elementCache().textInput(fieldIdentifier).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public boolean getBooleanField(CharSequence fieldIdentifier)
    {
        return elementCache().checkbox(fieldIdentifier).get();
    }

    public String getFieldValue(WebElement input)
    {
        String value = input.getText();
        if (StringUtils.isEmpty(value))
            value = input.getAttribute("value");
        if (StringUtils.isEmpty(value))
            value = input.getAttribute("placeholder");
        if (StringUtils.isEmpty(value) && "checkbox".equals(input.getAttribute("type")))
            value = input.getAttribute("title");
        return value;
    }

    protected String getValueForReactSelect(ReactSelect reactSelect)
    {
        if (!reactSelect.getSelections().isEmpty())
        {
            return reactSelect.getSelections().get(0);
        }
        else
        {
            return "";
        }
    }

    private WebElement getAmountUnitsRow()
    {
        String fieldLabel = "Amount and Units";
        return elementCache().formRowByControlLabel(fieldLabel);
    }

    public Pair<String, String> getAmountAndUnitsReadOnlyValues()
    {
        String amountVal = getFieldValue(getAmountInput());
        String unitVal = Locator.tagWithClass("div", "select-input__value-container").withoutAttribute("type", "hidden").findElement(getAmountUnitsRow()).getText();
        return new Pair<>(amountVal, unitVal);
    }

    public Pair<String, String> getAmountAndUnitsInputValues()
    {
        enableAmountAndUnits();
        String amountVal = getWrapper().getFormElement(getAmountInput());
        String unitVal = getValueForReactSelect(getAmountUnitSelect());
        return new Pair<>(amountVal, unitVal);
    }

    public void enableAmountAndUnits()
    {
        ToggleButton toggle = new ToggleButton.ToggleButtonFinder(getDriver()).findOrNull(getAmountUnitsRow());
        if (toggle != null && !toggle.isOn())
        {
            toggle.set(true);
            _changeCounter++;
        }
    }

    public void disableAmountAndUnits()
    {
        ToggleButton toggle = new ToggleButton.ToggleButtonFinder(getDriver()).findOrNull(getAmountUnitsRow());
        if (toggle != null && toggle.isOn())
        {
            toggle.set(false);
            _changeCounter--;
        }
    }

    private WebElement getAmountInput()
    {
        return elementCache().amountInputLoc.findElement(getAmountUnitsRow());
    }

    public ReactSelect getAmountUnitSelect()
    {
        enableAmountAndUnits();
        return new ReactSelect(getAmountUnitsRow(), getDriver());
    }

    public void setAmountUnit(String amount, String unit)
    {
        enableAmountAndUnits();

        if (amount != null)
            getWrapper().setFormElement(getAmountInput(), amount);
        if (unit != null)
        {
            ReactSelect reactSelect = getAmountUnitSelect();
            if (!unit.isEmpty())
                reactSelect.select(unit);
            else
                reactSelect.clearSelection();
        }

        if (amount != null && unit != null)
            _changeCounter++;
    }

    @Override
    protected abstract ElementCache newElementCache();

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected abstract class ElementCache extends ModalDialog.ElementCache
    {
        protected final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        protected final Locator checkboxLoc = Locator.tagWithAttribute("input", "type", "checkbox");
        protected final Locator.XPathLocator amountInputLoc = Locator.tag("input").withAttribute("aria-label", "Amount");

        protected final Map<String, WebElement> _rows = new HashMap<>();

        /**
         * Returns the form row div that contains the controls for the given field.
         */
        public abstract WebElement formRow(CharSequence fieldIdentifier);

        // For composite fields (e.g. StoredAmount + Units) that render a <div> label instead of <label for="...">,
        private WebElement formRowByControlLabel(String fieldLabel)
        {
            return _rows.computeIfAbsent(fieldLabel, k ->
                    Locator.tagWithClass("div", "row")
                            .withChild(Locator.tagWithClass("div", "control-label").withText(fieldLabel))
                            .waitForElement(this, WAIT_FOR_JAVASCRIPT));
        }

        public FilteringReactSelect selectionField(CharSequence fieldIdentifier)
        {
            return new FilteringReactSelect(formRow(fieldIdentifier), getDriver());
        }

        public Checkbox checkbox(CharSequence fieldIdentifier)
        {
            return new Checkbox(checkboxLoc.findElement(formRow(fieldIdentifier)));
        }

        public Input textInput(CharSequence fieldIdentifier)
        {
            return new Input(textInputLoc.findElement(formRow(fieldIdentifier)), getDriver());
        }

        public Input textArea(CharSequence fieldIdentifier)
        {
            return new Input(Locator.tag("textarea").findElement(formRow(fieldIdentifier)), getDriver());
        }

        public ReactDateTimePicker dateInput(CharSequence fieldIdentifier)
        {
            return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver()).find(formRow(fieldIdentifier));
        }

        public FileAttachmentContainer fileUploadField(CharSequence fieldIdentifier)
        {
            return new FileAttachmentContainer(formRow(fieldIdentifier), getDriver());
        }
    }
}
