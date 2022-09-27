package org.labkey.remoteapi.issues;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IssueResponseModel
{
    private final List<IssueComment> _issueComments = new ArrayList<>();
    private final Map<String, Object> _serverProps = new CaseInsensitiveHashMap<>();
    private final Map<String, Object> _allProps = new CaseInsensitiveHashMap<>();

    public IssueResponseModel(JSONObject json)
    {
        JSONObject props = (JSONObject) json.get("properties");
        props.toMap().forEach((k, v) -> {
            _serverProps.put(k, v);
            _allProps.put(k, v);
        });

        if (json.get("comments") != null)
        {
            var commentsObj = (JSONArray)json.get("comments");
            for (int i=0; i < commentsObj.length(); i++)
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

    public String getIssueDefName()
    {
        return (String) _allProps.get(ResponseKeys.issueDefName.toString());
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
        Title,
        IssueId,
        issueDefName,
        AssignedTo,
        type,
        priority,
        comment,
        notifyList,
        resolution,
        resolved,
        ResolvedBy,
        Closed,
        ClosedBy,
        Status,
        Modified,
        ModifiedBy
    }
}
