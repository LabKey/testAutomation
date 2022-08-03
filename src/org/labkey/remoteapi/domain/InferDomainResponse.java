package org.labkey.remoteapi.domain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            JSONArray fieldsJson = getProperty("fields");
            fieldsJson.forEach(fieldJson -> temp.add(new PropertyDescriptor((JSONObject) fieldJson)));
            if (temp.isEmpty())
            {
                throw new IllegalArgumentException("No fields found in response");
            }
            _fields = Collections.unmodifiableList(temp);
        }
        return _fields;
    }
}
