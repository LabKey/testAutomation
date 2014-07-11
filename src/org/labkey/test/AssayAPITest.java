/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.APIAssayHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Category({DailyA.class})
public class AssayAPITest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    @Test
    public void testSteps() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(new File(TestFileUtils.getSampledataPath() + "/AssayAPI/XLS Assay.xar.xml"), ++pipelineCount, "XLS Assay",
                new File(TestFileUtils.getSampledataPath() + "/GPAT/" + runName),  runName, new String[] {"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));
//
        goToProjectHome();

        //Issue 16073
        importAssayAndRun(new File(TestFileUtils.getSampledataPath() + "/AssayAPI/BatchPropRequired.xar"), ++pipelineCount, "BatchPropRequired",
                new File(TestFileUtils.getSampledataPath() + "/GPAT/" + runName),   "trial01-1.xls", new String[] {"K770K3VY-19"});
        waitForElement(Locator.paginationText(1, 100, 201));
//        _assayHelper.getCurrentAssayNumber();
    }

    protected void  importAssayAndRun(File assayPath, int pipelineCount, String assayName, File runPath,
                                      String runName, String[] textToCheck) throws IOException, CommandException
    {
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount);
        assayHelper.importAssay(assayName, runPath, getProjectName(), Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo"));

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
