/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.remoteapi.plate;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreatePlateParams
{
    private String _assayType; // blank or Standard
    private List<Map<String, Object>> _data = new ArrayList<>();
    private String _description;
    private final String _name;
    private final Integer _plateSetId;
    private Integer _plateType; // 1- 3x4(12), 2- 4x6(24), 3-6x8(48), 4-8x12(96), 5-16x24(384)
    private boolean _template;

    public CreatePlateParams(String name, Integer plateSetId, PlateTypes plateType)
    {
        _name = name;
        _plateSetId = plateSetId;
        _plateType = plateType.getRowId();
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
     */
    public CreatePlateParams setPlateType(PlateTypes plateType)
    {
        _plateType = plateType.getRowId();
        return this;
    }

    public PlateTypes getPlateType()
    {
        return PlateTypes.fromRowId(_plateType);
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
