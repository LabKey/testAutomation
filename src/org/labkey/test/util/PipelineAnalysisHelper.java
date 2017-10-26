/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineAnalysisHelper
{
    protected BaseWebDriverTest _test;

    private static int expectedJobCount = 1;

    public PipelineAnalysisHelper(BaseWebDriverTest test)
    {
        _test = test;
        expectedJobCount = 1;
    }

    @LogMethod
    public void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties)
    {
        runPipelineAnalysis(importAction, files, protocolProperties, "Analyze", true);
    }

    public static void setExpectedJobCount(int count)
    {
        expectedJobCount = count;
    }

    @LogMethod
    public void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties, String analyzeButtonText, boolean expectSuccess)
    {
        StringBuilder fileString = new StringBuilder();

        _test.goToModule("FileContent");
        for (String file : files)
        {
            _test._fileBrowserHelper.selectFileBrowserItem(file);
            if (fileString.length() > 0)
                fileString.append("\n");
            fileString.append(file.substring(file.lastIndexOf("/") + 1, file.length()));
        }
        _test._fileBrowserHelper.selectImportDataAction(importAction);

        _test.waitForElement(Locator.id("fileStatus").withText(fileString.toString()), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, false);
        assertEquals("Wrong file(s)", fileString.toString(), _test.getText(Locator.id("fileStatus")));
        for (Map.Entry<String, String> property : protocolProperties.entrySet())
        {
            if ("xmlParameters".equals(property.getKey()))
            {
                _test._extHelper.setCodeMirrorValue(property.getKey(), property.getValue());
            }
            else
            {
                WebElement protocolFormInput = Locator.id(property.getKey() + "Input").findElement(_test.getDriver());
                if (protocolFormInput.getAttribute("type").equals("checkbox"))
                {
                    boolean check = Boolean.parseBoolean(property.getValue());
                    if (check)
                        _test.checkCheckbox(protocolFormInput);
                    else
                        _test.uncheckCheckbox(protocolFormInput);
                }
                else
                {
                    _test.setFormElement(protocolFormInput, property.getValue());
                }
            }
        }

        if (expectSuccess)
            _test.clickButton(analyzeButtonText);
        else
            _test.clickButton(analyzeButtonText, 0);
    }

    public String jobDescription(String pipelineName, String protocolName)
    {
        return "R pipeline script: " + pipelineName + " - " + protocolName;
    }

    @LogMethod
    public void verifyPipelineAnalysis(@LoggedParam String pipelineName, String protocolName, @Nullable String runName, @Nullable String description, File fileRoot, Map<String, Set<String>> expectedFilesAndContents)
    {
        String analysisPath = "/" + pipelineName + "/" + protocolName + "/";

        _test.goToModule("Pipeline");
        _test.waitForPipelineJobsToComplete(expectedJobCount, jobDescription(pipelineName, protocolName), false);

        // Go to the run details graph page
        PipelineStatusTable jobStatus = new PipelineStatusTable(_test);
        jobStatus.clickStatusLink(0);

        _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Status')]/../td[2]"), "COMPLETE");

        if (runName != null)
        {
            _test.clickAndWait(Locator.linkWithText("Run"));

            _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Name')]/../td[2]"), runName);
            if (description != null)
                _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Comments')]/../td[2]"), description);
        }

        _test.goToModule("FileContent");

        _test._fileBrowserHelper.selectFileBrowserItem(analysisPath);
        _test.assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId(".work"));

        for (Map.Entry<String, Set<String>> fileAndContents : expectedFilesAndContents.entrySet())
        {
            boolean absolutePath = fileAndContents.getKey().startsWith("/");
            String filePath = (absolutePath ? "" : analysisPath) + fileAndContents.getKey();
            _test.log("Verify " + filePath);
            Set<String> fileContents = fileAndContents.getValue();
            _test._fileBrowserHelper.selectFileBrowserItem(filePath);
            File actualFile = new File(fileRoot, filePath);
            if (!fileContents.isEmpty())
            {
                String actualFileContents = TestFileUtils.getFileContents(actualFile);
                for (String fileContent : fileContents)
                {
                    assertTrue("File '" + actualFile + "' didn't contain expected text:" + fileContent, actualFileContents.contains(fileContent));
                }
            }
            else
            {
                assertTrue("Expected file doesn't exist: " + actualFile, actualFile.exists());
            }
        }
    }

    /**
     * Run a given protocol. Create it if it doesn't exist
     * @param protocolName The name of the protocol to run
     * @param protocolDef Optional bioml xml definition to use for this protocol
     * @param allowRetry  Allowing retrying the run if the Retry button is present. If false and the Retry button is present, a test failure occurs
     */
    public void runProtocol(@NotNull String protocolName, @Nullable String protocolDef, boolean allowRetry)
    {
        setProtocol(protocolName, protocolDef);
        analyzeOrRetry(allowRetry);

        _test.waitForRunningPipelineJobs(BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void analyzeOrRetry(boolean allowRetry)
    {
        Locator retryButton = Locator.lkButton("Retry");
        if (allowRetry && _test.isElementPresent(retryButton))
            _test.clickButton("Retry");
        else
        {
            _test.assertElementNotPresent(retryButton);
            _test.clickButton("Analyze");
        }
    }

    public Select waitForProtocolSelect()
    {
        Locator.XPathLocator protocolSelect = Locator.id("protocolSelect");
        _test.waitForElement(protocolSelect.append(Locator.tag("option").withText("<New Protocol>")));
        return new Select(protocolSelect.findElement(_test.getDriver()));
    }

    public Select waitForConfigurationSelect()
    {
        Locator.XPathLocator workflowConfigSelect = Locator.id("workflowConfigSelect");
        _test.waitForElement(workflowConfigSelect.append(Locator.tag("option").withText("<New Configuration>")));
        return new Select(workflowConfigSelect.findElement(_test.getDriver()));
    }

    public void setProtocol(String protocolName, String protocolDef)
    {
        // If the given protocol name already exists, select it. If not, define a new one.
        Select protocolSelect = waitForProtocolSelect();
        if (!Locator.tag("option").withText(protocolName).findElements(_test.getDriver()).isEmpty())
        {
            protocolSelect.selectByVisibleText(protocolName);
            if (null != protocolDef)
                verifySavedProtocolDef(protocolDef, _test._extHelper.getCodeMirrorValue("xmlParameters"));
        }
        else
        {
            protocolSelect.selectByVisibleText("<New Protocol>");
            _test.setFormElement(Locator.id("protocolNameInput"), protocolName);
            if (null != protocolDef)
                _test._extHelper.setCodeMirrorValue("xmlParameters", protocolDef);
        }
    }

    private void verifySavedProtocolDef(String expected, String actual)
    {
        Map<String, String> expectedParameters = loadParameterMapFromXmlString(expected);
        Map<String, String> actualParameters = loadParameterMapFromXmlString(actual);
        assertEquals("Protocol XML definition not as expected", expectedParameters, actualParameters);
    }

    private Map<String, String> loadParameterMapFromXmlString(String xml)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = builder.parse(is);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression expr = xpath.compile("//bioml//note");
            NodeList all = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            // Strictly speaking this doesn't need to be a TreeMap to do the comparison, but having the parameters
            // ordered by name will make it easier to human parse the errors.
            Map<String, String> parameters = new TreeMap<>();
            if (all != null && all.getLength() > 0)
            {
                for (int i = 0; i < all.getLength(); i++)
                {
                    parameters.put(all.item(i).getAttributes().getNamedItem("label").getNodeValue(), all.item(i).getTextContent());
                }
            }
            return parameters;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception parsing xml string " + xml, e);
        }
    }
}
