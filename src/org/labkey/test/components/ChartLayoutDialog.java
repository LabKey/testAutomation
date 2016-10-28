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
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ChartLayoutDialog<EC extends ChartLayoutDialog.ElementCache> extends ChartWizardDialog<EC>
{
    public ChartLayoutDialog(WebDriver driver)
    {
        super("Customize look and feel", driver);
    }

    @Deprecated
    public ChartLayoutDialog(BaseWebDriverTest test)
    {
        this(test.getDriver());
    }

    @Deprecated // Does nothing
    public void waitForDialog()
    {
    }

    public List<String> getAvailableTabs()
    {
        return getWrapper().getTexts(Locator.xpath("//div[contains(@class, 'navigation-panel')]//div[contains(@class, 'item')]").findElements(this));
    }

    public ChartLayoutDialog clickGeneralTab()
    {
        clickTab(elementCache().generalTab);
        return this;
    }

    public ChartLayoutDialog clickXAxisTab()
    {
        clickTab(elementCache().xAxisTab);
        return this;
    }

    public ChartLayoutDialog clickYAxisTab()
    {
        clickTab(elementCache().yAxisTab);
        return this;
    }

    public ChartLayoutDialog clickDeveloperTab()
    {
        clickTab(elementCache().developerTab);
        return this;
    }

    private void clickTab(WebElement tabElement)
    {
        tabElement.click();
        getWrapper().sleep(500);
    }

    public void setScaleType(ScaleType scaleType)
    {
        switch(scaleType)
        {
            case Linear:
                getWrapper().click(elementCache().visibleLinearScaleRadioButton);
                break;
            case Log:
                getWrapper().click(elementCache().visibleLogScaleRadioButton);
                break;
        }
    }

    public ChartLayoutDialog setXAxisScale(ScaleType scaleType)
    {
        clickXAxisTab();
        setScaleType(scaleType);
        return this;
    }

    public ChartLayoutDialog setXAxisLabel(String label)
    {
        clickXAxisTab();
        setLabel(label);
        return this;
    }

    public String getXAxisLabel()
    {
        clickXAxisTab();
        return getWrapper().getFormElement(elementCache().visibleLabelTextBox);
    }

    public ChartLayoutDialog setYAxisScale(ScaleType scaleType)
    {
        clickYAxisTab();
        setScaleType(scaleType);
        return this;
    }

    public ChartLayoutDialog setYAxisLabel(String label)
    {
        clickYAxisTab();
        setLabel(label);
        return this;
    }

    public String getYAxisLabel()
    {
        clickYAxisTab();
        return getWrapper().getFormElement(elementCache().visibleLabelTextBox);
    }

    private void setLabel(String label)
    {
        getWrapper().setFormElement(elementCache().visibleLabelTextBox, label);
    }

    public ChartLayoutDialog setPlotTitle(String title)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotTitleTextBox, title);
        return this;
    }

    public String getPlotTitle()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().plotTitleTextBox);
    }

    public ChartLayoutDialog setPlotWidth(String width)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotWidthTextBox, width);
        return this;
    }

    public int getPlotWidth()
    {
        clickGeneralTab();
        return Integer.parseInt(getWrapper().getFormElement(elementCache().plotWidthTextBox));
    }

    public ChartLayoutDialog setPlotHeight(String height)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotHeightTextBox, height);
        return this;
    }

    public int getPlotHeight()
    {
        clickGeneralTab();
        return Integer.parseInt(getWrapper().getFormElement(elementCache().plotHeightTextBox));
    }

    protected void setColor(String colorLabel, String hexColorValue)
    {
        String tempStr = hexColorValue.replace("#", "").toUpperCase();
        getWrapper().click(Locator.xpath(elementCache().VISIBLE_PANEL_XPATH + "//label[text() = '" + colorLabel + "']/following-sibling::div[not(contains(@class, 'x4-item-disabled'))]/a[contains(@class, '" + tempStr + "')]"));
    }

    protected void setSliderValue(WebElement sliderElement, int newValue)
    {
        // An alternative to actually moving the slider might be to have some javascript code that sets the aria-valuenow attribute of the element.
        int currentValue = getSliderCurrentValue(sliderElement);
        int xChangeAmount, minAllowedValue, maxAllowedValue;
        WebElement sliderThumb = sliderElement.findElement(By.className("x4-slider-thumb"));

        minAllowedValue = Integer.parseInt(sliderElement.getAttribute("aria-valuemin"));
        maxAllowedValue = Integer.parseInt(sliderElement.getAttribute("aria-valuemax"));

        // Protect against bad inputs.
        if(newValue > maxAllowedValue)
            newValue = maxAllowedValue;
        if(newValue < minAllowedValue)
            newValue = minAllowedValue;

        if(currentValue == newValue)
            return;

        if(currentValue > newValue)
        {
            xChangeAmount = -10;
            while(getSliderCurrentValue(sliderElement) > newValue)
                getWrapper().dragAndDrop(sliderThumb, xChangeAmount, 0);
        }
        else
        {
            xChangeAmount = 10;
            while(getSliderCurrentValue(sliderElement) < newValue)
                getWrapper().dragAndDrop(sliderThumb, xChangeAmount, 0);
        }

    }

    protected int getSliderCurrentValue(WebElement sliderValue)
    {
        return Integer.parseInt(sliderValue.getAttribute("aria-valuenow"));
    }

    public ChartLayoutDialog clickDeveloperEnable()
    {
        clickDeveloperTab();
        getWrapper().clickButton("Enable", 0);
        getWrapper().waitForText("// use LABKEY.ActionURL.buildURL to generate a link");
        return this;
    }

    public ChartLayoutDialog clickDeveloperDisable(boolean clickYes)
    {
        clickDeveloperTab();
        getWrapper().clickButton("Disable", 0);
        getWrapper()._extHelper.waitForExtDialog("Confirmation...");

        if(clickYes)
        {
            getWrapper().clickButton("Yes", 0);
        }
        else
        {
            getWrapper().clickButton("No", 0);
        }

        return this;
    }

    public ChartLayoutDialog clickDeveloperSourceTab()
    {
        clickDeveloperTab();
        getWrapper()._ext4Helper.clickTabContainingText("Source");
        return this;
    }

    public String getDeveloperSourceContent()
    {
        clickDeveloperSourceTab();
        return getWrapper()._extHelper.getCodeMirrorValue("point-click-fn-textarea");
    }

    public ChartLayoutDialog setDeveloperSourceContent(String source)
    {
        clickDeveloperSourceTab();
        getWrapper()._extHelper.setCodeMirrorValue("point-click-fn-textarea", source);
        return this;
    }

    public ChartLayoutDialog clickDeveloperHelpTab()
    {
        clickDeveloperTab();
        getWrapper()._ext4Helper.clickTabContainingText("Help");
        return this;
    }

    public String getDeveloperHelpContent()
    {
        clickDeveloperHelpTab();
        return getWrapper()._extHelper.getCodeMirrorValue("point-click-fn-textarea");
    }

    public void clickApply()
    {
        clickApply(10000);
    }

    public void clickApply(int waitTime)
    {
        getWrapper().clickButton("Apply", 0);
        getWrapper().sleep(1000);

    }

    public void clickApplyWithError()
    {
        getWrapper().clickButton("Apply", 0);
        getWrapper().sleep(1000);
    }

    public void clickCancel()
    {
        getWrapper().clickButton("Cancel", 0);
    }

    @Override
    protected EC newElementCache()
    {
        return (EC)new ElementCache();
    }

    class ElementCache extends ChartWizardDialog.ElementCache
    {
        static public final String VISIBLE_PANEL_XPATH = "//div[contains(@class, 'center-panel')]//div[contains(@class, 'x4-fit-item')][not(contains(@class, 'x4-item-disabled'))][not(contains(@style, 'display: none;'))]";

        public WebElement generalTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='General']"), this);
        public WebElement xAxisTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='X-Axis']"), this);
        public WebElement yAxisTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='Y-Axis']"), this);
        public WebElement developerTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='Developer']"), this);
        public WebElement visiblePanel = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH), getWrapper().getWrappedDriver());
        public WebElement plotTitleTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Title:']/parent::td/following-sibling::td//input"), this);
        public WebElement plotWidthTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='width']"), this);
        public WebElement plotHeightTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='height']"), this);
        public WebElement developerEnable = new LazyWebElement(Locator.xpath("//span[text()='Enable']"), this);
        public WebElement developerDisable = new LazyWebElement(Locator.xpath("//span[text()='Disable']"), this);
        // Making these elements locators because it looks like once a WebElement creates a reference it doesn't re-evaluate the xpath. These three elements are shared on two tabs.
        public Locator visibleLinearScaleRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='linear']/preceding-sibling::input[@type='button']");
        public Locator visibleLogScaleRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='log']/preceding-sibling::input[@type='button']");
        public Locator visibleLabelTextBox = Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='label']");
    }

    public enum ScaleType
    {
        Linear,
        Log
    }

}
