package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartTypeDialog<EC extends Component.ElementCache> extends Component<EC>
{

    private  final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'chart-type-panel')]";

    protected WebElement _chartTypeDialog;
    protected BaseWebDriverTest _test;

    public ChartTypeDialog(BaseWebDriverTest test)
    {
        _test = test;
    }

    public boolean isDialogVisible()
    {
        return elements().dialog.isDisplayed();
    }

    public void waitForDialog()
    {
        _test.waitForElement(Locator.xpath(DIALOG_XPATH + "//div[text()='Create a plot']"));
    }

    public void setChartType(ChartType chartType)
    {
        switch(chartType)
        {
            case Bar:
                _test.click(elements().plotTypeBar);
                break;
            case Box:
                _test.click(elements().plotTypeBox);
                break;
            case Pie:
                _test.click(elements().plotTypePie);
                break;
            case Scatter:
                _test.click(elements().plotTypeScatter);
                break;
        }
    }

    public boolean isChartTypeEnabled(ChartType chartType)
    {
        boolean returnValue = false;
        String classValue = "";

        switch(chartType)
        {
            case Bar:
                classValue = elements().plotTypeBar.getAttribute("class");
                break;
            case Box:
                classValue = elements().plotTypeBox.getAttribute("class");
                break;
            case Pie:
                classValue = elements().plotTypePie.getAttribute("class");
                break;
            case Scatter:
                classValue = elements().plotTypeScatter.getAttribute("class");
                break;
            default:
                classValue = "-disabled";
        }

        if(classValue.contains("-disabled"))
            returnValue = false;
        else
            returnValue = true;

        return returnValue;
    }

    public ArrayList<String> getListOfChartTypes()
    {
        ArrayList<String> chartTypes = new ArrayList<>();
        List<WebElement> webElements = Locator.findElements(_test.getDriver(), Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'types-panel')]//div[contains(@id, 'chart-type')]//div"));

        for(WebElement we : webElements)
        {
            chartTypes.add(we.getText());
        }

        return chartTypes;
    }

    public void setXAxis(String columnName)
    {
        setAttribute(elements().xAxis, columnName);
    }

    public void setYAxis(String columnName)
    {
        setAttribute(elements().yAxis, columnName);
    }

    public void setColor(String columnName)
    {
        setAttribute(elements().color, columnName);
    }

    public void setShape(String columnName)
    {
        setAttribute(elements().shape, columnName);
    }

    private void setAttribute(WebElement attributeElement, String columnName)
    {
        _test.click(Locator.xpath("//div[text()='" + columnName + "']"));
        _test.click(attributeElement);
        _test.waitForFormElementToNotEqual(attributeElement.findElement(By.xpath("//div[@class='field-selection-display']")), columnName);
    }

    public void removeXAxis()
    {
        _test.mouseOver(elements().xAxis);
        removeAttribute(elements().xAxisRemove);
    }

    public void removeYAxis()
    {
        _test.mouseOver(elements().yAxis);
        removeAttribute(elements().yAxisRemove);
    }

    public void removeColor()
    {
        _test.mouseOver(elements().color);
        removeAttribute(elements().colorRemove);
    }

    public void removeShape()
    {
        _test.mouseOver(elements().shape);
        removeAttribute(elements().shapeRemove);
    }

    private void removeAttribute(WebElement attributeElement)
    {
        _test.click(attributeElement);
    }

    public String getXAxisValue()
    {
        return getAttributeValue(elements().xAxis);
    }

    public String getYAxisValue()
    {
        return getAttributeValue(elements().yAxis);
    }

    public String getColorValue()
    {
        return getAttributeValue(elements().color);
    }

    public String getShapeValue()
    {
        return getAttributeValue(elements().shape);
    }

    private String getAttributeValue(WebElement attributeElement)
    {
        String text;
        try
        {
            WebElement webElement = attributeElement.findElement(By.xpath("//div[@class='field-selection-dislay']"));
            text = webElement.getText();
        }
        catch(NoSuchElementException nse)
        {
            text = "";
        }
        return text;
    }

    public void clickApply()
    {
        clickApply(10000);
    }

    public void clickApply(int waitTime)
    {
        _test.clickButton("Apply", 0);
        _test.sleep(1000);
        _test._ext4Helper.waitForMaskToDisappear(waitTime);
    }

    public void clickCancel()
    {
        _test.clickButton("Cancel", 0);
    }

    public String getChartTypeTitle()
    {
        return elements().typeTitle.getText();
    }

    public ArrayList<String> getColumnList()
    {
        String rawText = elements().columnList.getText();
        String[] textArray = rawText.split("\n");
        ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(textArray));
        arrayList.remove(0); // Remove the first element in the list. This is the title "Columns".
        return arrayList;
    }

    public ArrayList<String> getListOfRequiredFields()
    {
        ArrayList<String> requiredFields = new ArrayList<>();
        List<WebElement> webElements = Locator.findElements(_test.getDriver(), Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')]"));
        String temp;

        for(WebElement we : webElements)
        {
            temp = we.getText();
            if((temp.contains("*")) || (temp.contains("(Required)")))
            {
                temp = temp.replace("*", "");
                temp = temp.replace("(Required)", "");
                temp = temp.trim();
                requiredFields.add(temp);
            }
        }

        return requiredFields;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _chartTypeDialog;
    }

    public Elements elements()
    {
        return new Elements();
    }

    class Elements extends ElementCache
    {

        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement typeTitle = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'type-title')]"), _test.getDriver());
        public WebElement plotTypeBar = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-bar_chart']"),  _test.getDriver());
        public WebElement plotTypeBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-box_chart']"),  _test.getDriver());
        public WebElement plotTypePie = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-pie_chart']"),  _test.getDriver());
        public WebElement plotTypeScatter = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-scatter_plot']"),  _test.getDriver());
        public WebElement xAxis = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'X Axis')]/following-sibling::div[contains(@class, 'field-area ')]"),  _test.getDriver());
        public WebElement xAxisRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'X Axis')]/following-sibling::div[contains(@class, 'field-area ')]//div[@class='field-selection-display']//div[contains(@class, 'field-selection-remove')]"),  _test.getDriver());
        public WebElement yAxis = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Y Axis')]/following-sibling::div[contains(@class, 'field-area ')]"),  _test.getDriver());
        public WebElement yAxisRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Y Axis')]/following-sibling::div[contains(@class, 'field-area ')]//div[@class='field-selection-display']//div[contains(@class, 'field-selection-remove')]"),  _test.getDriver());
        public WebElement color = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Color')]/following-sibling::div[contains(@class, 'field-area ')]"),  _test.getDriver());
        public WebElement colorRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Color')]/following-sibling::div[contains(@class, 'field-area ')]//div[@class='field-selection-display']//div[contains(@class, 'field-selection-remove')]"),  _test.getDriver());
        public WebElement shape = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Shape')]/following-sibling::div[contains(@class, 'field-area ')]"),  _test.getDriver());
        public WebElement shapeRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')][contains(text(), 'Shape')]/following-sibling::div[contains(@class, 'field-area ')]//div[@class='field-selection-display']//div[contains(@class, 'field-selection-remove')]"),  _test.getDriver());
        public WebElement columnList = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'mapping-query-col')]"), _test.getDriver());
        public WebElement fieldTitles = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')]"), _test.getDriver());
    }

    public enum ChartType
    {
        Bar,
        Box,
        Pie,
        Scatter
    }
}
