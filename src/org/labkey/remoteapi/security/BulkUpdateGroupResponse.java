/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.remoteapi.security;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.Map;

public class BulkUpdateGroupResponse extends CommandResponse
{
    public BulkUpdateGroupResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public Number getId()
    {
        return getProperty("id");
    }

    public String getName()
    {
        return getProperty("name");
    }

    public List<Map<String, Object>> getNewUsers()
    {
        return getProperty("newUsers");
    }

    public List<Map<String, Object>> getAddedMembers()
    {
        return getProperty("members.added");
    }

    public List<Map<String, Object>> getRemovedMembers()
    {
        return getProperty("members.removed");
    }

    public Map<String, Object> getErrors()
    {
        return getProperty("errors");
    }
}
