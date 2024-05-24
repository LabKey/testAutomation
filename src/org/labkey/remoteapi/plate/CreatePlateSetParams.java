package org.labkey.remoteapi.plate;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreatePlateSetParams
{
    // This will match PlateController.CreatePlateSetForm
    private String _name;
    private String _description;
    private List<CreatePlateSetPlate> _plates = new ArrayList<CreatePlateSetPlate>();
    private PlateSetType _type;
    private String _plateSetId; // optional
    private Integer _rowId;
    private Integer _parentPlateSetId;

    public CreatePlateSetParams()
    {
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("name", _name);
        json.put("description", _description);
        if (_type != null)
            json.put("type", _type.getType());
        json.put("rowId", _rowId);
        json.put("parentPlateSetId", _parentPlateSetId);
        if (_plateSetId != null)
            json.put("plateSetId", _plateSetId);
        if (_plates.size() > 0)
        {
            JSONArray plates = new JSONArray();
            for (CreatePlateSetPlate plate : _plates)
                plates.put(plate.toJSON());
            json.put("plates", plates);
        }
        return json;
    }

    public CreatePlateSetParams setName(String name)
    {
        _name = name;
        return this;
    }

    public String getName()
    {
        return _name;
    }

    public CreatePlateSetParams setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public CreatePlateSetParams setType(PlateSetType type)
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

    public CreatePlateSetParams setParentPlateSetId(Integer parentPlateSetId)
    {
        _parentPlateSetId = parentPlateSetId;
        return this;
    }

    public CreatePlateSetParams setPlateSetPlates( List<CreatePlateSetPlate> plates)
    {
        _plates = plates;
        return this;
    }

    public List<CreatePlateSetPlate> getPlates()
    {
        return _plates;
    }

    public Integer getParentPlateSetId()
    {
        return _parentPlateSetId;
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
