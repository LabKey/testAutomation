package org.labkey.remoteapi.plate;

import org.json.JSONObject;

import java.util.Map;

public class PlateParams
{
    private boolean _archived;
    private String _assayType;
    private int _columns;
    private String _description;
    private String _name;
    private String _plateId;
    private int _plateSetId;
    private int _plateType;
    private int _rowId;
    private int _rows;
    private boolean _template;

    private PlateParams()
    {
    }

    public PlateParams(JSONObject json)
    {
        if (json.has("name"))
            _name = json.getString("name");
        if (json.has("plateId"))
            _plateId = json.getString("plateId");
        if (json.has("rowId"))
            _rowId = json.getInt("rowId");
        if (json.has("plateSet"))
            _plateSetId = json.getInt("plateSet");

        if (json.has("plateType"))  // server returns a jsonObject about the plate
        {
            JSONObject plateInfo = json.getJSONObject("plateType");
            _description = plateInfo.getString("description");
            _rows = plateInfo.getInt("rows");
            _columns = plateInfo.getInt("cols");
            _plateType = plateInfo.getInt("rowId"); // this is the ordinal of the plate-types, e.g. 1-3x4, 2-4x6, 3-8x12
        }

        if (json.has("assayType"))
            _assayType = json.getString("assayType");

        if (json.has("template"))
            _template = json.getBoolean("template");

        if (json.has("archived"))
            _archived = json.getBoolean("archived");
    }

    public static PlateParams fromQueryRow(Map<String, Object> row)
    {
        var params = new PlateParams();
        params._archived = (Boolean) row.get("Archived");
        params._assayType = (String) row.get("AssayType");
        params._columns = (Integer) row.get("PlateType/Columns");
        params._description = (String) row.get("Description");
        params._name = (String) row.get("Name");
        params._plateId = (String) row.get("PlateId");
        params._plateSetId = (Integer) row.get("PlateSet");
        params._plateType = (Integer) row.get("PlateType");
        params._rowId = (Integer) row.get("RowId");
        params._rows = (Integer) row.get("PlateType/Rows");
        params._template = (Boolean) row.get("Template");

        return params;
    }

    public String getName()
    {
        return _name;
    }

    public String getPlateId()
    {
        return _plateId;
    }

    public int getRowId()
    {
        return _rowId;
    }

    public int getPlateSetId()
    {
        return _plateSetId;
    }

    public String getDescription()
    {
        return _description;
    }

    public int getRows()
    {
        return _rows;
    }

    public int getColumns()
    {
        return _columns;
    }

    public String getAssayType()
    {
        return _assayType;
    }

    public boolean isTemplate()
    {
        return _template;
    }

    public boolean isArchived()
    {
        return _archived;
    }

    public PlateTypes getPlateType()
    {
        return PlateTypes.fromRowId(_plateType);
    }
}
