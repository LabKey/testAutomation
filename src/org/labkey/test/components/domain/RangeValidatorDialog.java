package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RangeValidatorDialog extends ModalDialog
{
    private DomainFieldRow _row;

    public RangeValidatorDialog(DomainFieldRow row, ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
        _row = row;
    }

    public RangeValidatorDialog(DomainFieldRow row, WebDriver driver)
    {
        this(row, new ModalDialogFinder(driver).withTitle("Range Validator for " + row.getName()), driver);
    }

    public List<RangeValidatorPanel> validators()
    {
        return new RangeValidatorPanel.RangeValidatorPanelFinder(this)
                .findAll(this);
    }

    public RangeValidatorPanel addValidationPanel(String name)
    {
        int targetIndex = validators().size();
        elementCache().addValidatorButton.click();
        return getValidationPanel(targetIndex).setName(name);
    }

    public RangeValidatorPanel getValidationPanel(int index)
    {
        return new RangeValidatorPanel.RangeValidatorPanelFinder(this)
                .byIndex(index).find(this);
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
        final WebElement addValidatorButton = Locator.tagWithClass("div", "domain-validation-add-btn")
                .findWhenNeeded(this);
    }


}
