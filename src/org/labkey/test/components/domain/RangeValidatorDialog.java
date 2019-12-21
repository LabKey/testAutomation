package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RangeValidatorDialog extends ModalDialog
{
    private DomainFieldRow _row;

    public RangeValidatorDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public RangeValidatorDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle("Range Validator for " + row.getName()));
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

    public RangeValidatorDialog addValidator(FieldDefinition.RangeValidator validator)
    {
        RangeValidatorPanel panel = addValidationPanel(validator.getName())
                .setDescription(validator.getDescription())
                .setErrorMessage(validator.getMessage())
                .setFirstCondition(validator.getFirstType().getOperator())
                .setFirstValue(validator.getFirstRange());
        if (null != validator.getSecondRange())
            panel.setSecondCondition(validator.getSecondType().getOperator())
                    .setSecondValue(validator.getSecondRange());

        return this;
    }

    public RangeValidatorDialog setValidator(int index, FieldDefinition.RangeValidator validator)
    {
        RangeValidatorPanel panel = getValidationPanel(index)
                .setName(validator.getName())
                .setDescription(validator.getDescription())
                .setErrorMessage(validator.getMessage())
                .setFirstCondition(validator.getFirstType().getOperator())
                .setFirstValue(validator.getFirstRange());
        if (null != validator.getSecondRange())
            panel.setSecondCondition(validator.getSecondType().getOperator())
                .setSecondValue(validator.getSecondRange());

        return this;
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
