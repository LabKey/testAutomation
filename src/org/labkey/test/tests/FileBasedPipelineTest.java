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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
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
            "Name", protocolName,
            "Description", "");
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

    @Test @Ignore("Inline pipeline definition is broken")
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
            "Name", protocolName,
            "Description", "");
        final Map<String, Set<String>> outputFiles = Maps.of(
            "r-copy.xml", Collections.<String>emptySet(),
            "sample.log", Collections.<String>emptySet());

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        runPipelineAnalysis(importAction, targetFiles, protocolProperties);
        verifyPipelineAnalysis(pipelineName, protocolName, fileRoot, outputFiles);
    }

    @Test @Ignore("Currenly behaves the same whether module is enabled or not")
    public void testRPipelineWithModuleDisabled()
    {
        final String folderName = "rPipelineDisabled";
        final String importAction = "Use R to duplicate a file";
        final String protocolName = "Inline R Copy";
        final String[] targetFiles = {SAMPLE_FILE.getName()};
        final Map<String, String> protocolProperties = Maps.of(
                "Name", protocolName,
                "Description", "");

        _containerHelper.createSubfolder(getProjectName(), folderName, null);
        disableModules("pipelinetest");

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(SAMPLE_FILE);

        runPipelineAnalysis(importAction, targetFiles, protocolProperties);
    }

    @LogMethod
    private void runPipelineAnalysis(@LoggedParam String importAction, String[] files, Map<String, String> protocolProperties)
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
            setFormElement(Locator.id("protocol" + property.getKey() + "Input"), property.getValue());
        }

        clickButton("Analyze");
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
