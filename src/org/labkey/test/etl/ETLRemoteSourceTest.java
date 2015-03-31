package org.labkey.test.etl;

import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.ETL;
import org.labkey.test.util.RemoteConnectionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: tgaluhn
 * Date: 3/25/2015
 *
 * This should always be a separate class than the main ETLTest, as the test cases here rely on a study import.
 * Having that import as part of the setup for all the ETL tests adds substantial unnecessary overhead time.
 *
 */
@Category({DailyB.class, Data.class, ETL.class})
public class ETLRemoteSourceTest extends ETLBaseTest
{
    private static final String TRANSFORM_REMOTE = "{simpletest}/remote";
    private static final String TRANSFORM_REMOTE_DESC = "Remote Test";
    private static final String TRANSFORM_REMOTE_CONNECTION = "EtlTest_RemoteConnection";
    private static final String TRANSFORM_REMOTE_BAD_DEST = "{simpletest}/remoteInvalidDestinationSchemaName";
    private static final String TRANSFORM_REMOTE_NOTRUNC = "{simpletest}/remote_noTruncate";
    private static final File TRANSFORM_REMOTE_STUDY = new File(TestFileUtils.getSampledataPath(), "dataintegration/ETLTestStudy.zip");


    @Nullable
    @Override
    protected String getProjectName()
    {
        return "ETLRemoteSourceTestProject";
    }

    @BeforeClass
    public static void setupProject()
    {
        ETLRemoteSourceTest init = (ETLRemoteSourceTest) getCurrentTest();

        init.doSetup();
    }

    @Override
    protected void doSetup()
    {
        super.doSetup();
        _containerHelper.enableModule("Study");
        //
        // import our study (used by the remote transform tests).  This study is a continuous time point
        // study to use the same type of ETL source and target that one of our customer uses.
        //
        clickTab("Study");
        importStudyFromZip(TRANSFORM_REMOTE_STUDY, true /*ignore query validation*/);
    }

    @Before
    public void preTest() throws Exception
    {
        deleteRemoteConnection();
        _etlHelper.cleanupTestTables();
        goToProjectHome();
    }

    @After
    public void postTest()
    {
        deleteRemoteConnection();
        resetErrors();
    }

    private void createRemoteConnection()
    {
        RemoteConnectionHelper rconnHelper = new RemoteConnectionHelper(this);
        rconnHelper.createConnection(TRANSFORM_REMOTE_CONNECTION, getBaseURL(), getProjectName());
    }

    private void deleteRemoteConnection()
    {
        RemoteConnectionHelper rconnHelper = new RemoteConnectionHelper(this);
        rconnHelper.deleteConnection(TRANSFORM_REMOTE_CONNECTION);
    }

    //
    // test a "remote" ETL that transfers data into datasets (instead of just base tables)
    //
    @Test
    public void verifyRemoteTransform()
    {
        // bump our pipeline job count since we used the pipeline to import the study
        _etlHelper.incrementJobsCompleteCount();
        //
        // prepare our "remote" source dataset
        //
        _etlHelper.gotoDataset("ETL Source");
        for (int i = 1; i < 4; i++)
        {
            _etlHelper.insertDatasetRow(String.valueOf(i), "Subject " + String.valueOf(i));
        }

        //
        // create our remote connection
        //
        createRemoteConnection();

        //
        // run the remote transform again.  At the end of this we should have one summary entry for the
        // most recently run transform and then two history entries (1 error, 1 complete)
        //
        _etlHelper.runETL(TRANSFORM_REMOTE);
        // note we do expect an error in the pipeline log from the above failure
        _etlHelper.checkRun(true /*expect error*/);
        _etlHelper.addTransformResult(TRANSFORM_REMOTE, "1", "COMPLETE", "3");
        _etlHelper.assertInDatasetTarget1("Subject 1", "Subject 2", "Subject 3");
        _etlHelper.verifyTransformSummary();
        _etlHelper.verifyTransformHistory(TRANSFORM_REMOTE, TRANSFORM_REMOTE_DESC);
    }

    @Test
    public void RemoteTransformErrorTest()
    {
        List<String> errors = new ArrayList<String>();
        //run remote etl without remote connection configured
        errors.add("ERROR: The remote connection EtlTest_RemoteConnection has not yet been setup in the remote connection manager.  You may configure a new remote connection through the schema browser.");
        errors.add("ERROR: Error running executeCopy");
        _etlHelper.runETLandCheckErrors(TRANSFORM_REMOTE, true, false, errors);
        errors.clear();

        // create our remote connection
        createRemoteConnection();
        errors.add("ERROR: Target schema not found: study_buddy");
        errors.add("Error running executeCopy");
        _etlHelper.runETLandCheckErrors(TRANSFORM_REMOTE_BAD_DEST, true, false, errors);
        errors.clear();

        //remote etl constraint violation
        _etlHelper.insertSourceRow("12", "Patient 12", "");
        _etlHelper.runETL(TRANSFORM_REMOTE_NOTRUNC);

        //since we just moved patient 12 to etl_target, running the etl a second time should give us a constraint violation
        errors.add("AK_etltarget");
        errors.add("constraint");
        errors.add("ERROR: Error running executeCopy");
        errors.add("org.labkey.api.pipeline.PipelineJobException: Error running executeCopy");
        _etlHelper.runETLandCheckErrors(TRANSFORM_REMOTE_NOTRUNC, true, false, errors);
        errors.clear();
    }
}
