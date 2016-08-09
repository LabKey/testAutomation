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

        //If a wait time of -1 is given use this as a trigger to look for an (expected) error. If there is an error the mask won't disappear, so don't wait for it. Too much of a hack?
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
        private final String VISIBLE_PANEL = "//div[contains(@class, 'center-panel')]//div[contains(@class, 'x4-fit-item')][not(contains(@class, 'x4-item-disabled'))][not(contains(@style, 'display: none;'))]";

        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement generalTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='General']"), _test.getDriver());
        public WebElement xAxisTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='X-Axis']"), _test.getDriver());
        public WebElement yAxisTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='Y-Axis']"), _test.getDriver());
        public WebElement developerTab = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'item')][text()='Developer']"), _test.getDriver());
        public WebElement visiblePanel = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL), _test.getWrappedDriver());
        public WebElement visibleLinearScaleRadioButton = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL + "//label[text()='linear']/preceding-sibling::input[@type='button']"), _test.getDriver());
        public WebElement visibleLogScaleRadioButton = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL + "//label[text()='log']/preceding-sibling::input[@type='button']"), _test.getDriver());
        public WebElement visibleLabelTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL + "//input[@name='label']"), _test.getDriver());
        public WebElement plotTitleTextBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + VISIBLE_PANEL + "//td//label[text()='Title:']/parent::td/following-sibling::td//input"), _test.getDriver());
        public WebElement developerEnable = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//span[text()='Enable']"), _test.getDriver());
        public WebElement developerDisable = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//span[text()='Disable']"), _test.getDriver());
    }

    public enum ScaleType
    {
        Linear,
        Log
    }

}
