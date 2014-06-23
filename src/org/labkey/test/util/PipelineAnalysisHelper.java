package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineAnalysisHelper
{
    private BaseWebDriverTest _test;

    private static int expectedJobCount = 0;

    public PipelineAnalysisHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    @LogMethod
    public void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties)
    {
        runPipelineAnalysis(importAction, files, protocolProperties, "Analyze", true);
    }

    public static void resetExpectedJobCount()
    {
        expectedJobCount = 0;
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
        {
            _test.clickButton(analyzeButtonText);
            expectedJobCount++;
        }
        else
            _test.clickButton(analyzeButtonText, 0);
    }

    public String jobDescription(String pipelineName, String protocolName)
    {
        return "R pipeline script: " + pipelineName + " - " + protocolName;
    }

    @LogMethod
    public void verifyPipelineAnalysis(@LoggedParam String pipelineName, String protocolName, File fileRoot, Map<String, Set<String>> expectedFilesAndContents)
    {
        String analysisPath = "/" + pipelineName + "/" + protocolName + "/";

        _test.goToModule("Pipeline");
        _test.waitForPipelineJobsToComplete(expectedJobCount, jobDescription(pipelineName, protocolName), false);

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
                String actualFileContents = BaseWebDriverTest.getFileContents(actualFile);
                for (String fileContent : fileContents)
                {
                    assertTrue("File didn't contain expected text:" + fileContent, actualFileContents.contains(fileContent));
                }
            }
        }
    }
}
