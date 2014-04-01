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
package org.labkey.test.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EHRClientAPIHelper
{
    private BaseWebDriverTest _test;
    private String _containerPath;
    public static final String DATE_SUBSTITUTION = "@@CURDATE@@";

    public EHRClientAPIHelper(BaseWebDriverTest test, String containerPath)
    {
        _test = test;
        _containerPath = containerPath;
    }

    public void createdIfNeeded(String schema, String query, Map<String, Object> row, String pkCol) throws Exception
    {
        if (!doesRowExist(schema, query, row, pkCol))
        {
            insertRow(schema, query, row, false);
        }
    }

    public Connection getConnection()
    {
        return new Connection(_test.getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public boolean doesRowExist(String schema, String query, Map<String, Object> row, String pkCol) throws Exception
    {
        SelectRowsCommand select = new SelectRowsCommand(schema, query);
        select.addFilter(new Filter(pkCol, row.get(pkCol), Filter.Operator.EQUAL));
        SelectRowsResponse resp = select.execute(getConnection(), _containerPath);

        return resp.getRowCount().intValue() > 0;
    }

    public int getRowCount(String schema, String query) throws Exception
    {
        SelectRowsCommand select = new SelectRowsCommand(schema, query);
        SelectRowsResponse resp = select.execute(getConnection(), _containerPath);

        return resp.getRowCount().intValue();
    }

    public SaveRowsResponse insertRow(String schema, String query, Map<String, Object> row, boolean expectFailure) throws Exception
    {
        try
        {
            InsertRowsCommand insertCmd = new InsertRowsCommand(schema, query);
            insertCmd.addRow(row);
            SaveRowsResponse resp = insertCmd.execute(getConnection(), _containerPath);

            if (expectFailure)
                throw new Exception("Expected command to fail");

            return resp;

        }
        catch (CommandException e)
        {
            if (!expectFailure)
                throw e;
            else
                return null;
        }
    }

    public SaveRowsResponse updateRow(String schema, String query, Map<String, Object> row, boolean expectFailure) throws Exception
    {
        try
        {
            SaveRowsCommand cmd = new UpdateRowsCommand(schema, query);
            cmd.addRow(row);

            SaveRowsResponse resp = cmd.execute(getConnection(), _containerPath);

            if (expectFailure)
                throw new Exception("Expected command to fail");

            return resp;
        }
        catch (CommandException e)
        {
            if (!expectFailure)
                throw e;
            else
                return null;
        }
    }

    public void deleteIfExists(String schema, String query, Map<String, Object> row, String pkCol) throws Exception
    {
        if (doesRowExist(schema, query, row,pkCol))
        {
            deleteRow(schema, query, row, pkCol, false);
        }
    }

    public SaveRowsResponse deleteRow(String schema, String query, Map<String, Object> row, String pkCol, boolean expectFailure) throws Exception
    {
        try
        {
            DeleteRowsCommand cmd = new DeleteRowsCommand(schema, query);
            cmd.addRow(row);

            SaveRowsResponse resp = cmd.execute(getConnection(), _containerPath);
            if (expectFailure)
                throw new Exception("Expected command to fail");

            return resp;
        }
        catch (Exception e)
        {
            if (expectFailure)
                return null;
            else throw e;
        }
    }

    public JSONObject prepareInsertCommand(String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows)
    {
        return prepareCommand("insertWithKeys", schema, queryName, pkName, fieldNames, rows, null);
    }

    public JSONObject prepareUpdateCommand(String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows, @Nullable Object[][] oldKeys)
    {
        return prepareCommand("updateChangingKeys", schema, queryName, pkName, fieldNames, rows, oldKeys);
    }

    private JSONObject prepareCommand(String command, String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows, @Nullable Object[][] oldKeys)
    {
        try
        {
            JSONObject resp = new JSONObject();
            resp.put("schemaName", schema);
            resp.put("queryName", queryName);
            resp.put("command", command);
            JSONArray jsonRows = new JSONArray();
            int idx = 0;
            for (Object[] row : rows)
            {
                JSONObject oldKeyMap = new JSONObject();
                JSONObject values = new JSONObject();

                int position = 0;
                for (String name : fieldNames)
                {
                    Object v = row[position];

                    //allow mechanism to use current time,
                    if (DATE_SUBSTITUTION.equals(v))
                        v = (new Date()).toString();

                    values.put(name, v);
                    if (pkName.equals(name))
                        oldKeyMap.put(name, v);

                    position++;
                }

                if (oldKeys != null && oldKeys.length > idx)
                {
                    JSONObject obj = new JSONObject();
                    int j = 0;
                    for (String field : fieldNames)
                    {
                        obj.put(field, oldKeys[idx][j]);
                        j++;
                    }
                    oldKeyMap = obj;
                }

                JSONObject ro = new JSONObject();
                ro.put("oldKeys", oldKeyMap);
                ro.put("values", values);
                jsonRows.put(ro);
            }
            resp.put("rows", jsonRows);

            return resp;
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String doSaveRows(String email, List<JSONObject> commands, JSONObject extraContext, boolean expectSuccess)
    {
        long start = System.currentTimeMillis();
        HttpClient client = WebTestHelper.getHttpClient(email, PasswordUtil.getPassword());
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpPost method = null;
        HttpResponse response = null;
        try
        {
            JSONObject json = new JSONObject();
            json.put("commands", commands);
            json.put("extraContext", extraContext);

            String requestUrl = WebTestHelper.getBaseURL() + "/query/" + _containerPath + "/saveRows.view";
            method = new HttpPost(requestUrl);
            method.addHeader("Content-Type", "application/json");
            method.setEntity(new StringEntity(json.toString(), "application/json", "UTF-8"));

            response = client.execute(method, context);
            int status = response.getStatusLine().getStatusCode();

            _test.log("Expect success: " + expectSuccess + ", actual: " + (HttpStatus.SC_OK == status));

            if (expectSuccess && HttpStatus.SC_OK != status)
            {
                logResponse(response);
                assertEquals("SaveRows request failed unexpectedly with code: " + status, HttpStatus.SC_OK, status);
            }
            else if (!expectSuccess && HttpStatus.SC_BAD_REQUEST != status)
            {
                logResponse(response);
                assertEquals("SaveRows request failed unexpectedly with code: " + status, HttpStatus.SC_BAD_REQUEST, status);
            }

            String responseBody = WebTestHelper.getHttpResponseBody(response);
            EntityUtils.consume(response.getEntity()); // close connection

            return responseBody;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try{
                if (response != null)
                    EntityUtils.consume(response.getEntity());
            }
            catch (IOException ex)
            {/*ignore*/}
            if (client != null)
                client.getConnectionManager().shutdown();
        }
    }

    private void logResponse(HttpResponse response)
    {
        String responseBody = WebTestHelper.getHttpResponseBody(response);
        try
        {
            JSONObject o = new JSONObject(responseBody);
            if (o.has("exception"))
                _test.log("Expection: " + o.getString("exception"));

            Map<String, List<String>> ret = processResponse(responseBody);
            for (String field : ret.keySet())
            {
                _test.log("Error in field: " + field);
                for (String err : ret.get(field))
                {
                    _test.log(err);
                }
            }
        }
        catch (JSONException e)
        {
            _test.log("Response was not JSON");
            _test.log(responseBody);
        }
    }

    public Map<String, List<String>> processResponse(String response)
    {
        try
        {
            Map<String, List<String>> ret = new HashMap<>();
            JSONObject o = new JSONObject(response);
            if (o.has("errors"))
            {
                JSONArray errors = o.getJSONArray("errors");
                for (int i = 0; i < errors.length(); i++)
                {
                    JSONObject error = errors.getJSONObject(i);
                    JSONArray subErrors = error.getJSONArray("errors");
                    if (subErrors != null)
                    {
                        for (int j = 0; j < subErrors.length(); j++)
                        {
                            JSONObject subError = subErrors.getJSONObject(j);
                            String msg = subError.getString("message");
                            if(!subError.has("field"))
                                throw new RuntimeException(msg);

                            String field = subError.getString("field");

                            List<String> list = ret.get(field);
                            if (list == null)
                                list = new ArrayList<>();

                            list.add(msg);
                            ret.put(field, list);
                        }
                    }
                }
            }

            //append errors from extraContext
            if (o.has("extraContext") && o.getJSONObject("extraContext").has("skippedErrors"))
            {
                JSONObject errors = o.getJSONObject("extraContext").getJSONObject("skippedErrors");
                Iterator keys = errors.keys();
                while (keys.hasNext())
                {
                    String key = (String)keys.next();
                    JSONArray errorArray = errors.getJSONArray(key);
                    for (int i=0;i<errorArray.length();i++)
                    {
                        JSONObject subError = errorArray.getJSONObject(i);
                        String msg = subError.getString("message");
                        String field = subError.getString("field");

                        List<String> list = ret.get(field);
                        if (list == null)
                            list = new ArrayList<>();

                        list.add(msg);
                        ret.put(field, list);
                    }
                }
            }

            return ret;
        }
        catch (JSONException e)
        {
            throw new RuntimeException(e);
        }
    }
}
