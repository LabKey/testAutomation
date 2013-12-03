/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RemoteConnectionHelperWD;

import java.io.File;

/**
 * User: Rylan
 * Date: 3/26/13
 * Time: 11:32 AM
 */
@Category({DailyB.class})
public class ETLTest extends ETLBaseTest
{
    //
    // expected results for transform UI testing
    //
    protected static final String TRANSFORM_APPEND = "{simpletest}/append";
    private static final String TRANSFORM_APPEND_DESC = "Append Test";
    private static final String TRANSFORM_BADCAST = "{simpletest}/badCast";
    private static final String TRANSFORM_BADCAST_DESC = "Bad Cast";
    private static final String TRANSFORM_BADTABLE = "{simpletest}/badTableName";
    private static final String TRANSFORM_BADTABLE_DESC = "BadTableName";
    protected static final String TRANSFORM_TRUNCATE = "{simpletest}/truncate";
    private static final String TRANSFORM_TRUNCATE_DESC = "Truncate Test";
    private static final String TRANSFORM_BYRUNID = "{simpletest}/appendIdByRun";
    private static final String TRANSFORM_REMOTE = "{simpletest}/remote";
    private static final String TRANSFORM_REMOTE_DESC = "Remote Test";
    private static final String TRANSFORM_REMOTE_CONNECTION = "EtlTest_RemoteConnection";
    private static final String TRANSFORM_REMOTE_STUDY = "/sampledata/dataintegration/ETLTestStudy.zip";
    private static final String TRANSFORM_BYRUNID_DESC = "ByRunId";
    private static final String TransformXMLsource = System.getenv("LABKEY_ROOT") + "\\build\\deploy\\modules\\simpletest\\etls";
    private static final String TransformXMLdest =  System.getenv("LABKEY_ROOT") + "\\build\\deploy\\modules\\ETLtest\\etls";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup() throws Exception
    {
        super.doCleanup(true);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        runInitialSetup(false);

        // verify we don't show any rows for this newly created
        // project (test container filter)
        verifyTransformSummary();

        //append into empty target
        insertSourceRow("0", "Subject 0", null);

        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "COMPLETE", "1");
        assertInTarget1("Subject 0");
        //checkRun();
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);

        //append into populated target
        insertSourceRow("1", "Subject 1", null);
        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "COMPLETE", "1");
        checkRun();
        assertInTarget1("Subject 0", "Subject 1");

        // verify transform summary should only have the most recent entry (i.e., doesn't change)
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);

        // rerun append and verify that no work is done
        runETL_NoWork(TRANSFORM_APPEND);

        // verify only two pipeline jobs existed since the "no work" one should not
        // have fired off a pipeline job
        checkRun();

        // verify transform summary should only have the most recent entry since
        // it should filter out "no work" rows
        verifyTransformSummary();
        // summary should be where it was as well
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC);


