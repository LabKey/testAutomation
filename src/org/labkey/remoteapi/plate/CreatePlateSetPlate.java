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

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class CreatePlateSetPlate
{
    private final String _name;
    private final Integer _plateType;
    private final List<Map<String, Object>> _data;

    public CreatePlateSetPlate(@Nullable String name, PlateTypes plateType, List<Map<String, Object>> data)
    {
        _name = name;
        _plateType = plateType.getRowId();
        _data = data;
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        if (_name != null)
            json.put("name", _name);
        json.put("plateType", _plateType);
        if (_data != null && !_data.isEmpty())
        {
            JSONArray data = new JSONArray();
            for (Map<String, Object> row : _data)
                data.put(row);
            json.put("data", data);
        }
        return json;
    }

    public PlateTypes getPlateType()
    {
        return PlateTypes.fromRowId(_plateType);
    }

    public List<Map<String, Object>> getData()
    {
        return _data;
    }
}
