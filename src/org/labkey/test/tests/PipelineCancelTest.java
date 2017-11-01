/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyB.class})
public class PipelineCancelTest  extends BaseWebDriverTest
{
    private static final File STUDY_ZIP = TestFileUtils.getSampleData("pipelineCancel/LabkeyDemoStudy.zip");
    @Override
    protected String getProjectName()
    {
        return "Pipeline Cancel Test";
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        startImportStudyFromZip(STUDY_ZIP);

        log("Cancel import");
        waitForText("Delaying import");
        clickAndWait(Locator.linkContainingText("Delaying import"));
        clickButton("Cancel");

        log("Verify cancel succeeded");
//        waitForText("CANCELLED");
        waitForText("Attempting to cancel");
//        waitForTextWithRefresh("Interrupting job by sending interrupt request", defaultWaitForPage);
        waitForTextWithRefresh(defaultWaitForPage, "CANCELLED");

        goToProjectHome();
        assertTextPresent("This folder does not contain a study."); //part of the import will be done, but it shouldn't have gotten to participants.

    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("pipeline");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
