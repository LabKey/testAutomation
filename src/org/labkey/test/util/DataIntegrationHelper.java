/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.di.ResetTransformStateCommand;
import org.labkey.remoteapi.di.ResetTransformStateResponse;
import org.labkey.remoteapi.di.RunTransformCommand;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.remoteapi.di.UpdateTransformConfigurationCommand;
import org.labkey.remoteapi.di.UpdateTransformConfigurationResponse;
import org.labkey.remoteapi.query.ExecuteSqlCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataIntegrationHelper
{
    private String _baseUrl = WebTestHelper.getBaseURL();
    private String _username = PasswordUtil.getUsername();
    private String _password = PasswordUtil.getPassword();
    private String _folderPath;
    public final static String DI_SCHEMA = "dataintegration";

    public DataIntegrationHelper(String folderPath)
    {
        _folderPath = folderPath;
    }

    public DataIntegrationHelper(String folderPath, String baseUrl)
    {
        _folderPath = folderPath;
        _baseUrl = baseUrl;
    }

    public SelectRowsResponse executeQuery(String folderPath, String schemaName, String queryStatement) throws IOException, CommandException
    {
        SelectRowsResponse exRsp = null;
        Connection cn = new Connection(_baseUrl, _username, _password);
        ExecuteSqlCommand exCmd = new ExecuteSqlCommand(schemaName, queryStatement);
        exRsp = exCmd.execute(cn, folderPath);
        return exRsp;
    }

    public SaveRowsResponse executeInsert(String folderPath, String schemaName, String tableName, Map<String,Object> rows) throws Exception
    {
        SaveRowsResponse response = null;
        Connection cn = new Connection(_baseUrl, _username, _password);
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, tableName);
        insCmd.addRow(rows);
        response = insCmd.execute(cn, folderPath);
        return response;
    }

    public RunTransformResponse runTransform(String transformId) throws CommandException, IOException
    {
        RunTransformResponse response;
        Connection cn = new Connection(_baseUrl, _username, _password);
        RunTransformCommand cmd = new RunTransformCommand(transformId);
        response = cmd.execute(cn, _folderPath);
        return response;
    }

    @LogMethod
    public RunTransformResponse runTransformAndWait(@LoggedParam String transformId, int msTimeout) throws CommandException, IOException
    {
        RunTransformResponse response;
        response = runTransform(transformId);
        String jobId = response.getJobId();
        String status = response.getStatus();
        if (status.equalsIgnoreCase("Queued"))
        {
            long startTime = System.currentTimeMillis();
            do
            {
                status = getTransformStatus(jobId);
                if(status.equalsIgnoreCase("COMPLETE") || status.equalsIgnoreCase("ERROR"))
                    return response;
                else
                    sleep(500);
            }while(System.currentTimeMillis() - startTime < msTimeout);
            throw new TestTimeoutException("Timeout for ETL job to complete. Status = " + status + ". Exceeded " + msTimeout + "ms");
        }
        return response;
    }

    public String getTransformStatus(String jobId) throws CommandException, IOException
    {
        return getTransformRunFieldByJobId(jobId, "Status");
    }

    public String getTransformStatusByTransformId(String transformId) throws CommandException, IOException
    {
        return getTransformRunFieldByTransformId(transformId, "Status");
    }

    public String getExperimentRunId(String jobId) throws Exception
    {
        return getTransformRunFieldByJobId(jobId, "ExpRunId");
    }

    public String getTransformRunFieldByTransformId(String transformId, String fieldName) throws CommandException, IOException
    {
        // TODO: Proper handling of null transformId
        String query = "SELECT " + fieldName + " FROM dataintegration.TransformRun WHERE transformId = '" + transformId + "' ORDER BY Created DESC LIMIT 1";
        SelectRowsResponse response = executeQuery("/" + _folderPath, DI_SCHEMA, query);
        return response.getRows().get(0).get(fieldName).toString();
    }

    public String getTransformRunFieldByJobId(String jobId, String fieldName) throws CommandException, IOException
    {
        // TODO: Proper handling of null jobId
        String query = "SELECT " + fieldName + " FROM dataintegration.TransformRun WHERE JobId = '" + jobId + "'";
        SelectRowsResponse response = executeQuery("/" + _folderPath, DI_SCHEMA, query);
        return response.getRows().get(0).get(fieldName).toString();
    }

    public String getTransformState(String transformId) throws CommandException, IOException
    {
        // TODO: Proper handling of null transformId
        String query = "SELECT TransformState FROM dataintegration.TransformConfiguration WHERE transformId = '" + transformId + "' ORDER BY Created DESC LIMIT 1";
        SelectRowsResponse response = executeQuery("/" + _folderPath, DI_SCHEMA, query);
        return response.getRows().get(0).get("TransformState").toString();
    }

    public ResetTransformStateResponse resetTransformState(String transformId) throws CommandException, IOException
    {
        Connection cn = new Connection(_baseUrl, _username, _password);
        ResetTransformStateResponse response = null;
        ResetTransformStateCommand rcmd = new ResetTransformStateCommand(transformId);
        return rcmd.execute(cn, _folderPath);
    }

    private UpdateTransformConfigurationResponse UpdateTransformConfiguration(String transformId, Boolean verbose, Boolean enabled) throws CommandException, IOException
    {
        Connection cn = new Connection(_baseUrl, _username, _password);
        UpdateTransformConfigurationResponse response = null;
        UpdateTransformConfigurationCommand command = new UpdateTransformConfigurationCommand(transformId);
        command.setVerboseLogging(verbose);
        command.setEnabled(enabled);
        response = command.execute(cn, DI_SCHEMA);
        return response;
    }

    private String getContainerForFolder(String folderName) throws CommandException, IOException
    {
        String query = "select EntityID from Containers where Name = '" + folderName + "'";
        SelectRowsResponse response = executeQuery("/" + folderName, "core", query);
        return response.getRows().get(0).get("EntityID").toString();
    }

    public void sleep(long ms)
    {
        BaseWebDriverTest.sleep(ms);
    }

    public String getEtlLogFile(String jobId) throws CommandException, IOException
    {
        String query = "SELECT FilePath FROM pipeline.job WHERE RowId = '" + jobId + "'";
        SelectRowsResponse response = executeQuery("/" + _folderPath, DI_SCHEMA, query);
        String filePath = response.getRows().get(0).get("FilePath").toString();
        if (filePath == null)
            return null;

        try
        {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            TestLogger.log("Retrieving job log file failed: " + e.getMessage());
            return null;
        }
    }

    public void assertInEtlLogFile(String jobId, String... logStrings) throws CommandException, IOException
    {
        final String etlLogFile = getEtlLogFile(jobId);

        for (String logString : logStrings)
            assertTrue("Log file did not contain: " + logString, StringUtils.containsIgnoreCase(etlLogFile, logString));
    }

    public void assertNotInEtlLogFile(String jobId, String... logStrings) throws CommandException, IOException
    {
        final String etlLogFile = getEtlLogFile(jobId);

        for (String logString : logStrings)
            assertFalse("Log file contained : " + logString, StringUtils.containsIgnoreCase(etlLogFile, logString));
    }
}
