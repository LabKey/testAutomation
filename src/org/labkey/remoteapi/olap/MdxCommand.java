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
package org.labkey.remoteapi.olap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.labkey.api.reader.Readers;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * User: tgaluhn
 * Date: 3/31/14
 */
public class MdxCommand extends PostCommand<MdxResponse>
{
    private String _configId;
    private String _schemaName;
    private String _cubeName;
    private String _query;

    public MdxCommand(MdxCommand source)
    {
        super(source);
        _configId = source.getConfigId();
        _schemaName = source.getSchemaName();
        _cubeName = source.getCubeName();
        _query = source.getQuery();
    }

    public MdxCommand(String configId, String schema, String query)
    {
        super("olap", "executeMdx");
        _configId = configId;
        _schemaName = schema;
        _query = query;
    }

    public String getConfigId()
    {
        return _configId;
    }

    public void setConfigId(String configId)
    {
        this._configId = configId;
    }

    public String getSchemaName()
    {
        return _schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        this._schemaName = schemaName;
    }

    public String getCubeName()
    {
        return _cubeName;
    }

    public void setCubeName(String cubeName)
    {
        this._cubeName = cubeName;
    }

    public String getQuery()
    {
        return _query;
    }

    public void setQuery(String query)
    {
        _query = query;
    }

    @Override
    protected MdxResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new MdxResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = super.getJsonObject();
        if (result == null)
        {
            result = new JSONObject();
        }
        result.put("configId", _configId);
        result.put("schemaName", _schemaName);
        if (_cubeName != null)
            result.put("cubeName", _cubeName);
        result.put("query", _query);
        setJsonObject(result);
        return result;
    }

    @Override
    public MdxResponse execute(Connection connection, String folderPath) throws IOException, CommandException
    {
        // Hack override to decode the gzip response, as that's not yet supported in the java client api
        try (Response response = _execute(connection, folderPath))
        {
            JSONObject json;
            json = (JSONObject) JSONValue.parse(Readers.getReader(new GZIPInputStream(response.getInputStream())));
            return createResponse(null, response.getStatusCode(), response.getContentType(), json);
        }
    }
}
