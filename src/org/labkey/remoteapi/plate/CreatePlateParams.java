package org.labkey.remoteapi.plate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.RowMapFactory;
import org.labkey.api.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreatePlateParams
{
    private String _assayType; // blank or Standard
    private List<Map<String, Object>> _data = new ArrayList<>();
    private String _description;
    private String _name;
    private Integer _plateSetId;
    private Integer _plateType; // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
    private boolean _template;

    public CreatePlateParams(String name, Integer plateSetId, PlateTypes plateType)
    {
        _name = name;
        _plateSetId = plateSetId;
        _plateType = plateType.getValue();
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("name", _name);
        json.put("description", _description);
        json.put("plateSetId", _plateSetId);
        json.put("plateType", _plateType);
        json.put("assayType", _assayType);
        json.put("template", _template);
        json.put("data", _data);
        return json;
    }

    public String getDescription()
    {
        return _description;
    }

    public CreatePlateParams setDescription(String description)
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
    public CreatePlateParams setPlateType(PlateTypes plateType)
    {
        _plateType = plateType.getValue();
        return this;
    }

    public PlateTypes getPlateType()
    {
        return PlateTypes.fromValue(_plateType);
    }

    public List<Map<String, Object>> getData()
    {
        return _data;
    }

    public CreatePlateParams setData(List<Map<String, Object>> data)
    {
        _data = data;
        return this;
    }

    public String getAssayType()
    {
        return _assayType;
    }

    public CreatePlateParams setAssayType(String assayType)
    {
        _assayType = assayType;
        return this;
    }

    public Boolean isTemplate()
    {
        return _template;
    }

    public CreatePlateParams setTemplate(boolean template)
    {
        _template = template;
        return this;
    }

}
