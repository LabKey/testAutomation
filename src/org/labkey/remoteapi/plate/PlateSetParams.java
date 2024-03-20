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
        json.put("name", _name);
        json.put("description", _description);
        json.put("type", _type);
        json.put("rowId", _id);
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
