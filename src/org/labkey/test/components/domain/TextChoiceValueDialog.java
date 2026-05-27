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
package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * This is the dialog that allows a user to enter the list of possible values for a TextChoice field.
 */
public class TextChoiceValueDialog extends ModalDialog
{
    private final DomainFieldRow _row;

    /**
     * Constructor with a field row and a finder.
     *
     * @param row The {@link DomainFieldRow} that contains the TextChoice field.
     * @param finder A {@link ModalDialogFinder}. The caller constructs the condition for finding the dialog.
     */
    public TextChoiceValueDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    /**
     * Constructor that only takes a {@link DomainFieldRow}. Dialog is found based on its title.
     *
     * @param row A {@link DomainFieldRow} that contains the TextChoice field.
     */
    public TextChoiceValueDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle(String.format("Add Text Choice Values for %s", row.getName())));
    }

    /**
     * Add the list of stings as possible values for a TrextChoice field.
     *
     * @param values List of string of possible values.
     * @return This dialog.
     */
    public TextChoiceValueDialog addValues(List<String> values)
    {
        String val = String.join("\n", values);
        getWrapper().setFormElement(elementCache().textArea, val);

        // Need to send a Keys.ENTER to the text area to trigger the change event and enable the Apply button.
        elementCache().textArea.sendKeys(Keys.ENTER);

        return this;
    }

    /**
     * Get the value count text (e.g. '3 new values provided').
     *
     * @return The text displayed ont he dialog.
     */
    public String getValueCountText()
    {
        return elementCache().valueCount.getText();
    }

    /**
     * Get the error message displayed in the dialog, if any.
     *
     * @return The error text, or null if no error is displayed.
     */
    public String getError()
    {
        WebElement error = Locator.tagWithClass("div", "domain-text-choices-error")
                .findElementOrNull(this);
        return error != null ? error.getText() : null;
    }

    /**
     * Click the apply button.
     *
     * @return The {@link DomainFieldRow} that has the TextChoice field.
     */
    public DomainFieldRow clickApply()
    {
        dismiss("Apply");
        return _row;
    }

    /**
     * Cancel out of the dialog.
     *
     * @return The {@link DomainFieldRow} that has the TextChoice field.
     */
    public DomainFieldRow clickCancel()
    {
        dismiss("Cancel");
        return _row;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final WebElement textArea = Locator.tag("textarea")
                .findWhenNeeded(this);

        final WebElement valueCount = Locator.tagWithClass("div", "text-choice-value-count")
                .refindWhenNeeded(this);
    }

}
