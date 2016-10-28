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
package org.labkey.remoteapi.admin;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.ResponseObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GetModulesResponse extends CommandResponse
{
    private Set<Module> _modules;
    private String _folderType;

    public GetModulesResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
        _folderType = getProperty("folderType");
    }

    public String getFolderType()
    {
        return _folderType;
    }

    public Set<Module> getModules()
    {
        if (_modules == null)
        {
            _modules = new TreeSet<>();
            for (Map<String, Object> moduleInfo : (List<Map<String, Object>>) getProperty("modules"))
            {
                _modules.add(new Module(moduleInfo));
            }
        }
        return _modules;
    }

    public class Module extends ResponseObject implements Comparable<Module>
    {
        String _tabName;
        String _name;
        Boolean _active;
        Boolean _requireSitePermission;
        Boolean _required;
        Boolean _enabled;

        public Module(Map<String, Object> map)
        {
            super(map);
            _tabName = (String)map.get("tabName");
            _name = (String)map.get("name");
            _active = (Boolean)map.get("active");
            _requireSitePermission = (Boolean)map.get("requireSitePermission");
            _required = (Boolean)map.get("required");
            _enabled = (Boolean)map.get("enabled");
        }

        public String getTabName()
        {
            return _tabName;
        }

        public String getName()
        {
            return _name;
        }

        public Boolean isActive()
        {
            return _active;
        }

        public Boolean isRequireSitePermission()
        {
            return _requireSitePermission;
        }

        public Boolean isRequired()
        {
            return _required;
        }

        public Boolean isEnabled()
        {
            return _enabled;
        }

        @Override
        public int compareTo(Module o)
        {
            return getName().compareTo(o.getName());
        }
    }
}
