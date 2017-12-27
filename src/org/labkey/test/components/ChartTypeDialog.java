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

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class ChartTypeDialog<EC extends ChartTypeDialog.ElementCache> extends ChartWizardDialog <EC>
{
    public ChartTypeDialog(WebDriver driver)
    {
        super("Create a plot", driver);
        Ext4Helper.Locators.ext4Button("Apply").waitForElement(this, 10000);
    }

    public ChartTypeDialog setChartType(ChartType chartType)
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
            case Time:
                elementCache().plotTypeTime.click();
                break;
            case Line:
                elementCache().plotTypeLine.click();
                break;
        }

        return this;
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
            case Time:
                classValue = elementCache().plotTypeTime.getAttribute("class");
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

    public ChartTypeDialog setXAxis(String columnName)
    {
        setXAxis(columnName, false);
        return this;
    }

    public ChartTypeDialog setXAxis(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().xAxisTitle(), columnName);
        else
            setValue(elementCache().xAxisDropText(), columnName);
        return this;
    }

    public ChartTypeDialog setSeries(String columnName)
    {
        setValue(elementCache().seriesTitle(), columnName);
        return this;
    }

    public ChartTypeDialog setXCategory(String columnName)
    {
        setXCategory(columnName, false);
        return this;
    }

    public ChartTypeDialog setXCategory(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().xCategoriesTitle(), columnName);
        else
            setValue(elementCache().xCategoriesDropText(), columnName);
        return this;
    }

    public ChartTypeDialog setXSubCategory(String columnName)
    {
        setXSubCategory(columnName, false);
        return this;
    }

    public ChartTypeDialog setXSubCategory(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().xSubCategoriesTitle(), columnName);
        else
            setValue(elementCache().xSubCategoriesDropText(), columnName);
        return this;
    }

    public ChartTypeDialog setYAxis(String columnName)
    {
        setYAxis(columnName, false);
        return this;
    }

    public ChartTypeDialog setYAxis(String columnName, boolean hasExistingSelection)
    {
        // the column selection grid differs depending on the selected chart type
        String columnGridCls = "Time".equals(getChartTypeTitle()) ? "study-columns-grid" : "query-columns-grid";

        if (hasExistingSelection)
            setValue(elementCache().yAxisTitle(), columnName, columnGridCls);
        else
            setValue(elementCache().yAxisDropText(), columnName,columnGridCls);
        return this;
    }

    public ChartTypeDialog clickYAxisMeasure(String columnName)
    {
        getWrapper().waitAndClick(elementCache().Y_FIELD_TEXT.withText(columnName));
        return this;
    }

    public ChartTypeDialog setYAxisSide(int measureIndex, YAxisSide side)
    {
        getWrapper().mouseOver(elementCache().yAxis());
        switch(side)
        {
            case Left:
                final WebElement leftArrow = elementCache().Y_FIELD_SIDE_LEFT.index(measureIndex).findElement(this);
                leftArrow.click();
                getWrapper().shortWait().until(ExpectedConditions.stalenessOf(leftArrow));
                break;
            case Right:
                final WebElement rightArrow = elementCache().Y_FIELD_SIDE_RIGHT.index(measureIndex).findElement(this);
                rightArrow.click();
                getWrapper().shortWait().until(ExpectedConditions.stalenessOf(rightArrow));
                break;
        }
        return this;
    }

    public ChartTypeDialog setYAxisAggregateMethod(String aggMethod)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Aggregate Method:", aggMethod);
        return this;
    }

    public ChartTypeDialog setCategories(String columnName)
    {
        setCategories(columnName, false);
        return this;
    }

    public ChartTypeDialog setCategories(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().categoriesTitle(), columnName);
        else
            setValue(elementCache().categoriesDropText(), columnName);
        return this;
    }

    public String getXCategories() {
        String value;

        try
        {
            return elementCache().xCategoriesDisplay().getText();
        }
        catch(org.openqa.selenium.NoSuchElementException nse)
        {
            value = "";
        }

        return value;
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

    public ChartTypeDialog setMeasure(String columnName)
    {
        setMeasure(columnName, false);
        return this;
    }

    public ChartTypeDialog setMeasure(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().measureTitle(), columnName);
        else
            setValue(elementCache().measureDropText(), columnName);
        return this;
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

    public ChartTypeDialog setColor(String columnName)
    {
        setColor(columnName, false);
        return this;
    }

    public ChartTypeDialog setColor(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().colorTitle(), columnName);
        else
            setValue(elementCache().colorDropText(), columnName);
        return this;
    }

    public ChartTypeDialog setShape(String columnName)
    {
        setShape(columnName, false);
        return this;
    }

    public ChartTypeDialog setShape(String columnName, boolean replaceExisting)
    {
        if (replaceExisting)
            setValue(elementCache().shapeTitle(), columnName);
        else
            setValue(elementCache().shapeDropText(), columnName);
        return this;
    }

    private ChartTypeDialog setValue(WebElement target, String columnName)
    {
        return setValue(target, columnName, "query-columns-grid");
    }

    private ChartTypeDialog setValue(WebElement target, String columnName, String columnGridCls)
    {
        elementCache().getColumn(columnName, columnGridCls).click();
        getWrapper().waitFor(() -> {
            return target.isDisplayed();
        }, "Target element is not displayed", 5000);
        target.click();
        getWrapper().waitForFormElementToNotEqual(target.findElement(By.xpath("//div[contains(@class, 'field-selection-text')]")), columnName);
        return this;
    }

    public ChartTypeDialog removeXAxis()
    {
        getWrapper().mouseOver(elementCache().xAxis());
        final WebElement remove = elementCache().xAxisRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
        return this;
    }

    public ChartTypeDialog removeYAxis()
    {
        getWrapper().mouseOver(elementCache().yAxis());
        final WebElement remove = elementCache().yAxisRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
        return this;
    }

    public ChartTypeDialog removeSeries()
    {
        getWrapper().mouseOver(elementCache().series());
        final WebElement remove = elementCache().seriesRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
        return this;
    }

    public ChartTypeDialog removeColor()
    {
        getWrapper().mouseOver(elementCache().color());
        final WebElement remove = elementCache().colorRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
        return this;
    }

    public ChartTypeDialog removeShape()
    {
        getWrapper().mouseOver(elementCache().shape());
        final WebElement remove = elementCache().shapeRemove();
        remove.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(remove));
        return this;
    }

    public String getXAxisValue()
    {
        return getFieldValue(elementCache().xAxis(), elementCache().xAxisDropText());
    }

    public String getYAxisValue()
    {
        return getFieldValue(elementCache().yAxis(), elementCache().yAxisDropText());
    }

    public String getColorValue()
    {
        return getFieldValue(elementCache().color(), elementCache().colorDropText());
    }

    public String getShapeValue()
    {
        return getFieldValue(elementCache().shape(), elementCache().shapeDropText());
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

    public ChartTypeDialog selectStudyQuery(String queryName)
    {
        getWrapper().waitForElementToDisappear(elementCache().queryColumnsPanelMask, 2 * WAIT_FOR_JAVASCRIPT);
        getWrapper().click(elementCache().studyQueryCombo.append("//div[contains(@class,'arrow')]"));
        getWrapper().waitAndClick(Ext4Helper.Locators.comboListItem().withText(queryName));
        return this;
    }

    public ChartTypeDialog setTimeAxisType(TimeAxisType axisType)
    {
        switch(axisType)
        {
            case Date:
                getWrapper().click(elementCache().timeAxisDateBasedRadioButton);
                getWrapper().waitForElementToDisappear(elementCache().disabledTimeInterval);
                getWrapper().assertElementNotPresent(elementCache().disabledIntervalStartDate);
                break;
            case Visit:
                getWrapper().click(elementCache().timeAxisVisitBasedRadioButton);
                getWrapper().waitForElement(elementCache().disabledTimeInterval);
                getWrapper().assertElementPresent(elementCache().disabledIntervalStartDate);
                break;
        }
        return this;
    }

    public ChartTypeDialog setTimeInterval(String interval)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Time Interval:", interval);
        return this;
    }

    public ChartTypeDialog setTimeIntervalStartDate(String startDateColLabel)
    {
        getWrapper()._ext4Helper.selectComboBoxItem("Interval Start Date:", startDateColLabel);
        return this;
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
    public ChartTypeDialog clickColumnValue(String columnValue)
    {
        elementCache().getColumn(columnValue, "query-columns-grid").click();
        return this;
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
    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    class ElementCache extends ChartWizardDialog.ElementCache
    {
        public final String XAXIS_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'X Axis')]";
        public final String XCATEGORY_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'X Axis Categories')]";
        public final String XSUBCATEGORY_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Split Categories By')]";
        public final String YAXIS_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Y Axis')]";
        public final String CATEGORIES_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Categories')]";
        public final String MEASURE_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Measure')]";
        public final String COLOR_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Color')]";
        public final String SHAPE_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Shape')]";
        public final String FIELD_AREA = "/following-sibling::div[contains(@class, 'field-area ')]";

        public final String FIELD_DISPLAY = "//div[contains(@class, 'field-selection-display')]";
        public final String DROP_TEXT = "/following-sibling::div[contains(@class, 'field-area-drop-text ')]";
        public final String REMOVE_ICON = FIELD_DISPLAY + "//div[contains(@class, 'field-selection-remove')]";
        public Locator Y_FIELD_TEXT = Locator.xpath(YAXIS_CONTAINER + FIELD_AREA + FIELD_DISPLAY + "//div[contains(@class, 'field-selection-text')]");

        public Locator Y_FIELD_SIDE_LEFT = Locator.xpath(YAXIS_CONTAINER + FIELD_AREA + FIELD_DISPLAY + "//i[contains(@class, 'fa-arrow-circle-left')]");
        public Locator Y_FIELD_SIDE_RIGHT = Locator.xpath(YAXIS_CONTAINER + FIELD_AREA + FIELD_DISPLAY + "//i[contains(@class, 'fa-arrow-circle-right')]");
        public WebElement typeTitle = new LazyWebElement(Locator.xpath("//div[contains(@class, 'type-title')]"), this);

        public final String SERIES_CONTAINER = "//div[contains(@class, 'field-title')][contains(text(), 'Series')]";

        public WebElement columnList = new LazyWebElement(Locator.xpath("//div[contains(@class, 'mapping-query-col')]"), this);
        public WebElement getColumn(String column, String columnGridCls)
        {
            Locator.XPathLocator gridLoc = Locator.tagWithClass("div", columnGridCls);
            return gridLoc.append(Locator.tagWithClass("tr", "x4-grid-data-row").withText(column)).waitForElement(columnList, 10000);
        }
        public WebElement fieldTitles = new LazyWebElement(Locator.xpath("//div[contains(@class, 'field-title')]"), this);

        public WebElement plotTypeBar = Locator.xpath("//div[@id='chart-type-bar_chart']").findWhenNeeded(this).withTimeout(1000);
        public WebElement plotTypeBox = Locator.xpath("//div[@id='chart-type-box_plot']").findWhenNeeded(this).withTimeout(1000);
        public WebElement plotTypePie = Locator.xpath("//div[@id='chart-type-pie_chart']").findWhenNeeded(this).withTimeout(1000);
        public WebElement plotTypeScatter = Locator.xpath("//div[@id='chart-type-scatter_plot']").findWhenNeeded(this).withTimeout(1000);
        public WebElement plotTypeTime = Locator.xpath("//div[@id='chart-type-time_chart']").findWhenNeeded(this).withTimeout(1000);
        public WebElement plotTypeLine = Locator.xpath("//div[@id='chart-type-line_plot']").findWhenNeeded(this).withTimeout(1000);

        public WebElement xAxis() {return Locator.xpath(XAXIS_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement xAxisTitle() {return Locator.xpath(XAXIS_CONTAINER).findElement(this);}
        public WebElement xAxisDropText() {return Locator.xpath(XAXIS_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement xAxisRemove() {return Locator.xpath(XAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}
        public WebElement xCategories() {return Locator.xpath(XCATEGORY_CONTAINER + FIELD_AREA).findElement(this);}

        public WebElement xCategoriesTitle() {return Locator.xpath(XCATEGORY_CONTAINER).findElement(this);}
        public WebElement xCategoriesDisplay(){return Locator.xpath(XCATEGORY_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement xCategoriesDropText() {return Locator.xpath(XCATEGORY_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement xCategoriesRemove() {return Locator.xpath(XCATEGORY_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement seriesTitle() {return Locator.xpath(SERIES_CONTAINER).findElement(this);}

        public WebElement xSubCategories() {return Locator.xpath(XSUBCATEGORY_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement xSubCategoriesTitle() {return Locator.xpath(XSUBCATEGORY_CONTAINER).findElement(this);}
        public WebElement xSubCategoriesDisplay(){return Locator.xpath(XSUBCATEGORY_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement xSubCategoriesDropText(){return Locator.xpath(XSUBCATEGORY_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement xSubCategoriesRemove(){return Locator.xpath(XSUBCATEGORY_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement yAxis() {return Locator.xpath(YAXIS_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement yAxisTitle() {return Locator.xpath(YAXIS_CONTAINER).findElement(this);}
        public WebElement yAxisDropText() {return Locator.xpath(YAXIS_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement yAxisRemove() {return Locator.xpath(YAXIS_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement series() {return Locator.xpath(SERIES_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement seriesRemove() {return Locator.xpath(SERIES_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement categories() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement categoriesTitle() {return Locator.xpath(CATEGORIES_CONTAINER).findElement(this);}
        public WebElement categoriesDisplay() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement categoriesDropText() {return Locator.xpath(CATEGORIES_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement categoriesRemove() {return Locator.xpath(CATEGORIES_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement measure() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement measureTitle() {return Locator.xpath(MEASURE_CONTAINER).findElement(this);}
        public WebElement measureDisplay() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA + FIELD_DISPLAY).findElement(this);}
        public WebElement measureDropText() {return Locator.xpath(MEASURE_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement measureRemove() {return Locator.xpath(MEASURE_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement color() {return Locator.xpath(COLOR_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement colorTitle() {return Locator.xpath(COLOR_CONTAINER).findElement(this);}
        public WebElement colorDropText() {return Locator.xpath(COLOR_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement colorRemove() {return Locator.xpath(COLOR_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public WebElement shape() {return Locator.xpath(SHAPE_CONTAINER + FIELD_AREA).findElement(this);}
        public WebElement shapeTitle() {return Locator.xpath(SHAPE_CONTAINER).findElement(this);}
        public WebElement shapeDropText() {return Locator.xpath(SHAPE_CONTAINER + DROP_TEXT).findElement(this);}
        public WebElement shapeRemove() {return Locator.xpath(SHAPE_CONTAINER + FIELD_AREA + REMOVE_ICON).findElement(this);}

        public Locator queryColumnsPanelMask = Locator.tagWithClass("div", "query-columns").withDescendant(Locator.tagWithClass("div", "x4-mask"));
        public Locator.XPathLocator studyQueryCombo = Locator.xpath("//tr["+Locator.NOT_HIDDEN+" and ./td/input[@placeholder='Select a query']]");
        public Locator timeAxisDateBasedRadioButton = Locator.xpath("//label[text()='Date-Based']/preceding-sibling::input[@type='button']");
        public Locator timeAxisVisitBasedRadioButton = Locator.xpath("//label[text()='Visit-Based']/preceding-sibling::input[@type='button']");
        public Locator disabledTimeInterval = Locator.xpath("//table[//label[text() = 'Time Interval:'] and contains(@class, 'x4-item-disabled')]");
        public Locator disabledIntervalStartDate = Locator.xpath("//table[//label[text() = 'Interval Start Date:'] and contains(@class, 'x4-item-disabled')]");
    }

    public enum ChartType
    {
        Bar,
        Box,
        Pie,
        Scatter,
        Line,
        Time
    }

    public enum TimeAxisType
    {
        Date,
        Visit
    }

    public enum YAxisSide
    {
        Left,
        Right
    }
}
