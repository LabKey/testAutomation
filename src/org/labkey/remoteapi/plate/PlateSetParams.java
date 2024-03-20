package org.labkey.remoteapi.plate;


import org.json.JSONObject;

public class PlateSetParams
{
    private String _name = null;
    private String _description = null;
    private String _type = null;
    private Integer _id = null;
    private Integer _parentPlateSetId = null;

    public PlateSetParams()
    {
    }

    public PlateSetParams(JSONObject json)
    {
        _name = json.getString("name");
        _description = json.optString("description", null);
        _type = json.optString("type", null);
        _id = json.getInt("rowId");
        _parentPlateSetId = json.optIntegerObject("parentPlateSetId", null);
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        if (_name != null)
            json.put("name", _name);
        if (_description != null)
            json.put("description", _description);
        if (_type != null)
            json.put("type", _type);
        if (_id != null)
            json.put("rowId", _id);
        if (_parentPlateSetId != null)
            json.put("parentPlateSetId", _parentPlateSetId);
        return json;
    }

    public PlateSetParams setName(String name)
    {
        _name = name;
        return this;
    }

    public String getName()
    {
        return _name;
    }

    public PlateSetParams setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public PlateSetParams setType(String type)
    {
        _type = type;
        return this;
    }

    public String getType()
    {
        return _type;
    }

    public Integer getId()
    {
        return _id;
    }

    public PlateSetParams setParentPlateSetId(Integer parentPlateSetId)
    {
        _parentPlateSetId = parentPlateSetId;
        return this;
    }

    public Integer getParentPlateSetId()
    {
        return _parentPlateSetId;
    }

}
