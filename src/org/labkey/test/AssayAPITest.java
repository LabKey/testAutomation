/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import java.io.File;
import java.util.Collections;

/**
 * User: elvan
 * Date: 9/14/12
 * Time: 2:06 PM
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
        importAssayAndRun(new File(getSampledataPath() + "/AssayAPI/XLS Assay.xar.xml"), ++pipelineCount, "XLS Assay",
                new File(getSampledataPath() + "/GPAT/" + runName),  runName, new String[] {"1 - 100 of 201", "K770K3VY-19"});
//
        goToProjectHome();

        //Issue 16073
        importAssayAndRun(new File(getSampledataPath() + "/AssayAPI/BatchPropRequired.xar"), ++pipelineCount, "BatchPropRequired",
                new File(getSampledataPath() + "/GPAT/" + runName),   "trial01-1.xls", new String[] {"1 - 100 of 201", "K770K3VY-19"});
//        _assayHelper.getCurrentAssayNumber();
    }

    protected void  importAssayAndRun(File assayPath, int pipelineCount, String assayName, File runPath,
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
        clickAndWait(Locator.linkContainingText(assayName));
        clickAndWait(Locator.linkContainingText(runName));
        assertTextPresent(textToCheck);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/assay";
    }
}
