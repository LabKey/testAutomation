package org.labkey.remoteapi.issues;

import org.json.simple.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.Map;

public class IssueComment
{
    private final String CREATED_BY_NAME = "createdByName";
    private final String COMMENT = "comment";
    private final String TITLE = "title";

    private final Map<String, Object> _properties = new CaseInsensitiveHashMap<>();

    public IssueComment(JSONObject json)
    {
        _properties.putAll(json);
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

    public Map<String, Object> getProperties()
    {
        return _properties;
    }
}
