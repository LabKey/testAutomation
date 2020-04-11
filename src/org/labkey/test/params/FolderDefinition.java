package org.labkey.test.params;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.security.CreateContainerCommand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FolderDefinition
{
    private String _parentPath = "/";

    // CreateContainerAction params
    private String _name;
    private String _folderType;
    private String _title;
    private String _description;
    private Set<String> _ensureModules = new HashSet<>();
    private Boolean _isWorkbook;
    private String _type;

    // SetFolderPermissionsAction params
    private boolean _inheritPermissions = false;
    private String _copyPermissionsProject;

    public FolderDefinition()
    {
    }

    public FolderDefinition(String name)
    {
        _name = name;
    }

    public String getParentPath()
    {
        return _parentPath;
    }

    public FolderDefinition setParentPath(String parentPath)
    {
        _parentPath = parentPath;
        return this;
    }

    public String getName()
    {
        return _name;
    }

    public FolderDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getFolderType()
    {
        return _folderType;
    }

    public FolderDefinition setFolderType(String folderType)
    {
        _folderType = folderType;
        return this;
    }

    public String getTitle()
    {
        return _title;
    }

    public FolderDefinition setTitle(String title)
    {
        _title = title;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public FolderDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public Set<String> getEnsureModules()
    {
        return new HashSet<>(_ensureModules);
    }

    public FolderDefinition setEnsureModules(Set<String> ensureModules)
    {
        _ensureModules = ensureModules;
        return this;
    }

    public Boolean getWorkbook()
    {
        return _isWorkbook;
    }

    public FolderDefinition setWorkbook(Boolean workbook)
    {
        _isWorkbook = workbook;
        return this;
    }

    public String getType()
    {
        return _type;
    }

    public FolderDefinition setType(String type)
    {
        _type = type;
        return this;
    }

    public FolderDefinition inheritParentPermissions()
    {
        _inheritPermissions = true;
        _copyPermissionsProject = null;
        return this;
    }

    public FolderDefinition copyPermissionsFromProject(String projectPath)
    {
        _inheritPermissions = true;
        _copyPermissionsProject = projectPath;
        return this;
    }

    public FolderDefinition permissionsForMyUserOnly()
    {
        _inheritPermissions = false;
        _copyPermissionsProject = null;
        return this;
    }

    public CreateContainerCommand getCreateCommand()
    {
        CreateContainerCommand command = new CreateContainerCommand(_name);
        final List<String> ensureModules = new ArrayList<>(getEnsureModules());
        if (!ensureModules.isEmpty())
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ensureModules", ensureModules); // Not yet supported by CreateContainerCommand
            command.setJsonObject(jsonObject);
        }
        command.setTitle(getTitle());
        command.setDescription(getDescription());
        command.setFolderType(getFolderType());
        command.setWorkbook(getWorkbook());
        command.setType(getType());
        return command;
    }

    public PostCommand getPermissionsCommand()
    {
        PostCommand<CommandResponse> command = new PostCommand<>("admin", "setFolderPermissions");
        JSONObject jsonObject = new JSONObject();
        if (_copyPermissionsProject != null)
        {
            jsonObject.put("permissionType", "CopyExistingProject");
            jsonObject.put("targetProject", _copyPermissionsProject);
        }
        else if (_inheritPermissions)
        {
            jsonObject.put("permissionType", "Inherit");
        }
        else
        {
            jsonObject.put("permissionType", "CurrentUser");
        }
        command.setJsonObject(jsonObject);
        return command;
    }
}
