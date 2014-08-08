/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.etl;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: tgaluhn
 * Date: 7/25/2014
 */
@Category({DailyB.class, Data.class})
public class ETLTaskRefTaskTest extends ETLBaseTest
{
    private static final String PROJECT_NAME = "ETLTaskRefTaskProject";
    private static final String ETL = "{simpletest}/TaskRefTask";
    public static final String LOG_MESSAGE = "Log from test task";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps() throws Exception
    {
        runInitialSetup();
        verifyTask();
        checkExpectedErrors(_expectedErrors);
    }

    private void verifyTask() throws Exception
    {
        RunTransformResponse rtr = runETL_API(ETL);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        assertInEtlLogFile(rtr.getJobId(), LOG_MESSAGE);

        // "setting1" in the xml has a value of "anything". TaskrefTestTask sets it to "test" to be persisted in TransformState
        assertTrue("Setting1 was not presisted with a value of 'test'.", _diHelper.getTransformState(ETL).contains("\"setting1\":\"test\""));
    }

    private void runInitialSetup()
    {
        log("running setup");
        _containerHelper.createProject(getProjectName(), null);
        _expectedErrors = 0;
        _jobsComplete = 0;

        _containerHelper.enableModules(Arrays.asList("DataIntegration", "simpletest"));
    }
}
