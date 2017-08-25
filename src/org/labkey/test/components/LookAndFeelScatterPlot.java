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

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelScatterPlot extends ChartLayoutDialog<LookAndFeelScatterPlot.ElementCache>
{
    public LookAndFeelScatterPlot(WebDriver wDriver)
    {
        super(wDriver);
    }

    @Override
    protected LookAndFeelScatterPlot.ElementCache newElementCache()
    {
        return new LookAndFeelScatterPlot.ElementCache();
    }

    public LookAndFeelScatterPlot setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelScatterPlot setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelScatterPlot setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }
    public LookAndFeelScatterPlot clickGeneralTab()
    {
        super.clickGeneralTab();
        return this;
    }

    public LookAndFeelScatterPlot clickXAxisTab()
    {
        super.clickXAxisTab();
        return this;
    }

    public LookAndFeelScatterPlot setXAxisLabel(String label)
    {
        super.setXAxisLabel(label);
        return this;
    }

    public LookAndFeelScatterPlot setXAxisScale(ScaleType scaleType)
    {
        super.setXAxisScale(scaleType);
        return this;
    }

    public LookAndFeelScatterPlot setXAxisRangeType(RangeType rangeType)
    {
        super.setXAxisRangeType(rangeType);
        return this;
    }

    public LookAndFeelScatterPlot setXAxisRangeMinMax(String min, String max)
    {
        super.setXAxisRangeMinMax(min, max);
        return this;
    }

    public LookAndFeelScatterPlot clickYAxisTab()
    {
        super.clickYAxisTab();
        return this;
    }

    public LookAndFeelScatterPlot setYAxisLabel(String label)
    {
        super.setYAxisLabel(label);
        return this;
    }

    public LookAndFeelScatterPlot setYAxisScale(ScaleType scaleType)
    {
        super.setYAxisScale(scaleType);
        return this;
    }

    public LookAndFeelScatterPlot setYAxisRangeType(RangeType rangeType)
    {
        super.setYAxisRangeType(rangeType);
        return this;
    }

    public LookAndFeelScatterPlot setYAxisRangeMinMax(String min, String max)
    {
        super.setYAxisRangeMinMax(min, max);
        return this;
    }

    public LookAndFeelScatterPlot setOpacity(int lineWidth)
    {
        clickGeneralTab();
        setSliderValue(elementCache().opacitySlider, lineWidth);
        return this;
    }

    public int getOpacity()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().opacitySlider);
    }

    public LookAndFeelScatterPlot setPointSize(int lineWidth)
    {
        clickGeneralTab();
        setSliderValue(elementCache().pointSizeSlider, lineWidth);
        return this;
    }

    public int getPointSize()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().pointSizeSlider);
    }

    public LookAndFeelScatterPlot clickJitterPoints()
    {
        clickGeneralTab();
        getWrapper().click(elementCache().jitterPointsCheckbox);
        return this;
    }

    public boolean jitterPointsChecked()
    {
        String classValue;

        classValue = elementCache().jitterPointsCheckboxValue.getAttribute("class");

        if(classValue.toLowerCase().contains("x4-form-cb-checked"))
            return true;
        else
            return false;
    }

    public LookAndFeelScatterPlot setPointColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Point Color:", hexColorValue);
        return this;
    }

    public LookAndFeelScatterPlot setBinThresholdToAlways(boolean alwaysBin)
    {
        super.setBinThreshold(alwaysBin);
        return this;
    }

    public LookAndFeelScatterPlot setBinShape(BinShape shape)
    {
        super.setBinShape(shape);
        return this;
    }

    public LookAndFeelScatterPlot setPointColorPalette(String colorPalette)
    {
        clickGeneralTab();
        setColorPalette("Color Palette:", colorPalette);
        return this;
    }

    class ElementCache extends ChartLayoutDialog.ElementCache
    {
        public WebElement jitterPointsCheckbox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Jitter Points:']/parent::td/following-sibling::td//input"), this);
        public WebElement jitterPointsCheckboxValue = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Jitter Points:']/ancestor::table"), this);
        public WebElement opacitySlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Opacity:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement pointSizeSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Point Size:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
    }

}
