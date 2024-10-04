/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class SaveChartDialog extends Window<SaveChartDialog.Elements>
{
    private static final Locator DIALOG_LOC = Locator.byClass("chart-wizard-dialog").append(Locator.byClass("save-chart-panel"));
    private final TimeChartWizard reportWizard;
    private boolean expectNavigateOnSave = false;

    public SaveChartDialog(TimeChartWizard reportWizard)
    {
        super(DIALOG_LOC.waitForElement(reportWizard.getDriver(), 10000), reportWizard.getDriver());
        this.reportWizard = reportWizard;
    }

    public SaveChartDialog setReportName(String name)
    {
        getWrapper().setFormElement(elementCache().reportName, name);
        expectNavigateOnSave = true;
        return this;
    }

    public SaveChartDialog setReportDescription(String description)
    {
        getWrapper().setFormElement(elementCache().reportDescription, description);
        return this;
    }

    public SaveChartDialog setThumbnailType(ThumbnailType thumbnail)
    {
        RadioButton radioButton;
        switch (thumbnail)
        {
            case auto:
                radioButton = elementCache().autoThumbnail;
                break;
            case none:
            default:
                radioButton = elementCache().noThumbnail;
                break;
        }

        // Just a sanity check to make sure the radio buttonis checked.
        // Here to help with debugging issue with thumbnail images.
        radioButton.check();
        WebDriverWrapper.waitFor(radioButton::isChecked, "The thumbnail option was not checked.", 500);

        return this;
    }

    public ThumbnailType getThumbnailType()
    {
        if(elementCache().autoThumbnail.isChecked())
        {
            return ThumbnailType.auto;
        }
        else
        {
            return ThumbnailType.none;
        }
    }

    public SaveChartDialog setViewableBy(ViewableBy visibility)
    {
        switch (visibility)
        {
            case onlyMe:
                elementCache().onlyMe.check();
                break;
            case allReaders:
                elementCache().allReaders.check();
                break;
            default:
                throw new IllegalArgumentException("Unknown option: " + visibility);
        }
        return this;
    }

    public SaveChartDialog setInherit(boolean checked)
    {
        elementCache().inheritCheckbox.set(checked);
        return this;
    }

    public TimeChartWizard clickCancel()
    {
        clickButton("Cancel", true);
        return reportWizard;
    }

    public TimeChartWizard clickSave()
    {
        if (expectNavigateOnSave)
        {
            return new TimeChartWizard(getWrapper()).doAndWaitForUpdate(() -> clickButton("Save"))
                    .waitForReportRender();
        }
        else
        {
            Locator.XPathLocator successMsgLoc = Locator.byClass("labkey-message").withText("Report saved successfully.");
            WebElement successMsg = getWrapper().doAndWaitForElementToRefresh(() -> clickButton("Save", true), successMsgLoc, getWrapper().shortWait());
            getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(successMsg));
            return reportWizard.waitForReportRender();
        }
    }

    public Window clickSaveExpectingError()
    {
        clickButton("Save", false);
        return new Window.WindowFinder(getDriver()).withTitle("Error").waitFor();
    }

    public String clickSaveWithoutRequiredFields()
    {
        clickButton("Save", false);
        return waitForInvalid().getAttribute("name");
    }

    public WebElement waitForInvalid()
    {
        return Locator.tagWithClass("input", "x4-form-invalid-field").waitForElement(this, 10000);
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Window.ElementCache
    {
        private final WebElement reportName = new LazyWebElement(Locator.xpath("//td//label[text()='Report Name:']/parent::td/following-sibling::td//input"), this);
        private final WebElement reportDescription = new LazyWebElement(Locator.xpath("//td//label[text()='Report Description:']/parent::td/following-sibling::td//textarea"), this);
        private final RadioButton noThumbnail = new RadioButton.RadioButtonFinder().withLabel("None").findWhenNeeded(this);
        private final RadioButton autoThumbnail = new RadioButton.RadioButtonFinder().withLabel("Auto-generate").findWhenNeeded(this);
        private final RadioButton allReaders = new RadioButton.RadioButtonFinder().withLabel("All readers").findWhenNeeded(this);
        private final RadioButton onlyMe = new RadioButton.RadioButtonFinder().withLabel("Only me").findWhenNeeded(this);
        private final Checkbox inheritCheckbox = Ext4Checkbox().withLabel("Make this report available in child folders").findWhenNeeded(this);
    }

    public enum ViewableBy
    {
        allReaders, onlyMe
    }

    public enum ThumbnailType
    {
        auto, none
    }
}
