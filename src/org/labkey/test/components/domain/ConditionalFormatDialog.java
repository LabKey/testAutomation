package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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

    public List<ConditionalFormatPanel> formatPanels()
    {
        return new ConditionalFormatPanel.ConditionalFormatPanelFinder(this).findAll(this);
    }

    public ConditionalFormatPanel getOpenFormatPanel()
    {
        return new ConditionalFormatPanel.ConditionalFormatPanelFinder(this).find(this);
    }

    public ConditionalFormatPanel addFormatPanel()
    {
        int targetIndex = formatPanels().size();
        elementCache().addformattingButton.click();
        return getPanelByIndex(targetIndex);
    }

    public ConditionalFormatPanel getPanelByIndex(int index)
    {
        return new ConditionalFormatPanel.ConditionalFormatPanelFinder(this)
                .withIndex(index).find(this);
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
        final WebElement addformattingButton = Locator.tagWithClass("div", "domain-validation-add-btn")
                .findWhenNeeded(this);
    }

}
