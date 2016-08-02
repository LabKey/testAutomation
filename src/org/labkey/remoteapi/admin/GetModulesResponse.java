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
