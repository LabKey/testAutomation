package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartTypeDialog extends ChartWizardDialog<ChartTypeDialog.ElementCache>
{
    public ChartTypeDialog(WebDriver driver)
    {
        super("Create a plot", driver);
        Ext4Helper.Locators.ext4Button("Apply").waitForElement(this, 10000);
    }

    @Deprecated
    public ChartTypeDialog(BaseWebDriverTest test)
    {
        this(test.getDriver());
    }

    @Deprecated // Does nothing
    public void waitForDialog()
    {
    }

    public void setChartType(ChartType chartType)
    {
        switch(chartType)
        {
            case Bar:
                elementCache().plotTypeBar.click();
                break;
            case Box:
                elementCache().plotTypeBox.click();
                break;
            case Pie:
                elementCache().plotTypePie.click();
                break;
            case Scatter:
                elementCache().plotTypeScatter.click();
                break;
        }
    }

    public boolean isChartTypeEnabled(ChartType chartType)
    {
        String classValue = "";

        switch(chartType)
        {
            case Bar:
                classValue = elementCache().plotTypeBar.getAttribute("class");
                break;
            case Box:
                classValue = elementCache().plotTypeBox.getAttribute("class");
                break;
            case Pie:
                classValue = elementCache().plotTypePie.getAttribute("class");
                break;
            case Scatter:
                classValue = elementCache().plotTypeScatter.getAttribute("class");
                break;
            default:
                classValue = "-disabled";
        }

        if(classValue.contains("-disabled"))
            return false;
        else
            return true;
    }

    public List<String> getListOfChartTypes()
    {
        return getWrapper().getTexts(Locator.xpath("//div[contains(@class, 'types-panel')]//div[contains(@id, 'chart-type')]//div").findElements(this));
    }

    public void setXAxis(String columnName)
    {
        setXAxis(columnName, false);
    }

    public void setXAxis(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().xAxis(), columnName);
        else
            setValue(elementCache().xAxisDropText, columnName);
    }

    public void setYAxis(String columnName)
    {
        setYAxis(columnName, false);
    }

    public void setYAxis(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().yAxis(), columnName);
        else
            setValue(elementCache().yAxisDropText, columnName);
    }

    public void setCategories(String columnName)
    {
        setCategories(columnName, false);
    }

    public void setCategories(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().categories(), columnName);
        else
            setValue(elementCache().categoriesDropText, columnName);
    }

    public String getCategories()
    {
        String value;

        try
        {
            return elementCache().categoriesDisplay().getText();
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
            setValue(elementCache().measure(), columnName);
        else
            setValue(elementCache().measureDropText, columnName);
    }

    public String getMeasure()
    {
        String value;

        try
        {
            value = elementCache().measureDisplay().getText();
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
            setValue(elementCache().color(), columnName);
        else
            setValue(elementCache().colorDropText, columnName);
    }

    public void setShape(String columnName)
    {
        setShape(columnName, false);
    }

    public void setShape(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().shape(), columnName);
        else
            setValue(elementCache().shapeDropText, columnName);
    }

    private void setValue(WebElement target, String columnName)
    {
        elementCache().getColumn(columnName).click();
        target.click();
        getWrapper().waitForFormElementToNotEqual(target.findElement(By.xpath("//div[@class='field-selection-display']")), columnName);
    }

    public void removeXAxis()
    {
        getWrapper().mouseOver(elementCache().xAxis());
        final WebElement remove = elementCache().xAxisRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
    }

    public void removeYAxis()
    {
        getWrapper().mouseOver(elementCache().yAxis());
        final WebElement remove = elementCache().yAxisRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
    }

    public void removeColor()
    {
        getWrapper().mouseOver(elementCache().color());
        final WebElement remove = elementCache().colorRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
    }

    public void removeShape()
    {
        getWrapper().mouseOver(elementCache().shape());
        final WebElement remove = elementCache().shapeRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
    }

    public String getXAxisValue()
    {
        return getFieldValue(elementCache().xAxis(), elementCache().xAxisDropText);
    }

    public String getYAxisValue()
    {
        return getFieldValue(elementCache().yAxis(), elementCache().yAxisDropText);
    }

    public String getColorValue()
    {
        return getFieldValue(elementCache().color(), elementCache().colorDropText);
    }

    public String getShapeValue()
    {
        return getFieldValue(elementCache().shape(), elementCache().shapeDropText);
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
        clickButton("Apply", 0);

        // If not equal to -1 then the apply should work.
        if(waitTime != -1)
        {
            waitForClose();
            getWrapper().sleep(1000);
            getWrapper()._ext4Helper.waitForMaskToDisappear(waitTime);
        }

    }

    // Should be something like 'Box' or 'Scatter'
    public String getChartTypeTitle()
    {
        return elementCache().typeTitle.getText();
    }

    // Simply clicks a value int he column list. Can be used to see if the value can be dropped to one of the attributes.
    public void clickColumnValue(String columnValue)
    {
        elementCache().getColumn(columnValue).click();
    }

    public ArrayList<String> getColumnList()
    {
        String rawText = elementCache().columnList.getText();
        String[] textArray = rawText.split("\n");
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(textArray));
        arrayList.remove(0); // Remove the first element in the list. This is the title "Columns".
        return arrayList;
    }

    public ArrayList<String> getListOfRequiredFields()
    {
        ArrayList<String> requiredFields = new ArrayList<>();
        List<WebElement> webElements = Locator.xpath("//div[contains(@class, 'field-title')]").findElements(this);
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
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends ChartWizardDialog.ElementCache
    {
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

        public WebElement typeTitle = new LazyWebElement(Locator.xpath("//div[contains(@class, 'type-title')]"), this);

        public WebElement columnList = new LazyWebElement(Locator.xpath("//div[contains(@class, 'mapping-query-col')]"), this);
        public WebElement getColumn(String column)
        {
            return Locator.tagWithClass("tr", "x4-grid-data-row").withText(column).waitForElement(columnList, 10000);
        }
        public WebElement fieldTitles = new LazyWebElement(Locator.xpath("//div[contains(@class, 'field-title')]"), this);

        public WebElement plotTypeBar = new LazyWebElement(Locator.xpath("//div[@id='chart-type-bar_chart']"),  this);
        public WebElement plotTypeBox = new LazyWebElement(Locator.xpath("//div[@id='chart-type-box_plot']"),  this);
        public WebElement plotTypePie = new LazyWebElement(Locator.xpath("//div[@id='chart-type-pie_chart']"),  this);
        public WebElement plotTypeScatter = new LazyWebElement(Locator.xpath("//div[@id='chart-type-scatter_plot']"),  this);

        public WebElement xAxis() {return Locator.xpath(XAXIS_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement xAxisDropText = new LazyWebElement(Locator.xpath(XAXIS_CONTAINER + DROP_TEXT),  this);
        public WebElement xAxisRemove() {return Locator.xpath(XAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement yAxis() {return Locator.xpath(YAXIS_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement yAxisDropText = new LazyWebElement(Locator.xpath(YAXIS_CONTAINER + DROP_TEXT),  this);
        public WebElement yAxisRemove() {return Locator.xpath(YAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement categories() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement categoriesDisplay() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement categoriesDropText = new LazyWebElement(Locator.xpath(CATEGORIES_CONTAINER + DROP_TEXT),  this);
        public WebElement categoriesRemove() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement measure() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement measureDisplay() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement measureDropText = new LazyWebElement(Locator.xpath(MEASURE_CONTAINER + DROP_TEXT),  this);
        public WebElement measureRemove() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement color() {return Locator.xpath(COLOR_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement colorDropText = new LazyWebElement(Locator.xpath(COLOR_CONTAINER + DROP_TEXT),  this);
        public WebElement colorRemove() {return Locator.xpath(COLOR_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement shape() {return Locator.xpath(SHAPE_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement shapeDropText = new LazyWebElement(Locator.xpath(SHAPE_CONTAINER + DROP_TEXT),  this);
        public WebElement shapeRemove() {return Locator.xpath(SHAPE_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}
    }

    public enum ChartType
    {
        Bar,
        Box,
        Pie,
        Scatter
    }
}
