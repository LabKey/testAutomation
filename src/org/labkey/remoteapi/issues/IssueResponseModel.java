package org.labkey.remoteapi.issues;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IssueResponseModel
{
    // keys
    private final String TITLE= "Title";
    private final String ISSUE_ID = "IssueId";
    private final String ISSUE_DEF_NAME = "issueDefName";
    private final String ASSIGNED_TO = "AssignedTo";
    private final String TYPE = "type";
    private final String PRIORITY = "priority";
    private final String COMMENT = "comment";
    private final String NOTIFY_LIST = "notifyList";
    private final String RESOLUTION = "resolution";
    private final String RESOLVED = "resolved";
    private final String RESOLVED_BY = "ResolvedBy";
    private final String CLOSED = "Closed";
    private final String CLOSED_BY = "ClosedBy";
    private final String STATUS = "Status";
    private final String MODIFIED = "Modified";
    private final String MODIFIED_BY = "ModifiedBy";
    private final List<IssueComment> _issueComments = new ArrayList<>();
    private final Map<String, Object> _serverProps = new CaseInsensitiveHashMap<>();

    public IssueResponseModel(JSONObject json)
    {
        JSONObject props = (JSONObject) json.get("properties");
        _serverProps.putAll(props);

        if (json.get("comments") != null)
        {
            var commentsObj = (JSONArray)json.get("comments");
            for (int i=0; i < commentsObj.size(); i++)
            {
                _issueComments.add(new IssueComment((JSONObject) commentsObj.get(i)));
            }
        }
    }

    // read-only props from the server
    public String getTitle()
    {
        return (String) _serverProps.get(TITLE);
    }

    public String getStatus()
    {
        return (String) _serverProps.get(STATUS);
    }

    public String closedBy()
    {
        return (String) _serverProps.get(CLOSED_BY);
    }

    public String modifiedBy()
    {
        return (String) _serverProps.get(MODIFIED_BY);
    }

    public String resolvedBy()
    {
        return (String) _serverProps.get(RESOLVED_BY);
    }

    public String resolution()
    {
        return (String) _serverProps.get(RESOLUTION);
    }

    public String getResolved()
    {
        return (String) _serverProps.get(RESOLVED);
    }

    public Long getPriority()
    {
        return (Long) _serverProps.get(PRIORITY);
    }

    public String getType()
    {
        return (String) _serverProps.get(TYPE);
    }

    public List<IssueComment> getComments()
    {
        return _issueComments;
    }
}
