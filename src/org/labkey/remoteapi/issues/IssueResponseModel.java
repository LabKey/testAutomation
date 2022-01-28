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
    public Long getAssignedTo()
    {
        return (Long) getProp(ResponseKeys.AssignedTo);
    }

    public String getTitle()
    {
        return (String) getProp(ResponseKeys.Title);
    }

    public String getStatus()
    {
        return (String) getProp(ResponseKeys.Status);
    }

    public String getClosedBy()
    {
        return (String) getProp(ResponseKeys.ClosedBy);
    }

    public String getModifiedBy()
    {
        return (String) getProp(ResponseKeys.ModifiedBy);
    }

    public String getResolvedBy()
    {
        return (String) getProp(ResponseKeys.ResolvedBy);
    }

    public String getResolution()
    {
        return (String) getProp(ResponseKeys.resolution);
    }

    public String getResolved()
    {
        return (String) getProp(ResponseKeys.resolved);
    }

    public Long getPriority()
    {
        return (Long) getProp(ResponseKeys.priority);
    }

    public String getType()
    {
        return (String) getProp(ResponseKeys.type);
    }

    public List<IssueComment> getComments()
    {
        return _issueComments;
    }

    /**
     * for ad-hoc querying of _serverProps
     * @param key
     * @return  the object at that key
     */
    public Object getProperties(String key)
    {
        return _serverProps.get(key);
    }

    public Object getProp(ResponseKeys key)
    {
        return _serverProps.get(key.toString());
    }

    /**
     * Contains the case-sensitive keys used by issues-getIssue.api
     */
    public enum ResponseKeys{
        Title(),
        IssueId(),
        issueDefName(),
        AssignedTo(),
        type(),
        priority(),
        comment(),
        notifyList(),
        resolution(),
        resolved(),
        ResolvedBy(),
        Closed(),
        ClosedBy(),
        Status(),
        Modified(),
        ModifiedBy();
        
        ResponseKeys(){}
    }
}
