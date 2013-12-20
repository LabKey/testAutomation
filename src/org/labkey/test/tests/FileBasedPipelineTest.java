/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverMultipleTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.FileBrowserHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.RReportHelperWD;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by tchadick on 12/9/13.
 */
@Category({DailyB.class})
public class FileBasedPipelineTest extends BaseWebDriverMultipleTest
{
    private static final String PIPELINETEST_MODULE = "pipelinetest";
    private static final File SAMPLE_FILE = new File(getSampledataPath(), "fileTypes/sample.txt");

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileBasedPipelineTest initTest = new FileBasedPipelineTest();
        initTest.doCleanup(false);

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest.enableModules(Arrays.asList(PIPELINETEST_MODULE, "Pipeline"), true);

        RReportHelperWD rReportHelper = new RReportHelperWD(initTest);
        rReportHelper.ensureRConfig();

        currentTest = initTest;
    }

    @Before
    public void startTest()
    {
        clickProject(getProjectName());
    }

    @Test
    public void testRCopyPipeline()
    {
        final String folderName = "rCopy";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "R Copy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
            "protocolName", protocolName,
            "protocolDescription", "");
        final Map<String, Set<String>> outputFiles = Maps.of(
            "r-copy.r", Collections.<String>emptySet(),
            "r-copy.r.Rout", Collections.<String>emptySet(),
            "r-copy.xml", Collections.<String>emptySet(),
            "sample.log", Collections.<String>emptySet(),
            "sample.xxx", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
    }

    @Test
    public void testRCopyInlinePipeline()
    {
        final String folderName = "rCopyInline";
        final String containerPath = getProjectName() + "/" + folderName;
        final File fileRoot = getDefaultFileRoot(containerPath);
        final String pipelineName = "r-copy-inline";
        final String importAction = "Use R to duplicate a file (inline script)";
        final String protocolName = "Inline R Copy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
            "protocolName", protocolName,
            "skipLines", "5");
        final Map<String, Set<String>> outputFiles = new HashMap<>();
        outputFiles.put("r-copy-inline.xml", Collections.<String>emptySet());
        outputFiles.put("script.R", Collections.<String>emptySet());
        outputFiles.put("script.Rout", Collections.<String>emptySet());
        outputFiles.put("sample.pipe.xar.xml", Collections.<String>emptySet());
        outputFiles.put("sample.log", Collections.<String>emptySet());
        outputFiles.put("sample.xxx", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        runPipelineAnalysis(importAction, targetFiles, protocolProperties, "Duplicate File(s)");
        verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
    }

    @Test (expected = NoSuchElementException.class)
    public void testRPipelineWithModuleDisabled()
    {
        final String folderName = "rPipelineDisabled";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "Inline R Copy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "protocolDescription", "");

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        disableModules("pipelinetest");

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        runPipelineAnalysis(importAction, targetFiles, protocolProperties);
    }

    @LogMethod
    private void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties)
    {
        runPipelineAnalysis(importAction, files, protocolProperties, "Analyze");
    }
    @LogMethod
    private void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties, String analyzeButtonText)
    {
        StringBuilder fileString = new StringBuilder();

        goToModule("FileContent");
        for (String file : files)
        {
            _fileBrowserHelper.selectFileBrowserItem(file);
            if (fileString.length() > 0)
                fileString.append("\n");
            fileString.append(file.substring(file.lastIndexOf("/") + 1, file.length()));
        }
        _fileBrowserHelper.selectImportDataAction(importAction);

        assertEquals("Wrong file(s)", fileString.toString(), getText(Locator.id("fileStatus")));
        for (Map.Entry<String, String> property : protocolProperties.entrySet())
        {
            setFormElement(Locator.id(property.getKey() + "Input"), property.getValue());
        }

        clickButton(analyzeButtonText);
    }

    @LogMethod
    private void verifyPipelineAnalysis(@LoggedParam String pipelineName, String protocolName, File fileRoot, Map<String, Set<String>> expectedFilesAndContents)
    {
        String analysisPath = "/" + pipelineName + "/" + protocolName + "/";

        goToModule("FileContent");

        _fileBrowserHelper.selectFileBrowserItem(analysisPath);
        assertElementNotPresent(FileBrowserHelperWD.Locators.gridRowWithNodeId(".work"));

        for (Map.Entry fileAndContents : expectedFilesAndContents.entrySet())
        {
            String filePath = analysisPath + fileAndContents.getKey();
            Set<String> fileContents = (Set<String>)fileAndContents.getValue();
            log("Verify " + filePath);
            _fileBrowserHelper.selectFileBrowserItem(analysisPath + fileAndContents.getKey());
            File actualFile = new File(fileRoot, filePath);
            String actualFileContents = getFileContents(actualFile);
            for (String fileContent : fileContents)
            {
                assertTrue("File didn't contain expected text:" + fileContent, actualFileContents.contains(fileContent));
            }
        }
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return this.getClass().getSimpleName() + " Project";
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/pipeline";
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
