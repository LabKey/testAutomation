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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: brittp
 * Date: Nov 22, 2005
 * Time: 1:31:42 PM
 */
public class ExpTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "ExpVerifyProject";
    private static final String FOLDER_NAME = "verifyfldr";
    private static final String EXPERIMENT_NAME = "Tutorial Examples";
    private static final String EXPERIMENT_LSID = "urn:lsid:cpas.fhcrc.org:dev.Experiment.ExperimentVerify.verifyfldr:Experiment:001";
    private static final String RUN_NAME = "Example 5 Run (XTandem peptide search)";
    private static final String RUN_NAME_IMAGEMAP = "Example 5 Run (XTandem peptide search)";
    private static final String DATA_OBJECT_TITLE = "Data: CAexample_mini.mzXML";
    private static final int MAX_WAIT_SECONDS = 60*5;

    public String getAssociatedModuleDirectory()
    {
        return "experiment";
    }

    protected void doCleanup()
    {
        try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Experiment" });
        addWebPart("Data Pipeline");
        addWebPart("Run Groups");
        clickNavButton("Setup");
        setFormElement("path", getLabKeyRoot() + "/sampledata/xarfiles/expVerify");
        submit();
        clickNavButton("View Status");
        clickLinkWithText(FOLDER_NAME);
        clickNavButton("Process and Import Data");
        clickNavButton("Import Experiment");
        clickLinkWithText("Data Pipeline");
        assertLinkNotPresentWithText("ERROR");
        int seconds = 0;
        while (!isLinkPresentWithText("COMPLETE") && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            clickTab("Pipeline");
        }

        if (!isLinkPresentWithText("COMPLETE"))
            fail("Import did not complete.");

        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(EXPERIMENT_NAME);
        assertTextPresent(EXPERIMENT_LSID);
        assertTextPresent("Example 5 Run");
        clickLinkWithText(RUN_NAME);
        clickLinkWithText("graph summary view");
        clickImageMapLinkByTitle("graphmap", RUN_NAME_IMAGEMAP);
        clickImageMapLinkByTitle("graphmap", DATA_OBJECT_TITLE);
        assertTextPresent("Not available on disk");
    }
}
