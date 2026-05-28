/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.components.study;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.study.DatasetDesignerPage;
import org.openqa.selenium.WebElement;

public class AdvancedDatasetSettingsDialog extends ModalDialog
{
    private final DatasetDesignerPage _page;

    public AdvancedDatasetSettingsDialog(DatasetDesignerPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Advanced Dataset Settings"));
        _page = page;
    }

    public AdvancedDatasetSettingsDialog setShowInOverview(boolean checked)
    {
        elementCache().showInOverviewCheckbox.set(checked);
        return this;
    }

    public boolean getShowInOverview()
    {
        return elementCache().showInOverviewCheckbox.get();
    }

    public AdvancedDatasetSettingsDialog setDatasetId(String value)
    {
        elementCache().datasetIdInput.set(value);
        return this;
    }

    public String getDatasetId()
    {
        return elementCache().datasetIdInput.get();
    }

    public AdvancedDatasetSettingsDialog selectVisitDateColumn(String column)
    {
        elementCache().visitDateColumnSelect.select(column);
        return this;
    }

    public String getVisitDateColumn()
    {
        return elementCache().visitDateColumnSelect.getValue();
    }

    public AdvancedDatasetSettingsDialog selectCohortAssociation(String value)
    {
        elementCache().cohortAssociationSelect.select(value);
        return this;
    }

    public String getCohortAssociation()
    {
        return elementCache().cohortAssociationSelect.getValue();
    }

    public AdvancedDatasetSettingsDialog setTag(String tag)
    {
        elementCache().tagInput.set(tag);
        return this;
    }

    public String getTag()
    {
        return elementCache().tagInput.get();
    }

    public AdvancedDatasetSettingsDialog shareDemographics(String by)
    {
        elementCache().shareDemographicSelect.select(by);
        return this;
    }
    public AdvancedDatasetSettingsDialog shareDemographics(DatasetDesignerPage.ShareDemographicsBy by)
    {
        elementCache().shareDemographicSelect.select(by.getOption());
        return this;
    }
    public DatasetDesignerPage.ShareDemographicsBy getShareDemographicsSelection()
    {
        return DatasetDesignerPage.ShareDemographicsBy.valueOf(elementCache().shareDemographicSelect.getValue());
    }

    public DatasetDesignerPage clickApply()
    {
        dismiss("Apply");
        return _page;
    }

    public DatasetDesignerPage clickCancel()
    {
        dismiss("Cancel");
        return _page;
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
        Checkbox showInOverviewCheckbox = Checkbox.Checkbox(Locator.id("showByDefault"))
                .findWhenNeeded(this);
        Input datasetIdInput = new Input(Locator.id("datasetId").findWhenNeeded(this),
                getDriver());

        WebElement visitDateRow = Locator.tagWithClass("div", "row")
                .containingIgnoreCase("Visit Date").findWhenNeeded(this);
        ReactSelect visitDateColumnSelect = ReactSelect.finder(getDriver())
                .findWhenNeeded(visitDateRow);

        WebElement cohortAssociationRow = Locator.tagWithClass("div", "row")
                .containingIgnoreCase("Cohort").findWhenNeeded(this);
        ReactSelect cohortAssociationSelect = ReactSelect.finder(getDriver())
                .findWhenNeeded(cohortAssociationRow);

        Input tagInput = new Input(Locator.id("tag").findWhenNeeded(this),
                getDriver());

        WebElement shareDemographicRow = Locator.tagWithClass("div", "row")
                .containingIgnoreCase("Share demographic").findWhenNeeded(this);
        ReactSelect shareDemographicSelect = ReactSelect.finder(getDriver())
                .findWhenNeeded(shareDemographicRow);
    }


}
