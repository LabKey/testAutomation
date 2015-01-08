/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;

/**
 * User: tgaluhn
 * Date: 1/4/2015
 *
 * Verify multiple transaction boundaries are used when specifying a transactionSize in the etl descriptor
 */
@Category({DailyB.class, Data.class})
public class ETLMultipleTransactionsTest extends ETLBaseTest
{
    private static final String PROJECT_NAME = "ETLMultipleTransactionsTestProject";
    private static final String ETL = "{simpletest}/multipleTransactions";

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
        String jobId = runETL_API(ETL).getJobId();
        incrementExpectedErrorCount();

        if (WebTestHelper.getDatabaseType().equals(WebTestHelper.DatabaseType.PostgreSQL))
        {
            // See issue 22213; we don't support specifying a transaction size on Postgres when source
            // and target are the same datasource. Make sure the error message happens.
            // Note: it will actually work for small datasets like that used in this test, but larger datasets will give
            // errors from pg and leave the target in a bad state.
            assertInEtlLogFile(jobId, "not supported on Postgres");
        }
        else
        {
            assertInEtlLogFile(jobId, "Target transactions will be committed every 2 rows", "Could not convert 'uhoh' for field rowid");
            goToProjectHome();
            assertInTarget2("xact1 1", "xact1 2");
        }
    }

    private void runInitialSetup()
    {
        doCleanup(false);
        PortalHelper portalHelper = new PortalHelper(this);
        log("running setup");
        _containerHelper.createProject(getProjectName(), null);
        _expectedErrors = 0;
        _jobsComplete = 0;

        _containerHelper.enableModules(Arrays.asList("DataIntegration", "simpletest"));
        portalHelper.addQueryWebPart("Target2", "vehicle", "etl_target2", null);
    }
}
