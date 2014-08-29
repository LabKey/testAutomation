/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.util.GenericChartHelper.Locators.*;

public class GenericChartHelper
{
    protected BaseWebDriverTest _test;
    protected String _sourceQuery;
    protected String _currentTitle;
    protected String _xMeasure;
    protected String _yMeasure;
    protected GenericChartAxisWindow _currentWindow;
    protected WebElement _currentSvg = null;
    protected boolean _svgExpectedStale;

    /**
     * Creating a plot from manage views page
     */
    public GenericChartHelper(BaseWebDriverTest test)
    {
        this(test, null, null, null);
    }

    /**
     * Creating a plot from a data region
     */
    public GenericChartHelper(BaseWebDriverTest test, String sourceQuery)
    {
        this(test, null, null, null);
        _sourceQuery = sourceQuery;
    }

    /**
     * Use for loading a saved chart
     */
    public GenericChartHelper(BaseWebDriverTest test, String yMeasure, String xMeasure, String title)
    {
        _test = test;
        _yMeasure = yMeasure;
        _xMeasure = xMeasure;
        _currentTitle = title;

        if (_yMeasure == null)
        {
            _currentWindow = new YAxisWindow();
        }
        else
        {
            _currentWindow = new NoAxisWindow();
            stashNewSvg();
        }
    }

    protected String getDefaultTitle()
    {
        return _sourceQuery + " - " + _yMeasure;
    }

    public WebElement getCurrentSvg()
    {
        return _currentSvg;
    }

    public void selectMeasure(String measure)
    {
        _currentWindow.selectMeasure(measure);
    }

    public void confirmSelection()
    {
        _currentWindow.confirmSelection();
        stashNewSvg();
    }

    public void openXAxisWindow()
    {
        clickAxisLabel(_xMeasure);
        _currentWindow = new XAxisWindow();
    }

    public void openYAxisWindow()
    {
        clickAxisLabel(_yMeasure);
        _currentWindow = new YAxisWindow();
    }

    public void saveChart(String name)
    {
        _test.clickButton("Save", 0);
        _test.waitForElement(saveWindow);
        _test.setFormElement(Locator.name("reportName"), name);
        _test.click(saveWindow.append(Ext4Helper.Locators.ext4Button("Save")));
        _test.waitForElement(PortalHelper.Locators.webPartTitle(name));
    }

    private void clickAxisLabel(String axisText)
    {
//        _test.waitAndClick(Locator.css("svg text").containing(axisText));
        _test.fireEvent(Locator.css("svg text").containing(axisText).waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT), BaseWebDriverTest.SeleniumEvent.click);
    }

    private void stashNewSvg()
    {
        if (_currentSvg != null && _svgExpectedStale)
        {
            _test.shortWait().until(ExpectedConditions.stalenessOf(_currentSvg));
            _test._extHelper.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }

        _currentSvg = Locator.css("svg").waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _svgExpectedStale = false;
    }

    private abstract class GenericChartAxisWindow
    {
        protected Locator.XPathLocator _windowLoc;
        protected String _selectedMeasure;

        GenericChartAxisWindow()
        {
            _windowLoc = null;
        }

        protected GenericChartAxisWindow(Locator.XPathLocator windowLoc)
        {
            _windowLoc = windowLoc.notHidden();
            _test.waitForElement(_windowLoc);
        }

        public void selectMeasure(String measure)
        {
            _test.waitAndClick(_windowLoc.append(pickerRow.withText(measure)));
            if (!measure.equals(_selectedMeasure))
            {
                _selectedMeasure = measure;
                _svgExpectedStale = true;
            }
        }

        public void confirmSelection()
        {
            _test.click(_windowLoc.append(Ext4Helper.Locators.ext4Button("Ok")));
            _test.waitForElementToDisappear(_windowLoc);
            stashMeasure();
            _currentWindow = nextWindow();
        }

        protected GenericChartAxisWindow nextWindow()
        {
            return new NoAxisWindow();
        }

        protected abstract void stashMeasure();
    }

    private class NoAxisWindow extends GenericChartAxisWindow
    {
        @Override
        public void confirmSelection()
        {
            Assert.fail("No Axis Window Open");
        }

        @Override
        public void selectMeasure(String measure)
        {
            Assert.fail("No Axis Window Open");
        }

        @Override
        protected void stashMeasure(){}
    }

    private class YAxisWindow extends GenericChartAxisWindow
    {
        public YAxisWindow()
        {
            super(yWindow);
        }

        @Override
        protected void stashMeasure()
        {
            _yMeasure = _selectedMeasure;
        }

        @Override
        protected GenericChartAxisWindow nextWindow()
        {
            if (_xMeasure == null)
                return new XAxisWindow();
            else
                return new NoAxisWindow();
        }
    }

    private class XAxisWindow extends GenericChartAxisWindow
    {
        public XAxisWindow()
        {
            super(xWindow);
        }

        @Override
        protected void stashMeasure()
        {
            _xMeasure = _selectedMeasure;
        }
    }

    public static class Locators
    {
        public static final Locator.XPathLocator yWindow = Ext4Helper.Locators.window("Y Axis");
        public static final Locator.XPathLocator xWindow = Ext4Helper.Locators.window("X Axis");
        public static final Locator.XPathLocator titleWindow = Ext4Helper.Locators.window("Main Title");
        public static final Locator.XPathLocator saveWindow = Ext4Helper.Locators.window("Save");
        public static final Locator.XPathLocator pickerRow = Locator.tagWithClass("tr", "x4-grid-data-row");
    }
}
