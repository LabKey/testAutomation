/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.remoteapi.domain;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteDomainCommand extends PostCommand<DeleteDomainResponse>
{
    private String _schemaName;
    private String _queryName;

    public DeleteDomainCommand(String schema, String queryName)
    {
        super("property", "deleteDomain");
        _queryName = queryName;
        _schemaName = schema;
    }

    /**
     * Returns the domain name.
     * @return The domain name.
     */
    public String getQueryName()
    {
        return _queryName;
    }

    /**
     * Sets the schema name
     * @param queryName The new domain name.
     */
    public void setQueryName(String queryName)
    {
        _queryName = queryName;
    }

    /**
     * Returns the schema name.
     * @return The schema name.
     */
    public String getSchemaName()
    {
        return _schemaName;
    }

    /**
     * Sets the schema name
     * @param schemaName The new schema name.
     */
    public void setSchemaName(String schemaName)
    {
        _schemaName = schemaName;
    }

    @Override
    protected DeleteDomainResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DeleteDomainResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("schemaName", getSchemaName());
        obj.put("queryName", getQueryName());

        return obj;
    }
}
