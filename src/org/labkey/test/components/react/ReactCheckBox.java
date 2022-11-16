
package org.labkey.test.components.react;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebElement;

/* this component wraps the ternary checkbox that uses hidden attributes 'checked' and 'indeterminate' to signal its state.
* if the box is 'checked', the hidden attribute called 'checked' equals 'true'- otherwise it is null
* if the box is 'indeterminate', that attribute is true - otherwise it is null
* if both are null, the box is unchecked */
public class ReactCheckBox extends Checkbox
{
    public ReactCheckBox(WebElement element)
    {
       super(element);
    }

    @Override
    public boolean isEnabled()
    {
        return getComponentElement().isEnabled();
    }

    @Override
    public boolean isSelected()
    {
        return isChecked();
    }

    @Override
    public boolean isDisplayed()
    {
        return getComponentElement().isDisplayed();
    }

    @Override
    public boolean isChecked()
    {
        String checkedAttribute = getComponentElement().getAttribute("checked");
        return checkedAttribute != null && checkedAttribute.equals("true");
    }

    public ReactCheckBox assertChecked(boolean shouldBeChecked)
    {
        if (shouldBeChecked)
            WebDriverWrapper.waitFor(() -> isChecked(), "Expect the checkbox to be checked", 500);
        else
            WebDriverWrapper.waitFor(()-> !isChecked(), "Expect the checkbox not to be checked", 500);
        return this;
    }

    public boolean isIndeterminate()
    {
        String indeterminate = getComponentElement().getAttribute("indeterminate");
        return indeterminate != null && indeterminate.equals("true");
    }

    public ReactCheckBox assertIndeterminate(boolean shouldBeIndeterminate)
    {
        if (shouldBeIndeterminate)
            WebDriverWrapper.waitFor(() -> isIndeterminate(), "Expect the checkbox to be indeterminate", 500);
        else
            WebDriverWrapper.waitFor(()-> !isIndeterminate(), "Expect the checkbox not to be indeterminate", 500);
        return this;
    }

    @Override
    public void set(@NotNull Boolean checked)
    {
        if (checked)
            set(true, CheckboxState.Checked);
        else
            set(false, CheckboxState.Unchecked);
    }

    /**
     *  Checks or un-checks the box and optionally validates intended state.
     * @param checked   the intended state.
     * @param expectedState  The intended state to verify before returning.  If null, no verification will occur
     */
    public void set(@NotNull Boolean checked, @Nullable CheckboxState expectedState)
    {
        if (isIndeterminate())  // when the checkbox is in an indeterminate state, clicking it will un-check it.
        {
            toggle();
            WebDriverWrapper.waitFor(() -> !isIndeterminate(), 500);
        }

        if (checked != isChecked())
            toggle();

        // Sometimes (like when the box has a ternary state, as the 'select all' box in grids)
        // checking the box won't result in a checked state- it will instead land on 'indeterminate'.

        if (expectedState != null)  // only validate if the caller has indicated their expectation
        {
            switch (expectedState)
            {
                case Checked:
                    WebDriverWrapper.waitFor(() -> isChecked(),
                            "Failed to check checkbox", 1000);
                    break;
                case Indeterminate:
                    WebDriverWrapper.waitFor(() -> isIndeterminate(),
                            "Expected checkbox to be in an indeterminate state", 1000);
                    break;
                case Unchecked:
                    WebDriverWrapper.waitFor(() -> !isChecked() && !isIndeterminate(),
                            "Failed to uncheck checkbox", 1000);
                    break;
            }
        }

    }

    public enum CheckboxState
    {
        Checked,
        Indeterminate,
        Unchecked;
    }
}


