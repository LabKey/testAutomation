package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.List;

public class TextChoiceValueDialog extends ModalDialog
{
    private DomainFieldRow _row;

    public TextChoiceValueDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public TextChoiceValueDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle(String.format("Add Text Choice Values for %s", row.getName())));
    }

    public TextChoiceValueDialog addValues(List<String> values)
    {
        String val = String.join("\n", values);
        getWrapper().setFormElement(elementCache().textArea, val);

        // Need to send a Keys.ENTER to the text area to trigger the change event and enable the Apply button.
        elementCache().textArea.sendKeys(Keys.ENTER);

        return this;
    }

    public String getValueCountText()
    {
        return elementCache().valueCount.getText();
    }

    public DomainFieldRow clickApply()
    {
        dismiss("Apply");
        return _row;
    }

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
