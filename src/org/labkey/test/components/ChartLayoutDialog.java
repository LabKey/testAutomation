package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class ChartLayoutDialog<EC extends Component.ElementCache> extends Component<EC>
{
    private  final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'chart-layout-panel')]";

    protected WebElement _chartLayoutDialog;
    protected BaseWebDriverTest _test;

    public ChartLayoutDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _chartLayoutDialog;
    }

    public boolean isDialogVisible()
    {
        return elements().dialog.isDisplayed();
    }

    public void waitForDialog()
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//div[text()='Customize look and feel']"));
    }

    public ArrayList<String> getAvailableTabs()
    {
        ArrayList<String> tabs = new ArrayList<>();
        List<WebElement> elements = Locator.findElements(_test.getDriver(), Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'navigation-panel')]//div[contains(@class, 'item')]"));
        for(WebElement element : elements)
        {
            tabs.add(element.getText());
        }

        return tabs;
    }

    public void clickGeneralTab()
    {
        clickTab(elements().generalTab);
    }

    public void clickXAxisTab()
    {
        clickTab(elements().xAxisTab);
    }

    public void clickYAxisTab()
    {
        clickTab(elements().yAxisTab);
    }

    public void clickDeveloperTab()
    {
        clickTab(elements().developerTab);
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
                elements().visibleLinearScaleRadioButton.click();
                break;
            case Log:
                _test.log("tag class info: " + elements().visibleLogScaleRadioButton.getAttribute("class"));
                elements().visibleLogScaleRadioButton.click();
                break;
        }

    }

    public void setXAxisLabel(String label)
    {
        clickXAxisTab();
        setLabel(label);
    }

    public void setYAxisLabel(String label)
    {
        clickYAxisTab();
        setLabel(label);
    }

    private void setLabel(String label)
    {
        _test.setFormElement(elements().visibleLabelTextBox, label);
    }

    public void setPlotTitle(String title)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotTitleTextBox, title);
    }

    public String getPlotTitle()
    {
        clickGeneralTab();
        return _test.getFormElement(elements().plotTitleTextBox);
    }

    public void setPlotSubTitle(String subTitle)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotSubTitleTextBox, subTitle);
    }

    public String getPlotSubTitle()
    {
        clickGeneralTab();
        return _test.getFormElement(elements().plotSubTitleTextBox);
    }

    public void setPlotFooter(String footer)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotFooterTextBox, footer);
    }

    public String getPlotFooter()
    {
        clickGeneralTab();
        return _test.getFormElement(elements().plotFooterTextBox);
    }

    public void setPlotWidth(int width)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotWidthTextBox, String.valueOf(width));
    }

    public int getPlotWidth()
    {
        clickGeneralTab();
        return Integer.parseInt(_test.getFormElement(elements().plotWidthTextBox));
    }

    public void setPlotHeight(int height)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotHeightTextBox, String.valueOf(height));
    }

    public int getPlotHeight()
    {
        clickGeneralTab();
        return Integer.parseInt(_test.getFormElement(elements().plotHeightTextBox));
    }

    public void setHidePercentageWhen(String width)
    {
        clickGeneralTab();
        _test.setFormElement(elements().plotHidePercentTextBox, width);
    }

    public String getHidePercentageWhen()
    {
        clickGeneralTab();
        return _test.getFormElement(elements().plotHidePercentTextBox);
    }

    public void clickShowPercentages()
    {
        clickGeneralTab();
        _test.click(elements().showPercentagesCheckbox);
    }

    public boolean showPercentagesChecked()
    {
        String classValue;

        clickGeneralTab();

        classValue = elements().showPercentagesCheckboxValue.getAttribute("class");

        if(classValue.toLowerCase().contains("x4-form-cb-checked"))
            return true;
        else
            return false;
    }

    public void setGradientColor(String hexColorValue)
    {
        String tempStr = hexColorValue.replace("#", "").toUpperCase();
        _test.click(Locator.xpath(elements().VISIBLE_PANEL_XPATH + "//label[text() = 'Gradient Color:']/following-sibling::div[not(contains(@class, 'x4-item-disabled'))]/a[contains(@class, '" + tempStr + "')]"));
    }

    public void setPercentagesColor(String hexColorValue)
    {
        String tempStr = hexColorValue.replace("#", "").toUpperCase();
        _test.click(Locator.xpath(elements().VISIBLE_PANEL_XPATH + "//label[text() = 'Percentages Color:']/following-sibling::div[not(contains(@class, 'x4-item-disabled'))]/a[contains(@class, '" + tempStr + "')]"));
    }

    public void setInnerRadiusPercentage(int radiusValue)
    {
        setSliderValue(elements().innerRadiusSlider, elements().innerRadiusSliderValue, radiusValue);
    }

    public int getInnerRadiusPercentage()
    {
        return Integer.parseInt(elements().innerRadiusSliderValue.getAttribute("aria-valuenow"));
    }

    public void setOuterRadiusPercentage(int radiusValue)
    {
        setSliderValue(elements().outerRadiusSlider, elements().outerRadiusSliderValue, radiusValue);
    }

    public int getOuterRadiusPercentage()
    {
        return Integer.parseInt(elements().outerRadiusSliderValue.getAttribute("aria-valuenow"));
    }

    public void setGradientPercentage(int radiusValue)
    {
        setSliderValue(elements().gradientSlider, elements().gradientSliderValue, radiusValue);
    }

    public int getGradientPercentage()
    {
        return Integer.parseInt(elements().gradientSliderValue.getAttribute("aria-valuenow"));
    }

    private void setSliderValue(WebElement slider, WebElement sliderValue, int percentValue)
    {
        // An alternative to actually moving the slider might be to have some javascript code that sets the aria-valuenow attribute of the element.

        if(percentValue > 100)
            percentValue = 100;
        if(percentValue < 0)
            percentValue = 0;


        int currentRadiusValue = getSliderCurrentPercent(sliderValue);
        int xChangeAmount;

        if(currentRadiusValue == percentValue)
            return;

        if(currentRadiusValue > percentValue)
        {
            xChangeAmount = -10;
            while(getSliderCurrentPercent(sliderValue) > percentValue)
                _test.dragAndDrop(slider, xChangeAmount, 0);
        }
        else
        {
            xChangeAmount = 10;
            while(getSliderCurrentPercent(sliderValue) < percentValue)
                _test.dragAndDrop(slider, xChangeAmount, 0);
        }

    }

    private int getSliderCurrentPercent(WebElement sliderValue)
    {
        return Integer.parseInt(sliderValue.getAttribute("aria-valuenow"));
    }

    public void setColorPalette(ColorPalette paletteValue)
    {
        _test._ext4Helper.selectComboBoxItem("Color palette:", paletteValue.dropDownText());
    }

    public String getColorPalette()
    {
        return elements().colorPalette.getAttribute("value");
    }

    public void clickDeveloperEnable()
    {
        clickDeveloperTab();
        _test.clickButton("Enable", 0);
        _test.waitForText("// use LABKEY.ActionURL.buildURL to generate a link");
    }

    public void clickDeveloperDisable(boolean clickYes)
    {
        clickDeveloperTab();
        _test.clickButton("Disable", 0);
        _test._extHelper.waitForExtDialog("Confirmation...");

        if(clickYes)
        {
            _test.clickButton("Yes", 0);
        }
        else
        {
            _test.clickButton("No", 0);
        }

    }

    public void clickDeveloperSourceTab()
    {
        clickDeveloperTab();
        _test._ext4Helper.clickTabContainingText("Source");
    }

    public String getDeveloperSourceContent()
    {
        clickDeveloperSourceTab();
        return _test._extHelper.getCodeMirrorValue("point-click-fn-textarea");
    }

    public void setDeveloperSourceContent(String source)
    {
        clickDeveloperSourceTab();
        _test._extHelper.setCodeMirrorValue("point-click-fn-textarea", source);
    }

    public void clickDeveloperHelpTab()
    {
        clickDeveloperTab();
        _test._ext4Helper.clickTabContainingText("Help");
    }

    public String getDeveloperHelpContent()
    {
        clickDeveloperHelpTab();
        return _test._extHelper.getCodeMirrorValue("point-click-fn-textarea");
    }

    public void clickApply()
    {
        clickApply(10000);
    }

    public void clickApply(int waitTime)
    {
        _test.clickButton("Apply", 0);
        _test.sleep(1000);

        //If a wait time of -1 is given use this as a trigger to look for an (expected) error. For example not setting a required field for the plot.
        // This is needed because if there is an error the mask doesn't disappear so the waitForMaskToDisappear would error out. Too much of a hack?
        if(waitTime != -1)
        {
            _test._ext4Helper.waitForMaskToDisappear(waitTime);
        }

    }

    public void clickCancel()
    {
        _test.clickButton("Cancel", 0);
    }

    public Elements elements()
    {
        return new Elements();
    }

    class Elements extends ElementCache
    {
        public final String VISIBLE_PANEL_XPATH = "//div[contains(@class, 'center-panel')]//div[contains(@class, 'x4-fit-item')][not(contains(@class, 'x4-item-disabled'))][not(contains(@style, 'display: none;'))]";

        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement generalTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='General']"), _test.getDriver());
        public WebElement xAxisTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='X-Axis']"), _test.getDriver());
        public WebElement yAxisTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='Y-Axis']"), _test.getDriver());
        public WebElement developerTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='Developer']"), _test.getDriver());
        public WebElement visiblePanel = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH), _test.getWrappedDriver());
        public WebElement visibleLinearScaleRadioButton = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//label[text()='linear']/preceding-sibling::input[@type='button']"), _test.getDriver());
        public WebElement visibleLogScaleRadioButton = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//label[text()='log']/preceding-sibling::input[@type='button']"), _test.getDriver());
        public WebElement visibleLabelTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='label']"), _test.getDriver());
        public WebElement plotTitleTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//td//label[text()='Title:']/parent::td/following-sibling::td//input"), _test.getDriver());
        public WebElement plotSubTitleTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='subtitle']"), _test.getDriver());
        public WebElement plotFooterTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='footer']"), _test.getDriver());
        public WebElement plotWidthTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='width']"), _test.getDriver());
        public WebElement plotHeightTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='height']"), _test.getDriver());
        public WebElement plotHidePercentTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='pieHideWhenLessThanPercentage']"), _test.getDriver());
        public WebElement showPercentagesCheckbox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/parent::td/following-sibling::td//input"), _test.getDriver());
        public WebElement showPercentagesCheckboxValue = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//td//label[text()='Show Percentages:']/ancestor::table"), _test.getDriver());
        public WebElement innerRadiusSlider = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Inner Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-thumb')]"), _test.getDriver());
        public WebElement innerRadiusSliderValue = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Inner Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), _test.getDriver());
        public WebElement outerRadiusSlider = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Outer Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-thumb')]"), _test.getDriver());
        public WebElement outerRadiusSliderValue = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Outer Radius %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), _test.getDriver());
        public WebElement gradientSlider = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Gradient %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-thumb')]"), _test.getDriver());
        public WebElement gradientSliderValue = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//table[not(contains(@class, 'x4-item-disabled'))]//label[text()='Gradient %:']/parent::td/following-sibling::td//div[contains(@class, 'x4-slider-horz')]"), _test.getDriver());
        public WebElement colorPalette = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL_XPATH + "//input[@name='colorPaletteScale']"), _test.getDriver());
        public WebElement developerEnable = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//span[text()='Enable']"), _test.getDriver());
        public WebElement developerDisable = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//span[text()='Disable']"), _test.getDriver());
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
