package org.labkey.test.components;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RyanS on 12/15/2014.
 */
public class D3PieChart
{
    protected BaseWebDriverTest _test;
    protected String _parentDivXpath; //class of div containing svg element

    public D3PieChart(BaseWebDriverTest test, String divClass)
    {
        _test = test;
        _parentDivXpath = "//div[@class='" + divClass + "']";
    }

    public List<String> getSegmentOuterLabels()
    {
        List<String> labels = new ArrayList<>();
        Locator.XPathLocator outerLabelXpath = Locator.xpath(_parentDivXpath + "//*[contains(@class,'labelGroup-inner')]//*");
        List<WebElement> elements = outerLabelXpath.findElements(_test.getDriver());
        for(WebElement element : elements)
        {
            labels.add(element.getText());
        }
        return labels;
    }

    public List<String> getSegmentInnerLabels()
    {
        List<String> labels = new ArrayList<>();
        Locator.XPathLocator outerLabelXpath = Locator.xpath(_parentDivXpath + "//*[contains(@class,'labelGroup-outer')]//*");
        List<WebElement> elements = outerLabelXpath.findElements(_test.getDriver());
        for(WebElement element : elements)
        {
            labels.add(element.getText());
        }
        return labels;
    }

    public List<String> getSegmentFillColors()
    {
        List<String> colors = new ArrayList<>();
        Locator.XPathLocator segmentLocator = Locator.xpath(_parentDivXpath + "//*[contains(@class,'arc')]//*");
        List<WebElement> elements = segmentLocator.findElements(_test.getDriver());
        for(WebElement element : elements)
        {
            colors.add(element.getAttribute("fill"));
        }
        return colors;
    }

    public String getCaption()
    {
        return Locator.divByClassContaining("caption").findElement(_test.getDriver()).getText();
    }

    public String getOuterLabelByInnerLabel(String innerLabel)
    {
        List<String> innerLabels = getSegmentInnerLabels();
        List<String> outerLabels = getSegmentOuterLabels();
        int index = innerLabels.indexOf(innerLabel);
        return outerLabels.get(index);
    }

    public void clickSegmentByInnerLabel(String innerLabel)
    {
        List<String> labels = getSegmentInnerLabels();
        int labelIndex = labels.indexOf(innerLabel);
        clickSegmentByIndex(labelIndex);
    }

    public void clickSegmentByOuterLabel(String outerLabel)
    {
        List<String> labels = getSegmentOuterLabels();
        int labelIndex = labels.indexOf(outerLabel);
        clickSegmentByIndex(labelIndex);
    }

    public void clickSegmentByIndex(int index)
    {
        List<WebElement> segments = Locator.xpath(_parentDivXpath + "//*[contains(@class,'arc')]//*").findElements(_test.getDriver());
        segments.get(index).click();
    }


}
