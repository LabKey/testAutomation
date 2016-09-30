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

    public LookAndFeelBoxPlot setOpacity(int lineWidth)
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
        public WebElement lineWidthSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Line Width:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement opacitySlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Opacity:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
    }
}
