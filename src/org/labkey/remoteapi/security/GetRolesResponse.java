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
import org.labkey.remoteapi.ResponseObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetRolesResponse extends CommandResponse
{
    private List<Role> _roles;

    public GetRolesResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public List<Role> getRoles()
    {
        if (_roles == null)
        {
            _roles = new ArrayList<>();
            for (Map<String, Object> roleMap : (List<Map<String, Object>>) getProperty("roles"))
            {
                _roles.add(new Role(roleMap));
            }
        }
        return _roles;
    }

    public class Role extends ResponseObject
    {
        String _uniqueName;
        String _name;
        String _description;
        String _sourceModule;
        List<Map<String, Object>> _permissions;

        public Role(Map<String, Object> map)
        {
            super(map);
            _uniqueName = (String)map.get("uniqueName");
            _name = (String)map.get("name");
            _description = (String)map.get("description");
            _sourceModule = (String)map.get("sourceModule");
            _permissions = (List<Map<String, Object>>)map.get("permissions");
        }

        public String getUniqueName()
        {
            return _uniqueName;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getSourceModule()
        {
            return _sourceModule;
        }

        public List<Map<String, Object>> getPermissions()
        {
            return _permissions;
        }
    }
}
