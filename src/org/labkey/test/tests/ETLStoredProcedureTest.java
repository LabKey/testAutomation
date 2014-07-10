/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@Category({DailyB.class, Data.class})
public class ETLStoredProcedureTest extends ETLTest
{
    private static final String TRANSFORM_NORMAL_OPERATION_SP = "{simpletest}/SProcNormalOperation";
    private static final String TRANSFORM_BAD_NON_ZERO_RETURN_CODE_SP = "{simpletest}/SProcBadNonZeroReturnCode";
    private static final String TRANSFORM_BAD_PROCEDURE_NAME_SP = "{simpletest}/SProcBadProcedureName";
    private static final String TRANSFORM_BAD_MISSING_TRANSFORM_RUN_ID_PARM_SP = "{simpletest}/SProcBadMissingTransformRunIdParm";
    private static final String TRANSFORM_BAD_THROW_ERROR_SP = "{simpletest}/SProcBadThrowError";
    private static final String TRANSFORM_PERSISTED_PARAMETER_SP = "{simpletest}/SProcPersistedParameter";
    private static final String TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP = "{simpletest}/SProcOverridePersistedParameter";
    private static final String TRANSFORM_RUN_FILTER_SP = "{simpletest}/SProcRunFilter";
    private static final String TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP = "{simpletest}/SProcModifiedSinceNoSource";
    private static final String TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP = "{simpletest}/SProcModifiedSinceWithSource";
    private static final String TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP = "{simpletest}/SProcBadModifiedSinceWithBadSource";

    @Test
    public void testSteps() throws Exception
    {

        // TODO: only need a subset of the initialSetup operations
        runInitialSetup(false);

        verifyStoredProcTransform();

        // be sure to check for all expected errors here so that the test won't fail on exit
        checkExpectedErrors(_expectedErrors);

    }

    protected void verifyStoredProcTransform() throws Exception
    {
        /*
        Test modes as mapped in the proc etlTest
        1	normal operation
        2	return code > 0
        3	raise error
        4	input/output parameter persistence
        5	override of persisted input/output parameter
        6	Run filter strategy, require @filterRunId
        7   Modified since filter strategy, no source, require @filterStartTimeStamp & @filterEndTimeStamp,
            populated from output of previous run
        8	Modified since filter strategy with source, require @filterStartTimeStamp & @filterEndTimeStamp
            populated from the filter strategy IncrementalStartTime & IncrementalEndTime
        */

        // All tests use the API
        RunTransformResponse rtr;

        // test mode 1, Normal operation
        rtr = runETL_API(TRANSFORM_NORMAL_OPERATION_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        assertInEtlLogFile(rtr.getJobId(), "Test print statement logging");
        assertInEtlLogFile(rtr.getJobId(), "Test returnMsg logging");

        // test mode 2, return code > 0, should be an error
        rtr = runETL_API(TRANSFORM_BAD_NON_ZERO_RETURN_CODE_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        incrementExpectedErrorCount();
        // That should have an error ERROR: Error: Sproc exited with return code 1

        // try to run a non-existent proc
        rtr = runETL_API(TRANSFORM_BAD_PROCEDURE_NAME_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        incrementExpectedErrorCount();
        // Error on missing procedure or @transformRunId; over api that didn't create a job

        // try to run a proc missing the required transformRunId param
        rtr = runETL_API(TRANSFORM_BAD_MISSING_TRANSFORM_RUN_ID_PARM_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        incrementExpectedErrorCount();
        // Error on missing procedure or @transformRunId

        // test mode 3, raise an error inside the sproc
        rtr = runETL_API(TRANSFORM_BAD_THROW_ERROR_SP);
        assertEquals("ERROR", _diHelper.getTransformStatus(rtr.getJobId()));
        incrementExpectedErrorCount(false);

        // test mode 4, parameter persistance. Run twice. First time, the value of the in/out parameter supplied in the xml file
        // is changed in the sproc, which should be persisted into the transformConfiguration.state variable map.
        // The second time, the proc doublechecks the changed value was persisted and passed in; errors if not.
        runETL_API(TRANSFORM_PERSISTED_PARAMETER_SP);
        rtr = runETL_API(TRANSFORM_PERSISTED_PARAMETER_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        // verify no error

        // test mode 5, override of persisted parameter. Run twice. First time, the value of the in/out parameter supplied in the xml file
        // is changed in the sproc, which should be persisted into the transformConfiguration.state variable map.
        // However, the parameter is set to override=true in the xml, so on the second run the persisted value should be ignored
        // and original value used instead. The proc verifies this and errors if value has changed.
        runETL_API(TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP);
        rtr = runETL_API(TRANSFORM_OVERRIDE_PERSISTED_PARAMETER_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        // verify no error

        // test mode 6, RunFilterStrategy specified in the xml. Require filterRunId input parameter to be populated
        insertTransferRow("142", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        rtr = runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        // run again, should be No Work. response status should be 'No Work'
        rtr = runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals("No work", rtr.getStatus());
        // insert new transfer and run again to test filterRunId was persisted properly for next run
        insertTransferRow("143", getDate(), getDate(), "new transfer", "added by test automation", "pending");
        rtr = runETL_API(TRANSFORM_RUN_FILTER_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));

        // test mode 7, ModifiedSinceFilterStrategy specified in the xml. Require filterStart & End timestamp input parameters to be populated
        rtr = runETL_API(TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        // run again to test filterStartTimeStamp & filterEndTimeStamp was persisted properly for next run
        rtr = runETL_API(TRANSFORM_MODIFIED_FILTER_NO_SOURCE_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));

        // Now test Modified where source is specified
        insertSourceRow("111", "Sproc Subject 1", "142");
        rtr = runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));
        // run again, should be no work
        rtr = runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals("No work", rtr.getStatus());
        // insert another source row, should have work
        insertSourceRow("112", "Sproc Subject 2", "143");
        rtr = runETL_API(TRANSFORM_MODIFIED_FILTER_WITH_SOURCE_SP);
        assertEquals("COMPLETE", _diHelper.getTransformStatus(rtr.getJobId()));

        // Bad source name
        try
        {
            rtr = runETL_API(TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP);
        }
        catch (CommandException e)
        {
            assertTrue("Incorrect exception message on bad source: ", e.getMessage().startsWith("Could not find table"));
            assertEquals("ERROR", _diHelper.getTransformStatusByTransformId(TRANSFORM_BAD_MODIFIED_FILTER_WITH_BAD_SOURCE_SP));
        }

    }
}
