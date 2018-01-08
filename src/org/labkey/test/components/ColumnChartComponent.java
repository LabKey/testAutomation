/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ColumnChartComponent
{
    public final static String TYPE_PIE = "piechart";
    public final static String TYPE_BAR = "bar";
    public final static String TYPE_BOX = "box";
    public final static String TYPE_UNKNOWN = "unknown";

    private WebDriverWrapper _driver;
    private WebElement _divReference, _svgReference;
    private String _title, _footer, _type, _plotId;
    private int _numberOfDataPoints;

    public ColumnChartComponent(WebDriverWrapper driver, WebElement webelement)
    {
        List<WebElement> childTags;
        String classValue;
        int axisCount;

        _driver = driver;
        _divReference = webelement;

        _title = _footer = _type = "";
        _numberOfDataPoints = -1;
        _svgReference = Locator.css("svg").waitForElement(_divReference, 10000);

        axisCount = _svgReference.findElements(By.cssSelector(" g.axis")).size();

        if(axisCount == 0)
        {
            childTags = _svgReference.findElements(By.cssSelector(" g"));
            classValue = "";

            // Since this is a pie chart find the tag that has a class attribute containing _pieChart, this will be the id.
            for(WebElement we : childTags)
            {
                if(we.getAttribute("class").toLowerCase().contains(TYPE_PIE))
                {
                    classValue = we.getAttribute("class");
                    break;
                }
            }

            if(classValue.length() != 0)
            {
                // Id for pie charts is different form bar and box plots.
                _plotId = classValue.substring(0, classValue.indexOf("_"));
                _type = TYPE_PIE;
            }
            else
            {
                // So if it didn't have axis values and didn't have a "piechart" class, I don't know what it is.
                _type = TYPE_UNKNOWN;
            }
        }
        else
        {

            _type = TYPE_UNKNOWN;

            // For these charts use the id that is called oout in the parent div.
            _plotId = webelement.getAttribute("id");

            // Need to identify if it is a bar or box plot.
            childTags = _svgReference.findElements(By.cssSelector(" g.layer a"));

            for(WebElement we : childTags)
            {
                if(we.getAttribute("class").toLowerCase().equals(TYPE_BOX))
                {
                    _type = TYPE_BOX;
                    break;
                }
                if(we.getAttribute("class").toLowerCase().contains(TYPE_BAR))
                {
                    _type = TYPE_BAR;
                    break;
                }
            }

        }

    }

    public WebElement getComponentElement()
    {
        return _divReference;
    }

    public String getPlotType()
    {
        return _type;
    }

    public String getTitle()
    {
        switch (_type)
        {
            case TYPE_PIE:
                _title = _svgReference.findElement(By.cssSelector(" text#"+ _plotId + "_title")).getText();
                break;
            case TYPE_BAR:
            case TYPE_BOX:
                _title = _svgReference.findElement(By.cssSelector(" g." + _plotId + "-labels text")).getText();
                break;
            case TYPE_UNKNOWN:
            default:
                _title = "";
                break;
        }

        return _title;
    }

    public String getFooter()
    {
        switch (_type)
        {
            case TYPE_PIE:
                _footer = _svgReference.findElement(By.cssSelector(" text#"+ _plotId + "_footer")).getText();
                break;
            case TYPE_BAR:
            case TYPE_BOX:
            case TYPE_UNKNOWN:
            default:
                _footer = "";
                break;
        }

        return _footer;
    }

    public int getNumberOfDataPoints()
    {
        switch (_type)
        {
            case TYPE_PIE:
                _numberOfDataPoints = _svgReference.findElements(By.cssSelector(" g." + _plotId + "_arc")).size();
                break;
            case TYPE_BAR:
                _numberOfDataPoints = _svgReference.findElements(By.cssSelector(" g.layer a.bar-individual")).size();
                break;
            case TYPE_BOX:
                _numberOfDataPoints = _svgReference.findElements(By.cssSelector(" g.layer a.box")).size();
                break;
            case TYPE_UNKNOWN:
            default:
                _numberOfDataPoints = -1;
                break;
        }

        return _numberOfDataPoints;
    }

    public void hoverOverDataPoint(int index)
    {
        switch (_type)
        {
            case TYPE_PIE:
                _driver.mouseOver(_svgReference.findElements(By.cssSelector(" g." + _plotId + "_arc")).get(index));
                break;
            case TYPE_BAR:
                _driver.mouseOver(_svgReference.findElements(By.cssSelector(" g.layer a.bar-individual")).get(index));
            case TYPE_BOX:
                _driver.mouseOver(_svgReference.findElements(By.cssSelector(" g.layer a.box")).get(index));
                break;
            case TYPE_UNKNOWN:
            default:
                _driver.log("Don't know this plot type, so not sure where to hover.");
                break;
        }

    }

    public boolean isRemoveIconVisible()
    {
        return _divReference.findElement(By.cssSelector(" div.plot-analytics-remove")).isDisplayed();
    }

    public void showRemoveIcon()
    {
        _driver.mouseOver(_divReference);
    }

    public void removePlot()
    {
        _divReference.findElement(By.cssSelector(" div.plot-analytics-remove")).click();
    }

}
