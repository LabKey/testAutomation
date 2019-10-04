package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class ConditionalFormatDialog extends ModalDialog
{
    private DomainFieldRow _row;

    private ConditionalFormatDialog(DomainFieldRow row, ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
        _row = row;
    }

    public ConditionalFormatDialog(DomainFieldRow row, WebDriver driver)
    {
       this(row, new ModalDialogFinder(driver).withTitle("Conditional Formatting for " + row.getName()), driver);
    }

    public ConditionalFormatPanel getOpenFormatPanel()
    {
        return new ConditionalFormatPanel.ConditionalFormatPanelFinder(getDriver()).find(this);
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
        // TODO: Add elements that are in the component
        final Input input = Input(Locator.css("input"), getDriver()).findWhenNeeded(this);
        final WebElement button = Locator.css("button").findWhenNeeded(this);
    }

}
