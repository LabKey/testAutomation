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
public class ETLTaskrefTaskTest extends ETLBaseTest
{
    private static final String PROJECT_NAME = "ETLTaskrefTaskProject";
    private static final String ETL = "{simpletest}/TaskrefTask";
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
        assertTrue(_diHelper.getTransformState(ETL).contains("\"setting1\":\"test\""));
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
