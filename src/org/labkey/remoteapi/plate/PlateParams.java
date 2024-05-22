package org.labkey.remoteapi.plate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.RowMapFactory;
import org.labkey.api.util.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlateParams
{
    private String _assayType; // blank or Standard
    private List<Map<String, Object>> _data = new ArrayList<>();
    private String _plateId;
    private String _description;
    private String _name;
    private Integer _plateSetId;
    private Integer _plateType; // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
    private boolean _template;
    private boolean _archived;
    private Integer _rows;
    private Integer _columns;
    private Integer _rowId;

    public PlateParams(String name, Integer plateSetId, PlateParams.PlateType plateType)
    {
        _name = name;
        _plateSetId = plateSetId;
        _plateType = plateType.getValue();
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

        if (json.has("data"))
        {
            RowMapFactory<Object> factory = new RowMapFactory<>();
            JSONArray data = json.getJSONArray("data");
            for (int i = 0; i < data.length(); i++)
            {
                JSONObject jsonObj = data.getJSONObject(i);
                if (jsonObj != null)
                {
                    Map<String, Object> rowMap = factory.getRowMap();
                    JsonUtil.fillMapShallow(jsonObj, rowMap);
                    _data.add(rowMap);
                }
            }
        }
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("name", _name);
        json.put("description", _description);
        json.put("plateSetId", _plateSetId);
        json.put("plateType", _plateType);  // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
        json.put("assayType", _assayType);
        json.put("template", _template);
        json.put("data", _data);
        return json;
    }

    public String getDescription()
    {
        return _description;
    }

    public PlateParams setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getName()
    {
        return _name;
    }

    /**
     * Sets the plate type for plate creation
     * // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
     * @param plateType
     * @return
     */
    public PlateParams setPlateType(PlateType plateType)
    {
        _plateType = plateType.getValue();
        return this;
    }

    public PlateType getPlateType()
    {
        return PlateType.fromValue(_plateType);
    }

    public Integer getPlateSetId()
    {
        return _plateSetId;
    }

    public String getPlateId()
    {
        return _plateId;
    }

    public List<Map<String, Object>> getData()
    {
        return _data;
    }

    public PlateParams setData(List<Map<String, Object>> data)
    {
        _data = data;
        return this;
    }

    public String getAssayType()
    {
        return _assayType;
    }

    public PlateParams setAssayType(String assayType)
    {
        _assayType = assayType;
        return this;
    }

    public Boolean isTemplate()
    {
        return _template;
    }

    public PlateParams setTemplate(boolean template)
    {
        _template = template;
        return this;
    }

    public Integer getRows()
    {
        return _rows;
    }

    public Integer getColumns()
    {
        return _columns;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public boolean getArchived()
    {
        return _archived;
    }

    // // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
    public enum PlateType{
        Plate_3x4(1, "3x4(12)"),
        Plate_4x6(2, "4x6(24)"),
        Plate_6x8(3, "6x8(48)"),
        Plate_8x12(4, "8x12(96)"),
        Plate_16x24(4, "16x24(384)");

        private final int _value;
        private final String _description;
        private static final Map<Integer, PlateType> _map = new HashMap<>();
        PlateType(int value, String description)
        {
            _value = value;
            _description = description;
        }

        public Integer getValue()
        {
            return _value;
        }

        static {
            for (PlateType plateType : PlateType.values())
            {
                _map.put(plateType.getValue(), plateType);
            }
        }

        public static PlateType fromValue(int value)
        {
            return _map.get(value);
        }
    }
}
