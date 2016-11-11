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

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelBoxPlot extends ChartLayoutDialog<LookAndFeelBoxPlot.ElementCache>
{

    public LookAndFeelBoxPlot(WebDriver wDriver)
    {
        super(wDriver);
    }

    public LookAndFeelBoxPlot setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelBoxPlot setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelBoxPlot setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }
    public LookAndFeelBoxPlot clickGeneralTab()
    {
        super.clickGeneralTab();
        return this;
    }

    public LookAndFeelBoxPlot clickXAxisTab()
    {
        super.clickXAxisTab();
        return this;
    }

    public LookAndFeelBoxPlot clickYAxisTab()
    {
        super.clickYAxisTab();
        return this;
    }

    public LookAndFeelBoxPlot setXAxisLabel(String label)
    {
        super.setXAxisLabel(label);
        return this;
    }

    public LookAndFeelBoxPlot setXAxisScale(ScaleType scaleType)
    {
        super.setXAxisScale(scaleType);
        return this;
    }

    public LookAndFeelBoxPlot setYAxisLabel(String label)
    {
        super.setYAxisLabel(label);
        return this;
    }

    public LookAndFeelBoxPlot setYAxisScale(ScaleType scaleType)
    {
        super.setYAxisScale(scaleType);
        return this;
    }

    public LookAndFeelBoxPlot setLineWidth(int lineWidth)
    {
        clickGeneralTab();
        setSliderValue(elementCache().lineWidthSlider, lineWidth);
        return this;
    }

    public int getLineWidth()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().lineWidthSlider);
    }

    public LookAndFeelBoxPlot setOpacity(int opacity)
    {
        clickGeneralTab();
        setSliderValue(elementCache().opacitySlider, opacity);
        return this;
    }

    public int getOpacity()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().opacitySlider);
    }

    public LookAndFeelBoxPlot setPointSize(int pointSize)
    {
        clickGeneralTab();
        setSliderValue(elementCache().pointSizeSlider, pointSize);
        return this;
    }

    public int getPointSize()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().pointSizeSlider);
    }

    public LookAndFeelBoxPlot clickJitterPoints()
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

    public LookAndFeelBoxPlot setPointColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Point Color:", hexColorValue);
        return this;
    }

    public LookAndFeelBoxPlot setLineColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Line Color:", hexColorValue);
        return this;
    }

    public LookAndFeelBoxPlot setFillColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Fill Color:", hexColorValue);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }
    
    class ElementCache extends ChartLayoutDialog.ElementCache
    {
        public WebElement jitterPointsCheckbox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Jitter Points:']/parent::td/following-sibling::td//input"), this);
        public WebElement jitterPointsCheckboxValue = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Jitter Points:']/ancestor::table"), this);
        public WebElement opacitySlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Opacity:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement pointSizeSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Point Size:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
    }
}
