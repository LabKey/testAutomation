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

    public InferDomainResponse(String text, int statusCode, String contentType, JSONObject json, InferDomainCommand sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
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
