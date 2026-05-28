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


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreatePlateSetParams
{
    // This will match PlateController.CreatePlateSetForm
    private String _name;
    private String _description;
    private List<CreatePlateSetPlate> _plates = new ArrayList<>();
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
        if (!_plates.isEmpty())
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
