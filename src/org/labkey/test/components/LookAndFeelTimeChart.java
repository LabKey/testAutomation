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

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LookAndFeelTimeChart extends ChartLayoutDialog<LookAndFeelTimeChart.ElementCache>
{
    public LookAndFeelTimeChart(WebDriver wDriver)
    {
        super(wDriver);
    }

    public LookAndFeelTimeChart setPlotTitle(String title)
    {
        super.setPlotTitle(title);
        return this;
    }

    public LookAndFeelTimeChart clickResetTitle()
    {
        super.clickResetTitle();
        return this;
    }

    public LookAndFeelTimeChart setPlotWidth(String width)
    {
        super.setPlotWidth(width);
        return this;
    }

    public LookAndFeelTimeChart setPlotHeight(String height)
    {
        super.setPlotHeight(height);
        return this;
    }

    public LookAndFeelTimeChart setLineWidth(int lineWidth)
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

    public LookAndFeelTimeChart clickHideDataPoints()
    {
        getWrapper().click(elementCache().hideDataPointsCheckbox);
        return this;
    }

    public boolean isHideDataPointsChecked()
    {
        String classValue = elementCache().hideDataPointsCheckboxValue.getAttribute("class");
        return classValue.toLowerCase().contains("x4-form-cb-checked");
    }

    public LookAndFeelTimeChart setSubjectSelectionType(SubjectSelectionType subjectSelectionType)
    {
        clickGeneralTab();
        switch(subjectSelectionType)
        {
            case Participants:
                getWrapper().click(elementCache().participantsSelectionRadioButton);
                break;
            case Groups:
                getWrapper().click(elementCache().groupsSelectionRadioButton);
                break;
        }
        return this;
    }

    public LookAndFeelTimeChart setChartLayout(ChartLayoutType chartLayoutType)
    {
        clickGeneralTab();
        switch(chartLayoutType)
        {
            case SingleChart:
                getWrapper().click(elementCache().singleChartRadioButton);
                break;
            case PerParticipant:
                getWrapper().click(elementCache().perParticipantRadioButton);
                break;
            case PerGroup:
                getWrapper().click(elementCache().perGroupRadioButton);
                break;
            case PerMeasureDimension:
                getWrapper().click(elementCache().perMeasureDimensionRadioButton);
                break;
        }
        return this;
    }

    public LookAndFeelTimeChart checkShowMean()
    {
        getWrapper()._ext4Helper.checkCheckbox(elementCache().showMeanCheckbox);
        return this;
    }

    public LookAndFeelTimeChart uncheckShowMean()
    {
        getWrapper()._ext4Helper.uncheckCheckbox(elementCache().showMeanCheckbox);
        return this;
    }

    public LookAndFeelTimeChart checkShowIndividualLines()
    {
        getWrapper()._ext4Helper.checkCheckbox(elementCache().showIndividualLinesCheckbox);
        return this;
    }

    public LookAndFeelTimeChart uncheckIndividualLines()
    {
        getWrapper()._ext4Helper.uncheckCheckbox(elementCache().showIndividualLinesCheckbox);
        return this;
    }

    public LookAndFeelTimeChart clickYAxisLeftTab()
    {
        clickTab(elementCache().yAxisLeftTab);
        return this;
    }

    public LookAndFeelTimeChart setYAxisLeftRangeType(RangeType rangeType)
    {
        clickYAxisLeftTab();
        setRangeType(rangeType);
        return this;
    }

    public LookAndFeelTimeChart setYAxisLeftRangeMinMax(String min, String max)
    {
        clickYAxisLeftTab();
        setRangeType(RangeType.Manual);
        setRangeMin(min);
        setRangeMax(max);
        return this;
    }

    public LookAndFeelTimeChart setYAxisLeftScale(ScaleType scaleType)
    {
        clickYAxisLeftTab();
        setScaleType(scaleType);
        return this;
    }

    public LookAndFeelTimeChart setYAxisLeftLabel(String label)
    {
        clickYAxisLeftTab();
        setLabel(label);
        return this;
    }

    public LookAndFeelTimeChart clickYAxisRightTab()
    {
        clickTab(elementCache().yAxisRightTab);
        return this;
    }

    public LookAndFeelTimeChart setYAxisRightRangeType(RangeType rangeType)
    {
        clickYAxisRightTab();
        setRangeType(rangeType);
        return this;
    }

    public LookAndFeelTimeChart setYAxisRightRangeMinMax(String min, String max)
    {
        clickYAxisRightTab();
        setRangeType(RangeType.Manual);
        setRangeMin(min);
        setRangeMax(max);
        return this;
    }

    public LookAndFeelTimeChart setYAxisRightScale(ScaleType scaleType)
    {
        clickYAxisRightTab();
        setScaleType(scaleType);
        return this;
    }

    public LookAndFeelTimeChart setYAxisRightLabel(String label)
    {
        clickYAxisRightTab();
        setLabel(label);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    class ElementCache extends ChartLayoutDialog.ElementCache
    {
        public WebElement hideDataPointsCheckbox = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Hide Data Points:']/../input"), this);
        public WebElement hideDataPointsCheckboxValue = new LazyWebElement(Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Hide Data Points:']/ancestor::table"), this);
        public Locator participantsSelectionRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Participants']/preceding-sibling::input[@type='button']");
        public Locator groupsSelectionRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='Participant Groups']/preceding-sibling::input[@type='button']");
        public Locator.XPathLocator showMeanCheckbox = Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Mean']/../input");
        public Locator.XPathLocator showIndividualLinesCheckbox = Locator.xpath(VISIBLE_PANEL_XPATH + "//td//label[text()='Show Individual Lines']/../input");
        public Locator singleChartRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='One Chart']/preceding-sibling::input[@type='button']");
        public Locator perParticipantRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='One Per Participant']/preceding-sibling::input[@type='button']");
        public Locator perGroupRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='One Per Group']/preceding-sibling::input[@type='button']");
        public Locator perMeasureDimensionRadioButton = Locator.xpath(VISIBLE_PANEL_XPATH + "//label[text()='One Per Measure/Dimension']/preceding-sibling::input[@type='button']");
        public Locator visibleYAxisLeftTab = Locator.xpath("//div[contains(@class, 'item')][text()='Y-Axis (Left)']");
        public WebElement yAxisLeftTab = new LazyWebElement(visibleYAxisLeftTab, this);
        public WebElement yAxisRightTab = new LazyWebElement(Locator.xpath("//div[contains(@class, 'item')][text()='Y-Axis (Right)']"), this);
    }

    public enum SubjectSelectionType
    {
        Participants,
        Groups
    }

    public enum ChartLayoutType
    {
        SingleChart,
        PerParticipant,
        PerGroup,
        PerMeasureDimension
    }
}