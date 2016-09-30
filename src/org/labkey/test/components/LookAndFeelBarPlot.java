package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelBarPlot extends ChartLayoutDialog<LookAndFeelBarPlot.ElementCache>
{

    public LookAndFeelBarPlot(WebDriver wDriver)
    {
        super(wDriver);
    }

    public LookAndFeelBarPlot setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelBarPlot setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelBarPlot setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }

    public LookAndFeelBarPlot setLineWidth(int lineWidth)
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

    public LookAndFeelBarPlot setOpacity(int lineWidth)
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

    public LookAndFeelBarPlot setLineColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Line Color:", hexColorValue);
        return this;
    }

    public LookAndFeelBarPlot setFillColor(String hexColorValue)
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
        public WebElement lineWidthSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Line Width:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement opacitySlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Opacity:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
    }
}
