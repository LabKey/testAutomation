/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RangeValidatorDialog extends ModalDialog
{
    private final DomainFieldRow _row;

    public RangeValidatorDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public RangeValidatorDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle("Range Validator(s) for " + row.getName()));
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
        final WebElement addValidatorButton = Locator.tagWithClass("div", "domain-validation-add-btn")
                .findWhenNeeded(this);
    }


}
