package org.labkey.remoteapi.issues;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IssueModel
{
    // keys
    private final String TITLE= "title";
    private final String ISSUE_ID = "issueid";
    private final String ISSUE_DEF_NAME = "issueDefName";
    private final String ASSIGNED_TO = "assignedto";
    private final String ACTION = "action";
    private final String TYPE = "type";
    private final String PRIORITY = "priority";
    private final String COMMENT = "comment";
    private final String NOTIFY_LIST = "notifyList";
    private Map<String, Object> _properties = new HashMap<>();

    // https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues
    public IssueModel()
    {
    }

    public IssueModel setProperties(Map properties)
    {
        _properties = properties;
        return this;
    }

    // todo: c'tor for json
//    public IssueModel(JSONObject json)
//    {
//          convert json to map here
//    }

    public JSONObject toJSON()
    {
        var json = new JSONObject(_properties);
        return json;
    }

    public IssueAction getAction()
    {
        return IssueAction.valueOf(_properties.get(ACTION).toString());
    }

    public IssueModel setAction(IssueAction action)
    {
        _properties.put(ACTION, action.getValue());
        return this;
    }

    public Long getIssueId()
    {
        if (_properties.get(ISSUE_ID) != null)
            return (Long)_properties.get(ISSUE_ID);
        else
            return null;
    }

    public IssueModel setIssueId(Long issueId)
    {
        _properties.put(ISSUE_ID, issueId);
        return this;
    }

    public String getTitle()
    {
        return (String) _properties.get(TITLE);
    }

    public IssueModel setTitle(String title)
    {
        _properties.put(TITLE, title);
        return this;
    }

    // per https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues, for a single notify user
    // you can supply an email address as a string; for multiple notifies, userIDs are necessary
    public String getNotify()
    {
        return (String) _properties.get("notifyList");
    }

    /**
     * when notify is just 1 user, you can provide their email
     * @param notify
     * @return
     */
    public IssueModel setNotify(String notify)
    {
        _properties.put(NOTIFY_LIST, notify);
        return this;
    }
//    public IssueModel setNotifyList(List<Integer> notifyList)
//    {
//        _properties.put(NOTIFY_LIST, new JSONArray(notifyList))
//    }

    public Integer getAssignedTo()
    {
        return (Integer)_properties.get(ASSIGNED_TO);
    }

    public IssueModel setAssignedTo(Integer assignedTo)
    {
        _properties.put(ASSIGNED_TO, assignedTo);
        return this;
    }

    public String getIssueDefName()
    {
        return (String) _properties.get(ISSUE_DEF_NAME);
    }

    public IssueModel setIssueDefName(String issueDefName)
    {
        _properties.put(ISSUE_DEF_NAME, issueDefName);
        return this;
    }

    public String getType()
    {
        return _properties.get(TYPE).toString();
    }

    public IssueModel setType(String type)
    {
        _properties.put(TYPE, type);
        return this;
    }

    public Integer getPriority()
    {
        return (Integer)_properties.get(PRIORITY);
    }

    public IssueModel setPriority(Integer priority)
    {
        _properties.put(PRIORITY, priority);
        return this;
    }

    public String getComment()
    {
        return _properties.get(COMMENT).toString();
    }

    public IssueModel setComment(String comment)
    {
        _properties.put(COMMENT, comment);
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
