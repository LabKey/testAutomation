/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.test;

import org.apache.commons.io.FileUtils;
import org.labkey.test.util.ExtHelper;

import java.io.File;
import java.io.IOException;

abstract public class BaseFlowTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "Flow Verify Project";
    protected static final String PIPELINE_PATH = "/sampledata/flow";

    public String getAssociatedModuleDirectory()
    {
        return "flow";
    }

    protected void setFlowPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath);
    }

    protected File getPipelineWorkDirectory()
    {
        return new File(getLabKeyRoot() + "/sampledata/flow/work");
    }

    protected void deletePipelineWorkDirectory()
    {
        File dir = getPipelineWorkDirectory();
        if (dir.exists())
        {
            try
            {
                FileUtils.deleteDirectory(dir);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    protected void waitForPipeline(String containerPath)
    {
        pushLocation();
        beginAt("/Flow" + containerPath + "/showJobs.view");
        while(!isTextPresent("There are no running or pending flow jobs"))
        {
            sleep(2000);
            refresh();
        }
        popLocation(longWaitForPage);
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        deletePipelineWorkDirectory();
        try
        {
            beginAt("/admin/begin.view");
            clickLinkWithText("flow cytometry");
            setFormElement("workingDirectory", "");
            clickNavButton("update");
        }
        catch (Throwable t) {}
    }

    protected void init()
    {
        beginAt("/admin/begin.view");
        clickLinkWithText("flow cytometry");
        deletePipelineWorkDirectory();
        setFormElement("workingDirectory", getPipelineWorkDirectory().toString());
        clickNavButton("update");
        assertTextPresent("Path does not exist");
        getPipelineWorkDirectory().mkdir();
        setFormElement("workingDirectory", getPipelineWorkDirectory().toString());
        clickNavButton("update");
        assertTextNotPresent("Path does not exist");
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, getFolderName(), "Flow", null);
    }

    protected String getFolderName()
    {
        return getClass().getSimpleName();
    }

    protected void gotoProjectQuery()
    {
        beginAt("/query/" + PROJECT_NAME + "/begin.view?schemaName=flow");
    }

    protected void createQuery(String container, String name, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=flow";
        beginAt(queryURL);
        createNewQuery("flow");
        setFormElement("ff_newQueryName", name);
        clickNavButton("Create and Edit Source");
        setFormElement("ff_queryText", sql);
        setFormElement("ff_metadataText", xml);
        clickNavButton("Save");
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties("flow", name);
            selectOptionByValue("inheritable", "true");
            submit();
        }
        beginAt(queryURL);
    }

    protected void importAnalysis(String containerPath, String workspacePath, String fcsPath, String analysisName, boolean viaPipeline)
    {
        importAnalysis(containerPath, workspacePath, fcsPath, false, analysisName, false, viaPipeline);
    }

    protected void importAnalysis(String containerPath, String workspacePath,
                                  String fcsPath, boolean existingKeywordRun,
                                  String analysisName, boolean existingAnalysisFolder, boolean viaPipeline)
    {
        if (viaPipeline)
        {
            importAnalysis_viaPipeline(workspacePath);
        }
        else
        {
            importAnalysis_begin(containerPath);
            importAnalysis_uploadWorkspace(containerPath, workspacePath);
        }
        importAnalysis_FCSFiles(containerPath, fcsPath, existingKeywordRun);
        if (fcsPath == null)
        {
            assertFormElementEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            assertFormElementEquals(Locator.name("runFilePathRoot"), "");
        }
        else
        {
            if (existingKeywordRun)
                assertFormElementNotEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            else
                assertFormElementEquals(Locator.name("existingKeywordRunId"), String.valueOf(0));
            assertFormElementNotEquals(Locator.name("runFilePathRoot"), "");
        }

        importAnalysis_analysisFolder(containerPath, analysisName, existingAnalysisFolder);

        importAnalysis_confirm(containerPath, workspacePath, fcsPath, existingKeywordRun, analysisName, existingAnalysisFolder);
    }

    protected void importAnalysis_viaPipeline(String workspacePath)
    {
        log("browse pipeline to begin import analysis wizard");
        if (!selenium.getTitle().startsWith("Flow Dashboard:"))
            clickLinkWithText("Flow Dashboard");
        clickLinkContainingText("FCS files to be imported");

        if (workspacePath.startsWith("/"))
            workspacePath = workspacePath.substring(1);
        String[] parts = workspacePath.split("/");

        for (int i = 0; i < parts.length; i++)
        {
            if (i == parts.length - 1)
            {
                // workaround for import button enable/disable state bug in pipeline browser
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
                ExtHelper.clickFileBrowserFileCheckbox(this, parts[i]);
            }
            else
                waitAndClick(Locator.fileTreeByName(parts[i]));
        }

        selectImportDataAction("Import FlowJo Workspace");
        waitForPageToLoad();
    }

    protected void importAnalysis_begin(String containerPath)
    {
        log("begin import analysis wizard");
        if (!selenium.getTitle().startsWith("Flow Dashboard:"))
            clickLinkWithText("Flow Dashboard");
        clickLinkWithText("Import FlowJo Workspace Analysis");
        assertTitleEquals("Import Analysis: Start: " + containerPath);
        clickNavButton("Begin");
    }

    protected void importAnalysis_uploadWorkspace(String containerPath, String workspacePath)
    {
        assertTitleEquals("Import Analysis: Upload Workspace: " + containerPath);
        sleep(500);
        selectTreeItem(workspacePath);
//        assertFormElementEquals("workspace.path", workspacePath);
        clickNavButton("Next");
    }

    protected void importAnalysis_FCSFiles(String containerPath, String fcsPath, boolean existingRun)
    {
        assertTitleEquals("Import Analysis: Associate FCS Files: " + containerPath);
        if (existingRun)
        {
            selectOptionByText("existingKeywordRunId", fcsPath);
            clickNavButton("Next");
        }
        else if (fcsPath != null)
        {
            selectTreeItem(fcsPath);
            clickNavButton("Next");
        }
        else
        {
            setFormElement(Locator.name("runFilePathRoot"), ""); // XXX: clicking the Skip button doesn't clear selections
            clickNavButton("Skip");
        }
    }

    protected void importAnalysis_analysisFolder(String containerPath, String analysisName, boolean existing)
    {
        assertTitleEquals("Import Analysis: Choose Analysis Folder: " + containerPath);
        if (existing)
        {
            selectOptionByText("existingAnalysisId", analysisName);
        }
        else
        {
            setFormElement("newAnalysisName", analysisName);
        }
        clickNavButton("Next");
    }

    protected void importAnalysis_confirm(String containerPath, String workspacePath,
                                          String fcsPath, boolean existingKeywordRun,
                                          String analysisFolder, boolean existingAnalysisFolder)
    {
        assertTitleEquals("Import Analysis: Confirm: " + containerPath);
        assertTextPresent("Analysis Folder: " + analysisFolder);
        if (existingKeywordRun)
            assertTextNotPresent("Existing FCS File run: none set");
        // XXX: assert fcsPath is present: need to normalize windows path backslashes
        if (fcsPath == null)
            assertTextPresent("FCS File Path: none set");
        assertTextPresent("Workspace: " + workspacePath);
        
        clickNavButton("Finish");
        waitForPipeline(containerPath);
        log("finished import analysis wizard");
    }

    protected void selectTreeItem(String path)
    {
        log("selectTreeItem path: " + path);
        if (path.startsWith("/"))
            path = path.substring(1);
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++)
        {
            if (i == parts.length - 1)
            {
                // select last item: click on tree node name
                ExtHelper.selectFileBrowserFile(this, parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                waitAndClick(Locator.fileTreeByName(parts[i]));
            }
        }
    }

}
