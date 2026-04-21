/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
package org.labkey.test.pipeline;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.selenium.ScrollUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.xq;

/**
 * <code>ExperimentGraph</code>
 */
public class ExperimentGraph extends WebDriverComponent<Component<?>.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    public ExperimentGraph(BaseWebDriverTest test)
    {
        _driver = test.getDriver();
        _el = Locator.id("graph_root").parent().findElement(test.getDriver());
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public void clickLink(String link)
    {
        ScrollUtils.scrollIntoViewPort(getComponentElement());
        WebElement linkEl = svgLinkByTitle(link);
        // It is challenging to scroll SVG elements to the correct part of the page. Just navigate.
        getWrapper().beginAt(linkEl.getAttribute("xlink:href"));
    }

    public void clickInputLink(String input)
    {
        clickLink(getInputLinkText(input));
    }

    public void clickOutputLink(String output)
    {
        clickLink(getOutputLinkText(output));
    }

    public String getInputLinkText(String input)
    {
        return "Data: " + input;
    }

    public String getOutputLinkText(String output)
    {
        return "Data: " + output + " (Run Output)";
    }

    public boolean isNodePresent(String link)
    {
        return svgLinkByTitle(link).isDisplayed();
    }

    public boolean isInputPresent(String input)
    {
        return isNodePresent(getInputLinkText(input));
    }

    public boolean isOutputPresent(String input)
    {
        return isNodePresent(getOutputLinkText(input));
    }

    public void assertNodePresent(String link)
    {
        assertTrue("Missing node in experiment graph " + link, isNodePresent(link));
    }

    public void assertInputPresent(String input)
    {
        assertTrue("Missing input in experiment graph " + input, isInputPresent(input));
    }

    public void assertOutputPresent(String output)
    {
        assertTrue("Missing output in experiment graph " + output, isOutputPresent(output));
    }

    public void validate(PipelineTestParams tp)
    {
        String[] names = tp.getExperimentLinks();
        for (String name : names)
        {
            assertNodePresent(name);
            String baseName = getBaseName(tp);
            assertInputPresent(tp.getParametersFile());
            for (String inputExt : tp.getInputExtensions())
                assertInputPresent(baseName + inputExt);
            for (String outputExt : tp.getOutputExtensions())
                assertOutputPresent(baseName + outputExt);
        }
    }

    private String getBaseName(PipelineTestParams tp)
    {
        String[] sampleNames = tp.getSampleNames();
        for (String sampleName : sampleNames)
        {
            if (getWrapper().isTextPresent(sampleName))
                return sampleName;
        }

        // AbstractMS2SearchProtocol.getJoinedBaseName() is hard-coded to use "all"
        // Probably fail later, but simpler than checking for null return.
        return "all";
    }

    private WebElement svgLinkByTitle(String title)
    {
        // Funky locator to find element within an SVG
        return Locator.xpath(".//*[local-name()='a'][@*[local-name()='title']=" + xq(title) + "]").findWhenNeeded(_el);
    }

}
