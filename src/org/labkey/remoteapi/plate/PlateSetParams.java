package org.labkey.remoteapi.plate;

import org.json.JSONObject;

public class PlateSetParams
{
    private boolean _archived;
    private String _containerId;
    private String _containerName;
    private String _containerPath;
    private String _created;
    private Integer _createdBy;
    private String _description;
    private String _modified;
    private Integer _modifiedBy;
    private String _name;
    private Integer _plateCount;
    private String _plateSetId;
    private Integer _primaryPlateSetId;
    private Integer _rootPlateSetId;
    private Integer _rowId;
    private boolean _template;
    private CreatePlateSetParams.PlateSetType _plateSetType;

    public PlateSetParams(JSONObject json)
    {
        if (json.has("archived"))
            _archived = json.getBoolean("archived");
        if (json.has("containerId"))
            _containerId = json.getString("containerId");
        if (json.has("containerName"))
            _containerName = json.getString("containerName");
        if (json.has("containerPath"))
            _containerPath = json.getString("containerPath");
        if (json.has("created"))
            _created = json.getString("created");
        if (json.has("createdBy"))
            _createdBy = json.getInt("createdBy");
        if (json.has("description"))
            _description = json.getString("description");
        if (json.has("modified"))
            _modified = json.getString("modified");
        if (json.has("modifiedBy"))
            _modifiedBy = json.getInt("modifiedBy");
        if (json.has("name"))
            _name = json.getString("name");
        if (json.has("plateCount"))
            _plateCount = json.getInt("plateCount");
        if (json.has("plateSetId"))
            _plateSetId = json.getString("plateSetId");
        if (json.has("primaryPlateSetId"))
            _primaryPlateSetId = json.getInt("primaryPlateSetId");
        if (json.has("rootPlateSetId"))
            _rootPlateSetId = json.getInt("rootPlateSetId");
        if (json.has("rowId"))
            _rowId = json.getInt("rowId");
        if (json.has("type"))
            _plateSetType = CreatePlateSetParams.PlateSetType.fromName(json.getString("type"));
    }

    public boolean getArchived()
    {
        return _archived;
    }

    public String getContainerId()
    {
        return _containerId;
    }

    public String getContainerName()
    {
        return _containerName;
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public String getCreated()
    {
        return _created;
    }

    public Integer getCreatedBy()
    {
        return _createdBy;
    }

    public String getDescription()
    {
        return _description;
    }

    public String getModified()
    {
        return _modified;
    }

    public Integer getModifiedBy()
    {
        return _modifiedBy;
    }

    public String getName()
    {
        return _name;
    }

    public Integer getPlateCount()
    {
        return _plateCount;
    }

    public String getPlateSetId()
    {
        return _plateSetId;
    }

    public Integer getPrimaryPlateSetId()
    {
        return _primaryPlateSetId;
    }

    public Integer getRootPlateSetId()
    {
        return _rootPlateSetId;
    }

    public Integer getRowId()
    {
        return _rowId;
    }
}
