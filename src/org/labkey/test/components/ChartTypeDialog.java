package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartTypeDialog<EC extends Component.ElementCache> extends Component<EC>
{

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
        _test.waitForElement(Locator.xpath(elements().DIALOG_XPATH + "//div[text()='Create a plot']"));
        _test.waitForElement(Locator.xpath(elements().DIALOG_XPATH + "//a[contains(@class, 'x4-btn')]//span[text()='Apply']"));
        _test.sleep(500);
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
        List<WebElement> webElements = Locator.findElements(_test.getDriver(), Locator.xpath(elements().DIALOG_XPATH + "//div[contains(@class, 'types-panel')]//div[contains(@id, 'chart-type')]//div"));

        for(WebElement we : webElements)
        {
            chartTypes.add(we.getText());
        }

        return chartTypes;
    }

    public void setXAxis(String columnName)
    {
        setXAxis(columnName, false);
    }

    public void setXAxis(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().xAxis, columnName);
        else
            setValue(elements().xAxisDropText, columnName);
    }

    public void setYAxis(String columnName)
    {
        setYAxis(columnName, false);
    }

    public void setYAxis(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().yAxis, columnName);
        else
            setValue(elements().yAxisDropText, columnName);
    }

    public void setCategories(String columnName)
    {
        setCategories(columnName, false);
    }

    public void setCategories(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().categories, columnName);
        else
            setValue(elements().categoriesDropText, columnName);
    }

    public String getCategories()
    {
        String value;

        try
        {
            return elements().categoriesDisplay.getText();
        }
            catch(org.openqa.selenium.NoSuchElementException nse)
        {
            value = "";
        }

        return value;
    }

    public void setMeasure(String columnName)
    {
        setMeasure(columnName, false);
    }

    public void setMeasure(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().measure, columnName);
        else
            setValue(elements().measureDropText, columnName);
    }

    public String getMeasure()
    {
        String value;

        try
        {
            value = elements().measureDisplay.getText();
        }
        catch(org.openqa.selenium.NoSuchElementException nse)
        {
            value = "";
        }

        return value;
    }

    public void setColor(String columnName)
    {
        setColor(columnName, false);
    }

    public void setColor(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().color, columnName);
        else
            setValue(elements().colorDropText, columnName);
    }

    public void setShape(String columnName)
    {
        setShape(columnName, false);
    }

    public void setShape(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elements().shape, columnName);
        else
            setValue(elements().shapeDropText, columnName);
    }

    private void setValue(WebElement element, String columnName)
    {
        _test.click(Locator.xpath("//div[text()='" + columnName + "']"));
        _test.click(element);
        _test.waitForFormElementToNotEqual(element.findElement(By.xpath("//div[@class='field-selection-display']")), columnName);
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

    private void removeAttribute(WebElement element)
    {
        _test.click(element);
    }

    public String getXAxisValue()
    {
        return getFieldValue(elements().xAxis, elements().xAxisDropText);
    }

    public String getYAxisValue()
    {
        return getFieldValue(elements().yAxis, elements().yAxisDropText);
    }

    public String getColorValue()
    {
        return getFieldValue(elements().color, elements().colorDropText);
    }

    public String getShapeValue()
    {
        return getFieldValue(elements().shape, elements().shapeDropText);
    }

    // This could be cleaned up to take only one parameter and then try to find the drag and drop element from it.
    // But since this is a private function I am doing it the quick and dirty way.
    private String getFieldValue(WebElement fieldElement, WebElement dragAndDropElement)
    {
        String text;
        text = fieldElement.getText();

        if(text.length() == 0)
        {
            // If the lenght is 0 see if the drag and drop text is visible.
            if(dragAndDropElement.isDisplayed())
                text = dragAndDropElement.getText();
            else
                text = "";
        }

        return text;
    }

    public void clickApply()
    {
        clickApply(10000);
    }

    // If waitTime is set to -1 it means you expected the mask to not go away. That is you expected an error.
    // An example would be not setting all of the required fields. In that case the color of the text of the required field would
    // change and the mask would not go away.
    public void clickApply(int waitTime)
    {
        _test.clickButton("Apply", 0);

        // If not equal to -1 then the apply should work.
        if(waitTime != -1)
        {
            _test.sleep(1000);
            _test._ext4Helper.waitForMaskToDisappear(waitTime);
        }

    }

    public void clickCancel()
    {
        _test.clickButton("Cancel", 0);
    }

    // Should be something like 'Box' or 'Scatter'
    public String getChartTypeTitle()
    {
        return elements().typeTitle.getText();
    }

    // Simply clicks a value int he column list. Can be used to see if the value can be dropped to one of the attributes.
    public void clickColumnValue(String columnValue)
    {
        _test.click(Locator.xpath("//div[text()='" + columnValue + "']"));
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
        List<WebElement> webElements = Locator.findElements(_test.getDriver(), Locator.xpath(elements().DIALOG_XPATH + "//div[contains(@class, 'field-title')]"));
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

        public final String DIALOG_XPATH = "//div[contains(@class, 'chart-wizard-dialog')]//div[contains(@class, 'chart-type-panel')]";
        public final String XAXIS_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'X Axis')]";
        public final String YAXIS_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Y Axis')]";
        public final String CATEGORIES_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Categories')]";
        public final String MEASURE_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Measure')]";
        public final String COLOR_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Color')]";
        public final String SHAPE_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Shape')]";

        public final String FIELD_AREA = "/following-sibling::div[contains(@class, 'field-area ')]";
        public final String FIELD_DISPLAY = "//div[@class='field-selection-display']";
        public final String DROP_TEXT = "/following-sibling::div[contains(@class, 'field-area-drop-text ')]";
        public final String REMOVE_ICON = FIELD_DISPLAY + "//div[contains(@class, 'field-selection-remove')]";

        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement dialog = new LazyWebElement(Locator.xpath(DIALOG_XPATH), _test.getDriver());
        public WebElement typeTitle = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'type-title')]"), _test.getDriver());

        public WebElement columnList = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'mapping-query-col')]"), _test.getDriver());
        public WebElement fieldTitles = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[contains(@class, 'field-title')]"), _test.getDriver());

        public WebElement plotTypeBar = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-bar_chart']"),  _test.getDriver());
        public WebElement plotTypeBox = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-box_plot']"),  _test.getDriver());
        public WebElement plotTypePie = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-pie_chart']"),  _test.getDriver());
        public WebElement plotTypeScatter = new LazyWebElement(Locator.xpath(DIALOG_XPATH + "//div[@id='chart-type-scatter_plot']"),  _test.getDriver());

        public WebElement xAxis = new LazyWebElement(Locator.xpath(DIALOG_XPATH + XAXIS_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement xAxisDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + XAXIS_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement xAxisRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + XAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());

        public WebElement yAxis = new LazyWebElement(Locator.xpath(DIALOG_XPATH + YAXIS_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement yAxisDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + YAXIS_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement yAxisRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + YAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());

        public WebElement categories = new LazyWebElement(Locator.xpath(DIALOG_XPATH + CATEGORIES_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement categoriesDisplay = new LazyWebElement(Locator.xpath(DIALOG_XPATH + CATEGORIES_CONTAINER + FIELD_AREA + FIELD_DISPLAY),  _test.getDriver());
        public WebElement categoriesDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + CATEGORIES_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement categoriesRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + CATEGORIES_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());

        public WebElement measure = new LazyWebElement(Locator.xpath(DIALOG_XPATH + MEASURE_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement measureDisplay = new LazyWebElement(Locator.xpath(DIALOG_XPATH + MEASURE_CONTAINER + FIELD_AREA + FIELD_DISPLAY),  _test.getDriver());
        public WebElement measureDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + MEASURE_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement measureRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + MEASURE_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());

        public WebElement color = new LazyWebElement(Locator.xpath(DIALOG_XPATH + COLOR_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement colorDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + COLOR_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement colorRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + COLOR_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());

        public WebElement shape = new LazyWebElement(Locator.xpath(DIALOG_XPATH + SHAPE_CONTAINER + FIELD_AREA),  _test.getDriver());
        public WebElement shapeDropText = new LazyWebElement(Locator.xpath(DIALOG_XPATH + SHAPE_CONTAINER + DROP_TEXT),  _test.getDriver());
        public WebElement shapeRemove = new LazyWebElement(Locator.xpath(DIALOG_XPATH + SHAPE_CONTAINER + FIELD_AREA + REMOVE_ICON),  _test.getDriver());
    }

    public enum ChartType
    {
        Bar,
        Box,
        Pie,
        Scatter
    }
}
