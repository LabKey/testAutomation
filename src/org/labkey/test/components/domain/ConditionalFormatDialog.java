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
import org.openqa.selenium.WebElement;

import java.util.List;

public class ConditionalFormatDialog extends ModalDialog
{
    private final DomainFieldRow _row;

    private ConditionalFormatDialog(DomainFieldRow row, ModalDialogFinder finder)
    {
        super(finder);
        _row = row;
    }

    public ConditionalFormatDialog(DomainFieldRow row)
    {
       this(row, new ModalDialogFinder(row.getDriver()).withTitle("Conditional Formatting for " + row.getName()));
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
        final WebElement addformattingButton = Locator.tagWithClass("div", "domain-validation-add-btn")
                .findWhenNeeded(this);
    }

}
