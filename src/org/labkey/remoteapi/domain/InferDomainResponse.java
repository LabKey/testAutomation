/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.remoteapi.domain;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InferDomainResponse extends CommandResponse
{
    List<PropertyDescriptor> _fields;

    public InferDomainResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
    }

    public List<PropertyDescriptor> getFields()
    {
        if (_fields == null)
        {
            List<PropertyDescriptor> temp = new ArrayList<>();
            List<Map<String, Object>> fieldsJson = getProperty("fields");
            fieldsJson.forEach(map -> temp.add(new PropertyDescriptor(new JSONObject(map))));
            if (temp.isEmpty())
            {
                throw new IllegalArgumentException("No fields found in response");
            }
            _fields = Collections.unmodifiableList(temp);
        }
        return _fields;
    }
}
