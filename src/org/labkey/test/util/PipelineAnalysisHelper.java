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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineAnalysisHelper
{
    private BaseWebDriverTest _test;

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

        assertEquals("Wrong file(s)", fileString.toString(), _test.getText(Locator.id("fileStatus")));
        for (Map.Entry<String, String> property : protocolProperties.entrySet())
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
        PipelineStatusTable jobStatus = new PipelineStatusTable(_test, true, true);
        jobStatus.clickStatusLink(0);
        _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Status')]/../td[2]"), "COMPLETE");

        if (runName != null)
        {
            _test.click(Locator.linkWithText("Run"));

            _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Name')]/../td[2]"), runName);
            if (description != null)
                _test.assertElementContains(Locator.xpath("//tr/td[contains(text(), 'Comments')]/../td[2]"), description);
        }

        _test.goToModule("FileContent");

        _test._fileBrowserHelper.selectFileBrowserItem(analysisPath);
        _test.assertElementNotPresent(FileBrowserHelper.Locators.gridRowWithNodeId(".work"));

        for (Map.Entry fileAndContents : expectedFilesAndContents.entrySet())
        {
            String filePath = analysisPath + fileAndContents.getKey();
            Set<String> fileContents = (Set<String>)fileAndContents.getValue();
            _test.log("Verify " + filePath);
            _test._fileBrowserHelper.selectFileBrowserItem(analysisPath + fileAndContents.getKey());
            File actualFile = new File(fileRoot, filePath);
            if (!fileContents.isEmpty())
            {
                String actualFileContents = TestFileUtils.getFileContents(actualFile);
                for (String fileContent : fileContents)
                {
                    assertTrue("File didn't contain expected text:" + fileContent, actualFileContents.contains(fileContent));
                }
            }
        }
    }
}
