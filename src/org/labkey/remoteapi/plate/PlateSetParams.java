package org.labkey.remoteapi.plate;


import org.json.JSONObject;

public class PlateSetParams
{
    private String _name;
    private String _description;
    private PlateSetType _type;
    private String _plateSetId;
    private Integer _rowId;
    private Integer _rootPlateSetId;

    public PlateSetParams()
    {
    }

    public PlateSetParams(JSONObject json)
    {
        _name = json.getString("name");
        _description = json.optString("description", null);
        _type = PlateSetType.fromName(json.optString("type", null));
        _rowId = json.getInt("rowId");
        _plateSetId = json.getString("plateSetId");
        _rootPlateSetId = json.optIntegerObject("rootPlateSetId", null);
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("name", _name);
        json.put("description", _description);
        if (_type != null)
            json.put("type", _type.getType());
        json.put("rowId", _rowId);
        json.put("parentPlateSetId", _rootPlateSetId);
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

    public PlateSetParams setType(PlateSetType type)
    {
        _type = type;
        return this;
    }

    public PlateSetType getType()
    {
        return _type;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public PlateSetParams setRootPlateSetId(Integer rootPlateSetId)
    {
        _rootPlateSetId = rootPlateSetId;
        return this;
    }

    public Integer getRootPlateSetId()
    {
        return _rootPlateSetId;
    }

    public String getPlateSetId()
    {
        return _plateSetId;
    }

    public enum PlateSetType
    {
        Primary("primary"),
        Assay("assay");

        PlateSetType(String type)
                {
                    this._type = type;
                }
        private final String _type;
        public String getType()
        {
            return _type;
        }
        public static PlateSetType fromName(String type)
        {
            if (type != null)
            {
                for (PlateSetType plateSetType : PlateSetType.values())
                {
                    if (type.equalsIgnoreCase(plateSetType.getType()))
                        return plateSetType;
                }
            }
            return null;
        }
    }
}