/*
UNDONE: need to fix the merge case

        // merge into populated target, note that "Subject 2" was inserted above to test ERROR case
        // for UI

        insertSourceRow("2", "Subject 2", null);
        runETL("merge");
        assertInTarget1("Subject 0", "Subject 1", "Subject 2");

        remove insert below when merge case is fixed
*/
        insertSourceRow("2", "Subject 2", null);
        //truncate into populated target
        deleteSourceRow("0", "1");
        runETL("truncate");
        // add a row for the 'truncate' etl - this should show up in our summary view
        addTransformResult(TRANSFORM_TRUNCATE, "1", "COMPLETE", "1");
        assertInTarget1("Subject 2");
        assertNotInTarget1("Subject 0", "Subject 1");
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_TRUNCATE, TRANSFORM_TRUNCATE_DESC);

        //copyETLfiles(TransformXMLsource, TransformXMLdest);
        //identify by run into populated target
        insertSourceRow("3", "Subject 3", "42");
        insertTransferRow("42", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        runETL("appendIdByRun");
        addTransformResult(TRANSFORM_BYRUNID, "1", "COMPLETE", "1");
        assertInTarget1("Subject 2", "Subject 3");

        // intentionally fail transform by running append again after
        // the records were modified above
        runETL("append");
        addTransformResult(TRANSFORM_APPEND, "1", "ERROR", null);
        checkRun(true /*expectError*/);
        incrementExpectedErrorCount(true);
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_APPEND, TRANSFORM_APPEND_DESC, "ERROR");

        //error logging test, casting error, note that this causes an error in the checker
        // before a pipeline job is even scheduled
        runETL_CheckerError("badCast");
        addTransformResult(TRANSFORM_BADCAST, "1", "ERROR", null);
        // verify we log the error regardless of whether the pipeline job runs or not
        verifyTransformSummary();

        assertInLog("contains value not castable to a date:");

        //error logging test, bad run table name
        // This test description is slightly off. For the "Run Now" case, the error is only in a popup to the user, not in a log file.
        runETL_CheckerError("badTableName");
        addTransformResult(TRANSFORM_BADTABLE, "1", "ERROR", null);
        assertInLog("Table not found:");

        // run tests over remote transform types
        verifyRemoteTransform();

        // be sure to check for all expected errors here so that the test won't fail on exit
        checkExpectedErrors(_expectedErrors);
    }

    protected void runInitialSetup(boolean remoteOnly)
    {
        PortalHelper portalHelper = new PortalHelper(this);
        log("running setup");
        _containerHelper.createProject(getProjectName(), null);
        _expectedErrors = 0;
        _jobsComplete = 0;

        enableModule("DataIntegration", true);
        enableModule("simpletest", true);

        if(!remoteOnly)
            enableModule("Study", true);

        portalHelper.addQueryWebPart("Source", "vehicle", "etl_source", null);
        portalHelper.addQueryWebPart("Target1", "vehicle", "etl_target", null);

        if (remoteOnly)
            return;

        portalHelper.addQueryWebPart("Transfers", "vehicle", "transfer", null);

        // UNDONE: remove when we finalize casing of table names versus views across pg and mssql
        String transformRun =  (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL) ?
                "transformrun" : "TransformRun";

        portalHelper.addQueryWebPart("TransformRun", "dataintegration", transformRun, null);
        portalHelper.addQueryWebPart("TransformHistory", "dataintegration", "TransformHistory", null);
        portalHelper.addQueryWebPart("TransformSummary", "dataintegration", "TransformSummary", null);

        //
        // import our study (used by the remote transform tests).  This study is a continuous time point
        // study to use the same type of ETL source and target that one of our customer uses.
        //
        clickTab("Study");
        importStudyFromZip(new File(getLabKeyRoot(), TRANSFORM_REMOTE_STUDY), true /*ignore query validation*/);
        // bump our pipeline job count since we used the pipeline to import the study
        _jobsComplete++;
    }

    //
    // test a "remote" ETL that transfers data into datasets (instead of just base tables)
    //
    protected void verifyRemoteTransform()
    {
        //
        // prepare our "remote" source dataset
        //
        gotoDataset("ETL Source");
        for (int i = 1; i < 4; i++)
        {
            insertDatasetRow(String.valueOf(i), "Subject " + String.valueOf(i));
        }
        clickTab("Portal");

        //
        // attempt to run the remote transform without a remote connection
        //
        runETL(TRANSFORM_REMOTE);
        checkRun(true /*expect error*/);
        addTransformResult(TRANSFORM_REMOTE, "1", "ERROR", null);
        assertNotInDatasetTarget1("Subject 1", "Subject 2", "Subject 3");
        // The error here is from the app, not the db
        incrementExpectedErrorCount(false);

        //
        // create our remote connection
        //
        RemoteConnectionHelperWD rconnHelper = new RemoteConnectionHelperWD(this);
        rconnHelper.createConnection(TRANSFORM_REMOTE_CONNECTION, getBaseURL(), getProjectName());

        //
        // run the remote transform again.  At the end of this we should have one summary entry for the
        // most recently run transform and then two history entries (1 error, 1 complete)
        //
        runETL(TRANSFORM_REMOTE);
        // note we do expect an error in the pipeline log from the above failure
        checkRun(true /*expect error*/);
        addTransformResult(TRANSFORM_REMOTE, "1", "COMPLETE", "3");
        assertInDatasetTarget1("Subject 1", "Subject 2", "Subject 3");
        verifyTransformSummary();
        verifyTransformHistory(TRANSFORM_REMOTE, TRANSFORM_REMOTE_DESC);
    }

}
