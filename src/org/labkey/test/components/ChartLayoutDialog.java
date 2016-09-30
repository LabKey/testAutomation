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

    public void clickGeneralTab()
    {
        clickTab(elementCache().generalTab);
    }

    public void clickXAxisTab()
    {
        clickTab(elementCache().xAxisTab);
    }

    public void clickYAxisTab()
    {
        clickTab(elementCache().yAxisTab);
    }

    public void clickDeveloperTab()
    {
        clickTab(elementCache().developerTab);
    }

    private void clickTab(WebElement tabElement)
    {
        tabElement.click();
    }

    public void setScaleType(ScaleType scaleType)
    {
        switch(scaleType)
        {
            case Linear:
                elementCache().visibleLinearScaleRadioButton.click();
                break;
            case Log:
                getWrapper().log("tag class info: " + elementCache().visibleLogScaleRadioButton.getAttribute("class"));
                elementCache().visibleLogScaleRadioButton.click();
                break;
        }
    }

    public void setXAxisLabel(String label)
    {
        clickXAxisTab();
        setLabel(label);
    }

    public String getXAxisLabel()
    {
        clickXAxisTab();
        return getWrapper().getFormElement(elementCache().visibleLabelTextBox);
    }

    public void setYAxisLabel(String label)
    {
        clickYAxisTab();
        setLabel(label);
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

    public void setPlotSubTitle(String subTitle)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotSubTitleTextBox, subTitle);
    }

    public String getPlotSubTitle()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().plotSubTitleTextBox);
    }

    public void setPlotFooter(String footer)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotFooterTextBox, footer);
    }

    public String getPlotFooter()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().plotFooterTextBox);
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

    public void setHidePercentageWhen(String width)
    {
        clickGeneralTab();
        getWrapper().setFormElement(elementCache().plotHidePercentTextBox, width);
    }

    public String getHidePercentageWhen()
    {
        clickGeneralTab();
        return getWrapper().getFormElement(elementCache().plotHidePercentTextBox);
    }

    public void clickShowPercentages()
    {
        clickGeneralTab();
        getWrapper().click(elementCache().showPercentagesCheckbox);
    }

    public boolean showPercentagesChecked()
    {
        String classValue;

        clickGeneralTab();

        classValue = elementCache().showPercentagesCheckboxValue.getAttribute("class");

        if(classValue.toLowerCase().contains("x4-form-cb-checked"))
            return true;
        else
            return false;
    }

    public void setGradientColor(String hexColorValue)
    {
        setColor("Gradient Color:", hexColorValue);
    }

    public void setPercentagesColor(String hexColorValue)
    {
        setColor("Percentages Color:", hexColorValue);
    }

    protected void setColor(String colorLabel, String hexColorValue)
    {
        String tempStr = hexColorValue.replace("#", "").toUpperCase();
        getWrapper().click(Locator.xpath(elementCache().VISIBLE_PANEL_XPATH + "//label[text() = '" + colorLabel + "']/following-sibling::div[not(contains(@class, 'x4-item-disabled'))]/a[contains(@class, '" + tempStr + "')]"));
    }

    public void setInnerRadiusPercentage(int radiusValue)
    {
        clickGeneralTab();
        setSliderValue(elementCache().innerRadiusSlider, radiusValue);
    }

    public int getInnerRadiusPercentage()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().innerRadiusSlider);
    }

    public void setOuterRadiusPercentage(int radiusValue)
    {
        clickGeneralTab();
        setSliderValue(elementCache().outerRadiusSlider, radiusValue);
    }

    public int getOuterRadiusPercentage()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().outerRadiusSlider);
    }

    public void setGradientPercentage(int radiusValue)
    {
        clickGeneralTab();
        setSliderValue(elementCache().gradientSlider, radiusValue);
    }

    public int getGradientPercentage()
    {
        clickGeneralTab();
        return getSliderCurrentValue(elementCache().gradientSlider);
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

    public void setColorPalette(ColorPalette paletteValue)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Color palette:", paletteValue.dropDownText());
    }

    public String getColorPalette()
    {
        return elementCache().colorPalette.getAttribute("value");
    }

    public void clickDeveloperEnable()
    {
        clickDeveloperTab();
        getWrapper().clickButton("Enable", 0);
        getWrapper().waitForText("// use LABKEY.ActionURL.buildURL to generate a link");
    }

    public void clickDeveloperDisable(boolean clickYes)
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

    }

    public void clickDeveloperSourceTab()
    {
        clickDeveloperTab();
        getWrapper()._ext4Helper.clickTabContainingText("Source");
    }

    public String getDeveloperSourceContent()
    {
        clickDeveloperSourceTab();
        return getWrapper()._extHelper.getCodeMirrorValue("point-click-fn-textarea");
    }

    public void setDeveloperSourceContent(String source)
    {
        clickDeveloperSourceTab();
        getWrapper()._extHelper.setCodeMirrorValue("point-click-fn-textarea", source);
    }

    public void clickDeveloperHelpTab()
    {
        clickDeveloperTab();
        getWrapper()._ext4Helper.clickTabContainingText("Help");
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

        //If a wait time of -1 is given use this as a trigger to look for an (expected) error. For example not setting a required field for the plot.
        // This is needed because if there is an error the mask doesn't disappear so the waitForMaskToDisappear would error out. Too much of a hack?
        if(waitTime != -1)
        {
            getWrapper()._ext4Helper.waitForMaskToDisappear(waitTime);
        }

    }

    public void clickApplyWithError()
    {
        getWrapper().clickButton("Apply", 0);
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
        public WebElement visibleLinearScaleRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='linear']/preceding-sibling::input[@type='button']"), this);
        public WebElement visibleLogScaleRadioButton = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='log']/preceding-sibling::input[@type='button']"), this);
        public WebElement visibleLabelTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='label']"), this);
        public WebElement plotTitleTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Title:']/parent::td/following-sibling::td//input"), this);
        public WebElement plotSubTitleTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='subtitle']"), this);
        public WebElement plotFooterTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='footer']"), this);
        public WebElement plotWidthTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='width']"), this);
        public WebElement plotHeightTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='height']"), this);
        public WebElement plotHidePercentTextBox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='pieHideWhenLessThanPercentage']"), this);
        public WebElement showPercentagesCheckbox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/parent::td/following-sibling::td//input"), this);
        public WebElement showPercentagesCheckboxValue = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/ancestor::table"), this);
        public WebElement innerRadiusSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Inner Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement outerRadiusSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Outer Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement gradientSlider = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Gradient %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), this);
        public WebElement colorPalette = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//input[@name='colorPaletteScale']"), this);
        public WebElement developerEnable = new LazyWebElement(Locator.xpath("//span[text()='Enable']"), this);
        public WebElement developerDisable = new LazyWebElement(Locator.xpath("//span[text()='Disable']"), this);
    }

    public enum ScaleType
    {
        Linear,
        Log
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
