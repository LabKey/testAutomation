/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import java.io.File;
import java.io.IOException;

abstract public class BaseFlowTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "FlowVerifyProject";
    protected static final String FOLDER_NAME = "flowFolder";
    protected static final String PIPELINE_PATH = "/sampledata/flow";

    public String getAssociatedModuleDirectory()
    {
        return "flow";
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
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Flow", null);
    }

    protected void gotoProjectQuery()
    {
        beginAt("/query/" + PROJECT_NAME + "/begin.view?schemaName=flow");
    }

    protected void createQuery(String container, String name, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=flow";
        beginAt(queryURL);
        clickImageWithAltText("Create New Query");
        setFormElement("ff_newQueryName", name);
        clickButtonWithImgSrc("Create%20and%20edit%20SQL");
        setFormElement("ff_queryText", sql);
        setFormElement("ff_metadataText", xml);
        clickButtonWithImgSrc("Save");
        if (inheritable)
        {
            beginAt(queryURL);
            clickImageWithAltText("Properties " + name);
            selectOptionByValue("ff_inheritable", "true");
            submit();
        }
        beginAt(queryURL);
    }

    protected void importAnalysis(String containerPath, String workspacePath, String fcsPath, String analysisName)
    {
        clickLinkWithText("Import FlowJo Workspace Analysis");
        assertTitleEquals("Import Analysis: " + containerPath);
        clickNavButton("Begin");

        assertTitleEquals("Import Analysis: Upload Workspace: " + containerPath);
        selectTreeItem("tree", workspacePath);
//        assertFormElementEquals("workspace.path", workspacePath);
        clickNavButton("Next");

        assertTitleEquals("Import Analysis: Associate FCS Files: " + containerPath);
        if (fcsPath != null)
            selectTreeItem("tree", fcsPath);
        else
            clearTreeSelections("tree");
//        assertFormElementEquals("runFilePathRoot", "");
        clickNavButton("Next");

        assertTitleEquals("Import Analysis: Choose Analysis Folder: " + containerPath);
        setFormElement("newAnalysisName", analysisName);

        clickNavButton("Next");
        assertTitleEquals("Import Analysis: Confirm: " + containerPath);
        // XXX: check confim page
        clickButtonWithImgSrc("Finish");
        waitForPipeline(containerPath);
    }

    protected void selectTreeItem(String treeCmpId, String path)
    {
        String result = selenium.getEval(
                "var ext = selenium.browserbot.getCurrentWindow().Ext;\n" +
                "var tree = ext.getCmp('" + treeCmpId + "');\n" +
                "tree.selectPath('/<root>' + '"+ path + "', 'text');\n" +
                "\"OK\"");
        assertEquals("OK", result);
        sleep(500);
    }

    protected void expandTreeItem(String treeCmpId, String path)
    {
        selenium.getEval(
                "var ext = selenium.browserbot.getCurrentWindow().Ext;\n" +
                "var tree = ext.getCmp('" + treeCmpId + "');\n" +
                "tree.expandPath('/<root>' + '"+ path + "', 'text');");
        sleep(500);
    }

    protected void clearTreeSelections(String treeCmpId)
    {
        selenium.getEval(
                "var ext = selenium.browserbot.getCurrentWindow().Ext;\n" +
                "var tree = ext.getCmp('" + treeCmpId + "');\n" +
                "tree.getSelectionModel().clearSelections();");
    }

}
