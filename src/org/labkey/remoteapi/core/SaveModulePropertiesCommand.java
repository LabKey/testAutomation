package org.labkey.remoteapi.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.security.GetContainersCommand;
import org.labkey.remoteapi.security.GetContainersResponse;
import org.labkey.test.params.ModuleProperty;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveModulePropertiesCommand extends PostCommand<CommandResponse>
{
    private final List<ModuleProperty> _moduleProperties;
    private final Map<String, String> _containerIds = new HashMap<>();

    public SaveModulePropertiesCommand(List<? extends ModuleProperty> moduleProperties)
    {
        super("core", "saveModuleProperties");
        _moduleProperties = Collections.unmodifiableList(moduleProperties);
    }

    @Override
    public CommandResponse execute(Connection connection, String folderPath) throws IOException, CommandException
    {
        for (ModuleProperty prop : _moduleProperties)
        {
            String containerPath = prop.getContainerPath();
            if (!_containerIds.containsKey(containerPath))
            {
                GetContainersResponse response = new GetContainersCommand().execute(connection, containerPath);
                _containerIds.put(containerPath, response.getContainerId());
            }
        }
        return super.execute(connection, folderPath);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        JSONArray properties = new JSONArray();
        for (ModuleProperty prop : _moduleProperties)
        {
            JSONObject propJson = new JSONObject();
            propJson.put("userId", 0);
            propJson.put("container", _containerIds.get(prop.getContainerPath()));
            propJson.put("moduleName", prop.getModuleName());
            propJson.put("propName", prop.getPropertyName());
            propJson.put("value", prop.getValue());
            properties.put(propJson);
        }
        json.put("properties", properties);
        return json;
    }
}
