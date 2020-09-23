package org.labkey.test.params;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.security.CreateContainerCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Properties for creating Projects, Folders, and Workbooks
 */
public class ContainerProps
{
    private String _parentPath;

    // CreateContainerAction params
    private String _name;
    private String _folderType;
    private String _title;
    private String _description;
    private List<String> _ensureModules = new ArrayList<>();
    private boolean _isWorkbook = false;
    private String _type;

    // SetFolderPermissionsAction params
    private Boolean _inheritPermissions = null;
    private String _copyPermissionsProject;

    /**
     * Private constructor for project, folder, and workbook factory methods.
     */
    private ContainerProps()
    { }

    public static ContainerProps project(String name)
    {
        return new ContainerProps().setName(name);
    }

    public static ContainerProps folder(String parentPath, String name)
    {
        return new ContainerProps()
            .setParentPath(parentPath)
            .setName(name);
    }

    public static ContainerProps workbook(String parentPath)
    {
        return new ContainerProps()
            .setParentPath(parentPath)
            .setWorkbook(true);
    }

    public boolean isProject()
    {
        return getParentPath() == null || getParentPath().isEmpty() || getParentPath().equalsIgnoreCase("/");
    }

    public String getParentPath()
    {
        return _parentPath;
    }

    public ContainerProps setParentPath(String parentPath)
    {
        _parentPath = parentPath;
        return this;
    }

    public String getName()
    {
        return _name;
    }

    public ContainerProps setName(String name)
    {
        _name = name;
        return this;
    }

    public String getFolderType()
    {
        return _folderType;
    }

    public ContainerProps setFolderType(String folderType)
    {
        _folderType = folderType;
        return this;
    }

    public String getTitle()
    {
        return _title;
    }

    public ContainerProps setTitle(String title)
    {
        _title = title;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public ContainerProps setDescription(String description)
    {
        _description = description;
        return this;
    }

    public List<String> getEnsureModules()
    {
        return Collections.unmodifiableList(_ensureModules);
    }

    public ContainerProps setEnsureModules(List<String> ensureModules)
    {
        _ensureModules = ensureModules;
        return this;
    }

    public boolean isWorkbook()
    {
        return _isWorkbook;
    }

    public ContainerProps setWorkbook(Boolean workbook)
    {
        _isWorkbook = workbook;
        return this;
    }

    public String getType()
    {
        return _type;
    }

    /**
     * Set container type, not to be confused with 'folderType'. This parameter
     * is usually not required because the server will infer 'normal' or
     * 'workbook' from other properties.
     * @param type 'type' parameter
     * @return this property object
     */
    public ContainerProps setType(String type)
    {
        _type = type;
        return this;
    }

    public ContainerProps inheritParentPermissions()
    {
        _inheritPermissions = true;
        _copyPermissionsProject = null;
        return this;
    }

    public ContainerProps copyPermissionsFromProject(String projectPath)
    {
        _inheritPermissions = true;
        _copyPermissionsProject = projectPath;
        return this;
    }

    public ContainerProps permissionsForMyUserOnly()
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
        command.setWorkbook(isWorkbook());
        command.setType(getType());
        return command;
    }

    public PostCommand getPermissionsCommand()
    {
        if (_inheritPermissions == null)
        {
            return null;
        }

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
