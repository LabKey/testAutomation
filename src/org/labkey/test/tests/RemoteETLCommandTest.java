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

import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.di.BaseTransformCommand;
import org.labkey.remoteapi.di.BaseTransformResponse;
import org.labkey.remoteapi.di.ResetTransformStateCommand;
import org.labkey.remoteapi.di.ResetTransformStateResponse;
import org.labkey.remoteapi.di.RunTransformCommand;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.remoteapi.di.UpdateTransformConfigurationCommand;
import org.labkey.remoteapi.di.UpdateTransformConfigurationResponse;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.PasswordUtil;

import java.util.Date;

/**
 * User: dax
 * Date: 11/3/13
 * Time: 11:32 AM
 */
@Category({DailyB.class})
public class RemoteETLCommandTest extends ETLTest
{
    private static final String TRANSFORM_NOTFOUND = "{simpletest}/notfound";

    @Override
    protected String getProjectName()
    {
        return "RemoteETLCommandTestProject";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        super.runInitialSetup(true);
        verifyRemoteApi();
    }

    private void verifyBaseTransformResponse(BaseTransformResponse response)
    {
        Assert.assertTrue("success should be true", response.getSuccess());
    }

    private void verifyUpdateTransformConfigurationResponse(UpdateTransformConfigurationResponse response, UpdateTransformConfigurationCommand command)
    {
        verifyBaseTransformResponse(response);
        // just verify this method doesn't choke
        JSONObject obj = response.getResult();
        // currently not filled in, verify the methods don't choke
        Date d = response.getLastChecked();
        obj = response.getState();

        // verify the accessors now (which look at the returned JSON data anyway)
        Assert.assertTrue("response.enabled should equal command.enabled",
                command.getEnabled() == null ? true : response.getEnabled() == command.getEnabled());
        Assert.assertTrue("response.verbose should equal command.verbose",
                command.getVerboseLogging() == null ? true : response.getVerboseLogging() == command.getVerboseLogging());
        Assert.assertTrue("response.description should equal truncation description",
                response.getDescriptionId().equalsIgnoreCase(command.getTransformId()));
    }

    private void verifyResetTransformStateResponse(ResetTransformStateResponse response)
    {
        verifyBaseTransformResponse(response);
    }

    private void verifyRunTransformResponse(RunTransformResponse response, boolean noWork)
    {
        verifyBaseTransformResponse(response);

        if (noWork)
        {
          Assert.assertTrue("status should be 'no work'", response.getStatus().equalsIgnoreCase("no work"));
          Assert.assertTrue("expect no pipeline job", null == response.getPipelineURL());
          Assert.assertTrue("expect no pipeline job", null == response.getJobId());
        }
        else
        {
            Assert.assertTrue("job id should not be null", response.getJobId() != null);
            Assert.assertFalse("status should not be error", response.getStatus().equalsIgnoreCase("error"));
            Assert.assertTrue("pipeline url should be valid",
                    response.getPipelineURL().contains(getPipelineURLFragment(response.getJobId())));
        }
    }

    private String getPipelineURLFragment(String jobId)
    {
        return "/pipeline-status/" + getProjectName() + "/details.view?rowId=" + jobId;
    }

    private void verifyRemoteApi()
    {
        //create a new connection, specifying base URL,
        //user email, and password
        Connection cn = new Connection("http://localhost:8080/labkey", PasswordUtil.getUsername(), PasswordUtil.getPassword());

        // expect error with missing transform
        invokeCommand(new RunTransformCommand(TRANSFORM_NOTFOUND), cn, TRANSFORM_NOTFOUND);

        insertSourceRow("0", "Subject 0", null);
        // this should succeed
        RunTransformResponse rtr = (RunTransformResponse) invokeCommand(new RunTransformCommand(TRANSFORM_APPEND), cn);
        _jobsComplete++;
        verifyRunTransformResponse(rtr, false /*no work*/);
        // verify transform happened
        assertInTarget1("Subject 0");

        // run again - but the checker should return no work
        rtr = (RunTransformResponse) invokeCommand(new RunTransformCommand(TRANSFORM_APPEND), cn);
        verifyRunTransformResponse(rtr, true);

        // expect error with missing transform
        invokeCommand(new ResetTransformStateCommand(TRANSFORM_NOTFOUND), cn, TRANSFORM_NOTFOUND);
        // now reset
        ResetTransformStateResponse rtsr = (ResetTransformStateResponse) invokeCommand(new ResetTransformStateCommand(TRANSFORM_APPEND), cn);
        verifyResetTransformStateResponse(rtsr);

        // rerun - we should run the transform but get an error in the pipeline - this is still a success though because
        // the transform was run.  The pipeline url will take you to the error status, however.
        rtr = (RunTransformResponse) invokeCommand(new RunTransformCommand(TRANSFORM_APPEND), cn);
        verifyRunTransformResponse(rtr, false);
        _jobsComplete++;
        // verify we had two runs complete (one success, one error)
        checkRun(true);

        // not found
        invokeCommand(new UpdateTransformConfigurationCommand(TRANSFORM_NOTFOUND), cn, TRANSFORM_NOTFOUND);

        // note that enabling will cause the transform to run immediately - so don't run one that injects
        // an error in the pipeline.  Truncate is safe because it wipes away whatever was in the target
        UpdateTransformConfigurationCommand utcc = new UpdateTransformConfigurationCommand(TRANSFORM_TRUNCATE);
        UpdateTransformConfigurationResponse utcr = (UpdateTransformConfigurationResponse) invokeCommand(utcc, cn);
        utcc.setEnabled(false);
        utcc.setVerboseLogging(false);
        verifyUpdateTransformConfigurationResponse(utcr, utcc);

        utcc.setEnabled(true);
        utcc.setVerboseLogging(true);
        utcr = (UpdateTransformConfigurationResponse) invokeCommand(utcc, cn);
        verifyUpdateTransformConfigurationResponse(utcr, utcc);

        // be sure to check for all expected errors here so that the test won't fail on exit
        checkExpectedErrors(getExpectedErrorCount(1));
    }

    private CommandResponse invokeCommand(BaseTransformCommand cmd, Connection cn)
    {
        return invokeCommand(cmd, cn, null);
    }

    private CommandResponse invokeCommand(BaseTransformCommand cmd, Connection cn, String expectedError)
    {
        CommandResponse response = null;
        try
        {
            response = cmd.execute(cn, getProjectName());
        }
        catch(Exception e)
        {
            if (null == expectedError)
                Assert.assertTrue("unexpected error on remote commmand", false);
            else
            {
                String s = e.getMessage();
                Assert.assertTrue(s.contains(expectedError));
            }
        }

        return response;
    }
}