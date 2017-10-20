/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
import org.labkey.test.util.Ext4Helper;
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

    public boolean isDeveloperTabVisible()
    {
        return getWrapper().isElementPresent(elementCache().developerTabLoc);
    }

    public ChartLayoutDialog clickDeveloperTab()
    {
        clickTab(elementCache().developerTab);
        return this;
    }

    protected void clickTab(WebElement tabElement)
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

    public void setRangeType(RangeType rangeType)
    {
        switch(rangeType)
        {
            case Automatic:
                getWrapper().click(elementCache().visibleAutomaticRangeRadioButton);
                break;
            case AutomaticAcrossCharts:
                getWrapper().click(elementCache().visibleAutomaticAcrossRangeRadioButton);
                break;
            case AutomaticWithinChart:
                getWrapper().click(elementCache().visibleAutomaticWithinRangeRadioButton);
                break;
            case Manual:
                getWrapper().click(elementCache().visibleManualRangeRadioButton);
                break;
        }
    }

    public ChartLayoutDialog setXAxisScale(ScaleType scaleType)
    {
        clickXAxisTab();
        setScaleType(scaleType);
        return this;
    }

    public ChartLayoutDialog setXAxisRangeType(RangeType rangeType)
    {
        clickXAxisTab();
        setRangeType(rangeType);
        return this;
    }

    public ChartLayoutDialog setXAxisRangeMinMax(String min, String max)
    {
        clickXAxisTab();
        setRangeType(RangeType.Manual);
        setRangeMin(min);
        setRangeMax(max);
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

    public ChartLayoutDialog setYAxisRangeType(RangeType rangeType)
    {
        clickYAxisTab();
        setRangeType(rangeType);
        return this;
    }

    public ChartLayoutDialog setYAxisRangeMinMax(String min, String max)
    {
        clickYAxisTab();
        setRangeType(RangeType.Manual);
        setRangeMin(min);
        setRangeMax(max);
        return this;
    }

    protected void setLabel(String label)
    {
        getWrapper().setFormElement(elementCache().visibleLabelTextBox, label);
    }

    protected void setRangeMin(String min)
    {
        getWrapper().setFormElement(elementCache().visibleRangeMinTextBox, min);
    }

    protected void setRangeMax(String min)
    {
        getWrapper().setFormElement(elementCache().visibleRangeMaxTextBox, min);
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

    public ChartLayoutDialog clickResetTitle()
    {
        clickGeneralTab();
        elementCache().plotTitleResetBtn.click();
        return this;
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
        Locator.XPathLocator comboBox = Ext4Helper.Locators.formItemWithLabel(colorLabel);
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'x4-form-trigger')]");
        getWrapper().waitAndClick(arrowTrigger);

        String colorStr = hexColorValue.replace("#", "").toUpperCase();
        Locator colorPickerItem = Locator.tagWithClass("div", "chart-option-color-picker")
                .append(Locator.tagWithClass("a", "color-" + colorStr));
        getWrapper().waitAndClick(colorPickerItem);
    }

    protected void setColorPalette(String colorLabel, String colorPalette)
    {
        Locator.XPathLocator comboBox = Ext4Helper.Locators.formItemWithLabel(colorLabel);
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'x4-form-trigger')]");
        getWrapper().waitAndClick(arrowTrigger);
        String colorStr;

        if (colorPalette.toLowerCase().equals("dark"))
            colorStr = "Dark";
        else if (colorPalette.toLowerCase().equals("alternate"))
            colorStr = "Alternate";
        else
            colorStr = "Light (default)";

        Locator dropDownItem = Locator.tagWithClass("ul", "x4-list-plain")
                .append(Locator.tagWithText("li", colorStr));
        getWrapper().waitAndClick(dropDownItem);

    }

    public ChartLayoutDialog setBinThreshold(boolean alwaysBin)
    {
        clickGeneralTab();
        if (alwaysBin)
        {
            getWrapper().click(elementCache().alwaysBinThresholdRadioButton);
        }
        else {
            getWrapper().click(elementCache().onlyWhenExceedsBinThresholdRadioButton);
        }
        return this;
    }

    public ChartLayoutDialog setBinShape(BinShape shape)
    {
        clickGeneralTab();
        switch(shape)
        {
            case Hexagon:
                getWrapper().click(elementCache().hexagonShapeRadioButton);
                break;
            case Square:
                getWrapper().click(elementCache().squareShapeRadioButton);
                break;
        }
        return this;
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
        static private final int wait = 10000;

        public Locator developerTabLoc = Locator.xpath("//div[contains(@class, 'item')][text()='Developer']");
        public WebElement generalTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='General']"), this).withTimeout(wait);
        public WebElement xAxisTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='X-Axis']"), this).withTimeout(wait);
        public WebElement yAxisTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='Y-Axis']"), this).withTimeout(wait);
        public WebElement developerTab = new LazyWebElement(developerTabLoc, this).withTimeout(wait);
        public WebElement visiblePanel = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH), getWrapper().getWrappedDriver());
        public WebElement plotTitleTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Title:']/parent::td/following-sibling::td//input"), this);
        public WebElement plotTitleResetBtn = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//span[contains(@class, 'fa-refresh')]"), this);
        public WebElement plotWidthTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='width']"), this);
        public WebElement plotHeightTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='height']"), this);
        public WebElement lineWidthSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Line Width:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement plotBinThresholdTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='binThresholdField']"), this);
        public WebElement onlyWhenExceedsBinThresholdRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='When # of data points exceeds 10,000']/preceding-sibling::input[@type='button']"), this);
        public WebElement alwaysBinThresholdRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Always']/preceding-sibling::input[@type='button']"), this);
        public WebElement hexagonShapeRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Hexagon']/preceding-sibling::input[@type='button']"), this);
        public WebElement squareShapeRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Square']/preceding-sibling::input[@type='button']"), this);
        public WebElement developerEnable = new LazyWebElement(Locator.xpath("//span[text()='Enable']"), this);
        public WebElement developerDisable = new LazyWebElement(Locator.xpath("//span[text()='Disable']"), this);
        // Making these elements locators because it looks like once a WebElement creates a reference it doesn't re-evaluate the xpath. These three elements are shared on multiple tabs.
        public Locator visibleLinearScaleRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Linear']/preceding-sibling::input[@type='button']");
        public Locator visibleLogScaleRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Log']/preceding-sibling::input[@type='button']");
        public Locator visibleAutomaticRangeRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Automatic']/preceding-sibling::input[@type='button']");
        public Locator visibleAutomaticAcrossRangeRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Automatic Across Charts']/preceding-sibling::input[@type='button']");
        public Locator visibleAutomaticWithinRangeRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Automatic Within Chart']/preceding-sibling::input[@type='button']");
        public Locator visibleManualRangeRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Manual']/preceding-sibling::input[@type='button']");
        public Locator visibleLabelTextBox = Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='label']");
        public Locator visibleRangeMinTextBox = Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='rangeMin']");
        public Locator visibleRangeMaxTextBox = Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='rangeMax']");
    }

    public enum ScaleType
    {
        Linear,
        Log
    }

    public enum RangeType
    {
        Automatic,
        AutomaticAcrossCharts,
        AutomaticWithinChart,
        Manual
    }

    public enum BinShape
    {
        Hexagon,
        Square
    }
}
