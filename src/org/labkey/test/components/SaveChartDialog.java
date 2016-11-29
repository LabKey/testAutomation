/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class SaveChartDialog<EC extends Component.ElementCache> extends Component<EC>
{
    private  final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'save-chart-panel')]";

    protected WebElement _saveChartDialog;
    protected BaseWebDriverTest _test;

    public SaveChartDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _saveChartDialog;
    }

    public boolean isDialogVisible()
    {
        return elements().dialog.isDisplayed();
    }

    public void waitForDialog()
    {
        waitForDialog(false);
    }

    public void waitForDialog(boolean saveAs)
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//div[text()='" + (saveAs ? "Save as" : "Save") + "']"));
    }

    public void setReportName(String name)
    {
        _test.setFormElement(elements().reportName, name);
    }

    public void setReportDescription(String description)
    {
        _test.setFormElement(elements().reportDescription, description);
    }

    public void setThumbnailType(boolean auto)
    {
        if (auto)
            elements().autoThumbnail.click();
        else
            elements().noThumbnail.click();
    }

    public void clickCancel()
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("Cancel", 0);
    }

    public void clickSave()
    {
        clickSave(false);
    }

    public void clickSave(boolean expectReload)
    {
        Window w = new Window(elements().dialog, _test.getDriver());
        w.clickButton("Save", expectReload ? _test.WAIT_FOR_PAGE : 0);
    }

    public void waitForInvalid()
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//input[contains(@class, 'x4-form-invalid-field')]"));
    }

    public Elements elements()
    {
        return new Elements();
    }

    class Elements extends ElementCache
    {
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement reportName = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//td//label[text()='Report Name:']/parent::td/following-sibling::td//input"), _test.getDriver());
        public WebElement reportDescription = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//td//label[text()='Report Description:']/parent::td/following-sibling::td//textarea"), _test.getDriver());
        public WebElement noThumbnail = new LazyWebElement(Locator.xpath("//input[@type='button' and ../label[text()='None']]"), _test.getDriver());
        public WebElement autoThumbnail = new LazyWebElement(Locator.xpath("//input[@type='button' and ../label[text()='Auto-generate']]"), _test.getDriver());
    }
}
