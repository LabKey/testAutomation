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

public class LookAndFeelPieChart extends ChartLayoutDialog<LookAndFeelPieChart.ElementCache>
{
    public LookAndFeelPieChart(WebDriver wDriver)
    {
        super(wDriver);
    }

    public LookAndFeelPieChart setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelPieChart setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelPieChart setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }

    public LookAndFeelPieChart setSubTitle(String subTitle)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().subTitleTextBox, subTitle);
        return this;
    }

    public String getSubTitle()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().subTitleTextBox);
    }

    public LookAndFeelPieChart setFooter(String footer)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().footerTextBox, footer);
        return this;
    }

    public String getFooter()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().footerTextBox);
    }

    public LookAndFeelPieChart setInnerRadiusPercentage(int radiusValue)
    {
        setSliderValue(elementCache().innerRadiusSlider, radiusValue);
        return this;
    }

    public int getInnerRadiusPercentage()
    {
        return getSliderCurrentValue(elementCache().innerRadiusSlider);
    }

    public LookAndFeelPieChart setOuterRadiusPercentage(int radiusValue)
    {
        setSliderValue(elementCache().outerRadiusSlider, radiusValue);
        return this;
    }

    public int getOuterRadiusPercentage()
    {
        return getSliderCurrentValue(elementCache().outerRadiusSlider);
    }

    public LookAndFeelPieChart setColorPalette(String paletteValue)
    {
        //getWrapper()._ext4Helper.selectComboBoxItem("Color palette:", paletteValue.dropDownText());
        setColorPalette("Color Palette:", paletteValue);
        return this;
    }

    public String getColorPalette()
    {
        return elementCache().colorPalette.getAttribute("value");
    }

    public LookAndFeelPieChart clickShowPercentages()
    {
        getWrapper().click(elementCache().showPercentagesCheckbox);
        return this;
    }

    public boolean showPercentagesChecked()
    {
        String classValue = elementCache().showPercentagesCheckboxValue.getAttribute("class");
        return classValue.toLowerCase().contains("x4-form-cb-checked");
    }

    public LookAndFeelPieChart setHidePercentageWhen(String width)
    {
        getWrapper().setFormElement(elementCache().hidePercentTextBox, width);
        return this;
    }

    public String getHidePercentageWhen()
    {
        return getWrapper().getFormElement(elementCache().hidePercentTextBox);
    }

    public LookAndFeelPieChart setPercentagesColor(String hexColorValue)
    {
        setColor("% Text Color:", hexColorValue);
        return this;
    }

    public LookAndFeelPieChart setGradientPercentage(int radiusValue)
    {
        setSliderValue(elementCache().gradientSlider, radiusValue);
        return this;
    }

    public int getGradientPercentage()
    {
        return getSliderCurrentValue(elementCache().gradientSlider);
    }

    public LookAndFeelPieChart setGradientColor(String hexColorValue)
    {
        setColor("Gradient Color:", hexColorValue);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends ChartLayoutDialog.ElementCache
    {
        public WebElement colorPalette = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='colorPaletteScale']"), this);
        public WebElement footerTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='footer']"), this);
        public WebElement gradientSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Gradient %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement hidePercentTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='pieHideWhenLessThanPercentage']"), this);
        public WebElement innerRadiusSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Inner Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement outerRadiusSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Outer Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement showPercentagesCheckbox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/parent::td/following-sibling::td//input"), this);
        public WebElement showPercentagesCheckboxValue = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/ancestor::table"), this);
        public WebElement subTitleTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='subtitle']"), this);
    }

    public enum ColorPalette
    {
        Light("Light (default)"),
        Dark("Dark"),
        Alternate("Alternate");

        private String _dropDownText;
        ColorPalette(String value)
        {
            _dropDownText = value;
        }

        public String dropDownText()
        {
            return _dropDownText;
        }
    }
}
