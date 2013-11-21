package org.labkey.test.util;

import org.labkey.remoteapi.di.ResetTransformStateCommand;
import org.labkey.remoteapi.di.ResetTransformStateResponse;
import org.labkey.remoteapi.di.RunTransformCommand;
import org.labkey.remoteapi.di.RunTransformResponse;
import org.labkey.remoteapi.di.UpdateTransformConfigurationCommand;
import org.labkey.remoteapi.di.UpdateTransformConfigurationResponse;
import org.labkey.remoteapi.query.*;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

/**
 * User: RyanS
 * Date: 10/22/13
 */
public class DataIntegrationHelper
{
    private String _baseUrl = "http://localhost:8080/labkey";
    private String _username = PasswordUtil.getUsername();
    private String _password = PasswordUtil.getPassword();
    private String _folderPath;
    private String _diSchema = "dataintegration";

    public DataIntegrationHelper(String folderPath)
    {
        _folderPath = folderPath;
    }

    public DataIntegrationHelper(String folderPath, String baseUrl)
    {
        _folderPath = folderPath;
        _baseUrl = baseUrl;
    }

    public SelectRowsResponse executeQuery(String folderPath, String schemaName, String queryStatement)
    {
        SelectRowsResponse exRsp = null;
        Connection cn = new Connection(_baseUrl, _username, _password);
        ExecuteSqlCommand exCmd = new ExecuteSqlCommand(schemaName, queryStatement);
        try
        {
            exRsp = exCmd.execute(cn, folderPath);
        }
        catch(Exception e)
        {
            TestLogger.log(e.getMessage());
        }
        return exRsp;
    }

    public SaveRowsResponse executeInsert(String folderPath, String schemaName, String tableName, Map<String,Object> rows)
    {
        SaveRowsResponse response = null;
        Connection cn = new Connection(_baseUrl, _username, _password);
        InsertRowsCommand insCmd = new InsertRowsCommand(schemaName, tableName);
        insCmd.addRow(rows);
        try
        {
            response = insCmd.execute(cn, folderPath);
        }
        catch(Exception e)
        {
            TestLogger.log(e.getMessage());
        }
        return response;
    }

    public RunTransformResponse runTransform(String transformId)
    {
        RunTransformResponse response = null;
        Connection cn = new Connection(_baseUrl, _username, _password);
        RunTransformCommand cmd = new RunTransformCommand(transformId);
        try
        {
            response = cmd.execute(cn, _folderPath);
        }
        catch(Exception e)
        {
            TestLogger.log("Error running transform " + transformId + ":" + e.getMessage());
        }
        return response;
    }

    public RunTransformResponse runTransformAndWait(String transformId, int msTimeout)
    {
        RunTransformResponse response = null;
        response = runTransform(transformId);
        String jobId = response.getJobId();
        String status = response.getStatus();
        if (!status.equalsIgnoreCase("COMPLETE"))
            for(int i=0; i<msTimeout; i+=1000)
            {
                status = getTransformStatus(jobId);
                if(status.equalsIgnoreCase("COMPLETE"))
                    return response;
                else
                    sleep(500);
            }
        return response;
    }

    private String getTransformStatus(String transformId)
    {
        String query = "SELECT Status FROM dataintegration.TransformRun WHERE JobId = '" + transformId + "'";
        SelectRowsResponse response = executeQuery("/" + _folderPath, _diSchema, query);
        return response.getRows().get(0).get("Status").toString();
    }

    private ResetTransformStateResponse resetTransformState(String transformId)
    {
        Connection cn = new Connection(_baseUrl, _username, _password);
        ResetTransformStateResponse response = null;
        ResetTransformStateCommand rcmd = new ResetTransformStateCommand(transformId);
        try
        {
            response = rcmd.execute(cn, _folderPath);
        }
        catch(Exception e)
        {
            TestLogger.log("ResetTransformStateResponse failed: " + e.getMessage());
        }
        return response;
    }

    private UpdateTransformConfigurationResponse UpdateTransformConfiguration(String transformId, Boolean verbose, Boolean enabled)
    {
        Connection cn = new Connection(_baseUrl, _username, _password);
        UpdateTransformConfigurationResponse response = null;
        UpdateTransformConfigurationCommand command = new UpdateTransformConfigurationCommand(transformId);
        command.setVerboseLogging(verbose);
        command.setEnabled(enabled);
        try
        {
            response = command.execute(cn, _diSchema);
        }
        catch(Exception e)
        {
            TestLogger.log("UpdateTransformConfiguration failed: " + e.getMessage());
        }
        return response;
    }

    private String getContainerForFolder(String folderName)
    {
        String query = "select EntityID from Containers where Name = '" + folderName + "'";
        SelectRowsResponse response = executeQuery("/" + folderName, "core", query);
        return response.getRows().get(0).get("EntityID").toString();
    }

    private void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignore)
        {
        }
    }
}
