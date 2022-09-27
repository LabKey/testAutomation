package org.labkey.remoteapi.issues;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IssueComment
{
    private final String CREATED_BY_NAME = "createdByName";
    private final String COMMENT = "comment";
    private final String TITLE = "title";
    private final String ATTACHMENTS = "attachments";

    private final Map<String, Object> _properties = new CaseInsensitiveHashMap<>();

    public IssueComment(JSONObject json)
    {
        _properties.putAll(json.toMap());
    }

    public String getCreatedBy()
    {
        return (String)_properties.get(CREATED_BY_NAME);
    }

    public String getComment()
    {
        return (String) _properties.get(COMMENT);
    }

    public String getTitle()
    {
        return (String) _properties.get(TITLE);
    }

    public List<String> getAttachments()
    {
        List<String> attachments= new ArrayList<>();
        if (_properties.get("attachments") != null)
        {
            JSONArray attachmentsArray = (JSONArray) _properties.get(ATTACHMENTS);
            for(int i=0; i < attachmentsArray.length(); i++)
            {
               attachments.add(attachmentsArray.getString(i));
            }
        }
        return attachments;
    }

    public Map<String, Object> getProperties()
    {
        return _properties;
    }
}
