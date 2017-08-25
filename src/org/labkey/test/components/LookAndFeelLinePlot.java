package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelLinePlot extends ChartLayoutDialog<LookAndFeelLinePlot.ElementCache>
{
    public LookAndFeelLinePlot(WebDriver wDriver)
    {
        super(wDriver);
    }

    @Override
    protected LookAndFeelLinePlot.ElementCache newElementCache()
    {
        return new LookAndFeelLinePlot.ElementCache();
    }

    public LookAndFeelLinePlot setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelLinePlot setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelLinePlot setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }
    public LookAndFeelLinePlot clickGeneralTab()
    {
        super.clickGeneralTab();
        return this;
    }

    public LookAndFeelLinePlot clickXAxisTab()
    {
        super.clickXAxisTab();
        return this;
    }

    public LookAndFeelLinePlot setXAxisLabel(String label)
    {
        super.setXAxisLabel(label);
        return this;
    }

    public LookAndFeelLinePlot setXAxisScale(ChartLayoutDialog.ScaleType scaleType)
    {
        super.setXAxisScale(scaleType);
        return this;
    }

    public LookAndFeelLinePlot setXAxisRangeType(ChartLayoutDialog.RangeType rangeType)
    {
        super.setXAxisRangeType(rangeType);
        return this;
    }

    public LookAndFeelLinePlot setXAxisRangeMinMax(String min, String max)
    {
        super.setXAxisRangeMinMax(min, max);
        return this;
    }

    public LookAndFeelLinePlot clickYAxisTab()
    {
        super.clickYAxisTab();
        return this;
    }

    public LookAndFeelLinePlot setYAxisLabel(String label)
    {
        super.setYAxisLabel(label);
        return this;
    }

    public LookAndFeelLinePlot setYAxisScale(ChartLayoutDialog.ScaleType scaleType)
    {
        super.setYAxisScale(scaleType);
        return this;
    }

    public LookAndFeelLinePlot setYAxisRangeType(ChartLayoutDialog.RangeType rangeType)
    {
        super.setYAxisRangeType(rangeType);
        return this;
    }

    public LookAndFeelLinePlot setYAxisRangeMinMax(String min, String max)
    {
        super.setYAxisRangeMinMax(min, max);
        return this;
    }

    public LookAndFeelLinePlot setOpacity(int lineWidth)
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

    public LookAndFeelLinePlot setLineWidth(int lineWidth)
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

    public LookAndFeelLinePlot setPointSize(int lineWidth)
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

    public LookAndFeelLinePlot setPointColor(String hexColorValue)
    {
        clickGeneralTab();
        setColor("Point Color:", hexColorValue);
        return this;
    }

    public LookAndFeelLinePlot setBinShape(ChartLayoutDialog.BinShape shape)
    {
        super.setBinShape(shape);
        return this;
    }

    class ElementCache extends ChartLayoutDialog.ElementCache
    {
        public WebElement opacitySlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Opacity:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement pointSizeSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Point Size:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement lineWidthSlider = new LazyWebElement(Locator.xpath(ElementCache.VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Line Width:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
    }

}
