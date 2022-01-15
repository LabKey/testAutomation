package org.labkey.remoteapi.issues;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IssueModel
{
    private IssueAction _action;
    private Integer _issueId;
    private String _title;
    private List<String> _notifyList;
    private Integer _assignedTo;
    private String _issueDefName;
    private String _type;       // probably convert ot enum IssueType
    private Integer _priority;  // probably convert to enum IssuePriority
    private String _comment;

    // there's probably a need for IssueModel to expose setters/getters for arbitrary fields/properties
    // https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues

    // we ma

    public IssueModel()
    {
    }

    public IssueModel(JSONObject json)
    {
        if (json.get("action") != null)
            _action = IssueAction.valueOf(json.get("action").toString());
        if (json.get("issueid") != null)
            _issueId = (Integer) json.get("issueid");
        if (json.get("title") != null)
            _title = json.get("title").toString();
        // notifyList
//        if (json.get("notifyList") != null)       // need to figure this out
//            _notifyList = json.get("notifyList")
        if (json.get("assignedTo") != null)
            _assignedTo = (Integer) json.get("assignedTo");
        if (json.get("issueDefName") != null)
            _issueDefName = json.get("issueDefName").toString();
        if (json.get("type") != null)
            _type = json.get("type").toString();
        if (json.get("priority") != null)
            _priority = (Integer) json.get("priority");
        if (json.get("comment") != null)
            _comment = json.get("comment").toString();
    }

    public JSONObject toJSON()
    {
        var json = new JSONObject();
        if (getAction() != null)
            json.put("action", _action.getValue());
        if (getTitle() != null)
            json.put("title", _title);
        if (getIssueId() != null)
            json.put("issueid", _issueId);
        if (getNotifyList() != null)
            json.put("notifyList", JSONArray.toJSONString(_notifyList));
        if (getAssignedTo() != null)
            json.put("assignedTo", _assignedTo);
        if (getIssueDefName() != null)
            json.put("issueDefName", _issueDefName);
        if (getType() != null)
            json.put("type", _type);
        if (getPriority() != null)
            json.put("priority", _priority);
        if (getComment() != null)
            json.put("comment", _comment);
        return json;
    }

    public IssueAction getAction()
    {
        return _action;
    }

    public IssueModel setAction(IssueAction action)
    {
        _action = action;
        return this;
    }

    public Integer getIssueId()
    {
        return _issueId;
    }

    public IssueModel setIssueId(Integer issueId)
    {
        _issueId = issueId;
        return this;
    }

    public String getTitle()
    {
        return _title;
    }

    public IssueModel setTitle(String title)
    {
        _title = title;
        return this;
    }

    // per https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues, for a single notify user
    // you can supply an email address as a string; for multiple notifies, userIDs are necessary
    public List<String> getNotifyList()
    {
        return _notifyList;
    }

    public IssueModel setNotifyList(List<String> notifyList)
    {
        _notifyList = notifyList;
        return this;
    }

    public Integer getAssignedTo()
    {
        return _assignedTo;
    }

    public IssueModel setAssignedTo(Integer assignedTo)
    {
        _assignedTo = assignedTo;
        return this;
    }

    public String getIssueDefName()
    {
        return _issueDefName;
    }

    public IssueModel setIssueDefName(String issueDefName)
    {
        _issueDefName = issueDefName;
        return this;
    }

    public String getType()
    {
        return _type;
    }

    public IssueModel setType(String type)
    {
        _type = type;
        return this;
    }

    public Integer getPriority()
    {
        return _priority;
    }

    public IssueModel setPriority(Integer priority)
    {
        _priority = priority;
        return this;
    }

    public String getComment()
    {
        return _comment;
    }

    public IssueModel setComment(String comment)
    {
        _comment = comment;
        return this;
    }


    public enum IssueAction{
        INSERT("insert"),
        UPDATE("update"),
        CLOSE("close"),
        OPEN("open");

        IssueAction(String value)
        {
            _value = value;
        }
        public String getValue()
        {
            return _value;
        }
        private final String _value;
    }
}
