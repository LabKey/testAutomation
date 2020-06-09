/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.components;

import org.jetbrains.annotations.NotNull;
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
        // TODO: Move indeterminate state check into base Checkbox class (in trunk)
        if (isIndeterminate())
        {
            toggle();
            WebDriverWrapper.waitFor(() -> !isIndeterminate(), 500);
        }

        if (checked != isChecked())
            toggle();
        WebDriverWrapper.waitFor(() -> checked == isChecked(), "Failed to " + (checked ? "check" : "uncheck") + " checkbox", 1000);
    }
}
