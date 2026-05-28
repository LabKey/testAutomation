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

public class RegexValidatorDialog extends ModalDialog
{
    private final DomainFieldRow _row;

    public RegexValidatorDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public RegexValidatorDialog(DomainFieldRow row)
    {
        this(row, new ModalDialogFinder(row.getDriver()).withTitle("" + row.getName()));
    }


    List<RegexValidatorPanel> validationPanels()
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(this)
                .findAll(this);
    }
    public RegexValidatorPanel getValidationPanel()
    {
        return validationPanels().getFirst();
    }
    public RegexValidatorPanel getValidationPanel(int index)
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(this)
                .withIndex(index).find(this);
    }
    public RegexValidatorPanel getValidationPanel(String name)
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(this)
                .openedByName(name).find(this);
    }

    public RegexValidatorPanel addValidationPanel(String name)
    {
        int targetIndex = validationPanels().size();
        elementCache().addValidatorButton.click();      // adds a new validator clause panel to the dialog
        RegexValidatorPanel panel = new RegexValidatorPanel.RegexValidatorPanelFinder(this)
                .withIndex(targetIndex).find(this);     // find it by assuming its ID will have index lastId +1
        panel.setName(name);
        return panel;
    }

    public RegexValidatorDialog addValidator(FieldDefinition.RegExValidator validator)
    {
        addValidationPanel(validator.getName())
                .setExpression(validator.getExpression())
                .setErrorMessage(validator.getMessage())
                .setDescription(validator.getDescription());
        return this;
    }

    public RegexValidatorDialog setValidator(int index, FieldDefinition.RegExValidator validator)
    {
        getValidationPanel(index)
                .setName(validator.getName())
                .setExpression(validator.getExpression())
                .setErrorMessage(validator.getMessage())
                .setDescription(validator.getDescription());
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
