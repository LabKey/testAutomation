package org.labkey.remoteapi.issues;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final String RESOLUTION = "resolution";
    private final String RESOLVED = "resolved";
    private final String STATUS = "status";

    private final Map<String, Object> _properties = new CaseInsensitiveHashMap<>();
    private final List<File> _attachments = new ArrayList();

    // https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues
    public IssueModel()
    {
    }

    public IssueModel setProperties(Map properties)
    {
        _properties.putAll(properties);
        return this;
    }

    public JSONObject toJSON()
    {
        var json = new JSONObject(_properties);

        // handle attachments
        if (!_attachments.isEmpty())
        {
            List<String> names = _attachments.stream()
                    .map(f -> f.getName())
                    .collect(Collectors.toList());

            json.put("attachment", String.join("|", names));
        }

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

    public Integer getAssignedTo()
    {
        return (Integer)_properties.get(ASSIGNED_TO);
    }

    public IssueModel setAssignedTo(Long assignedTo)
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
        return setProp(TYPE, type);
    }

    public Long getPriority()
    {
        return (Long) _properties.get(PRIORITY);
    }

    public IssueModel setPriority(Long priority)
    {
        return setProp(PRIORITY, priority);
    }

    public String getComment()
    {
        return _properties.get(COMMENT).toString();
    }

    public IssueModel setComment(String comment)
    {
        return setProp(COMMENT, comment);
    }

    public String getResolution()
    {
        return (String) _properties.get(RESOLUTION);
    }

    public IssueModel setResolution(String resolution)
    {
        return setProp(RESOLUTION, resolution);
    }

    public IssueModel setProp(String propName, Object value)
    {
        _properties.put(propName, value);
        return this;
    }

    public IssueModel addAttachment(File attachmentFile)
    {
        _attachments.add(attachmentFile);
        return this;
    }

    public List<File> getAttachments()
    {
        return _attachments;
    }

    public enum IssueAction
    {
        INSERT("insert"),
        UPDATE("update"),
        RESOLVE("resolve"),
        CLOSE("close"),
        REOPEN("reopen");

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
