/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.util.APIAssayHelper;

import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/14/12
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssayAPITest extends BaseSeleniumWebTest
{
    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(getSampledataPath() + "/AssayAPI/XLS Assay.xar.xml", ++pipelineCount, "XLS Assay",
                getSampledataPath() + "/GPAT/" + runName,  runName, new String[] {"1 - 100 of 201", "K770K3VY-19"});
//
        goToProjectHome();

        //Issue 16073
        importAssayAndRun(getSampledataPath() + "/AssayAPI/BatchPropRequired.xar", ++pipelineCount, "BatchPropRequired",
                getSampledataPath() + "/GPAT/" + runName,   "trial01-1.xls", new String[] {"1 - 100 of 201", "K770K3VY-19"});
//        _assayHelper.getCurrentAssayNumber();
    }

    protected void  importAssayAndRun(String assayPath, int pipelineCount, String assayName, String runPath,
                                      String runName, String[] textToCheck)
    {
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount, assayName);
        try
        {
            assayHelper.importAssay(assayName, runPath, getProjectName(), Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo"));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }

        log("verify import worked");
        goToProjectHome();
        clickLinkContainingText(assayName);
        clickLinkContainingText(runName);
        assertTextPresent(textToCheck);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws Exception
    {
        deleteProject(getProjectName(), afterTest);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
