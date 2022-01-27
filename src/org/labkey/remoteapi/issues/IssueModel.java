package org.labkey.remoteapi.issues;

import org.json.simple.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IssueModel
{
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
        return IssueAction.valueOf(_properties.get(ModelKeys.action).toString());
    }

    public IssueModel setAction(IssueAction action)
    {
        return setProp(ModelKeys.action, action);
    }

    public Long getIssueId()
    {
        if (_properties.get(ModelKeys.issueid) != null)
            return (Long)_properties.get(ModelKeys.issueid);
        else
            return null;
    }

    public IssueModel setIssueId(Long issueId)
    {
        return setProp(ModelKeys.issueid, issueId);
    }

    public String getTitle()
    {
        return (String) _properties.get(ModelKeys.title);
    }

    public IssueModel setTitle(String title)
    {
        return setProp(ModelKeys.title, title);
    }

    // per https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues, for a single notify user
    // you can supply an email address as a string; for multiple notifies, userIDs are necessary
    public String getNotify()
    {
        return (String) _properties.get(ModelKeys.notifyList.toString());
    }

    /**
     * when notify is just 1 user, you can provide their email
     * @param notify
     * @return
     */
    public IssueModel setNotify(String notify)
    {
        return setProp(ModelKeys.notifyList, notify);
    }

    public Integer getAssignedTo()
    {
        return (Integer)_properties.get(ModelKeys.assignedto);
    }

    public IssueModel setAssignedTo(Long assignedTo)
    {
        return setProp(ModelKeys.assignedto, assignedTo);
    }

    public String getIssueDefName()
    {
        return (String) _properties.get(ModelKeys.issueDefName.toString());
    }

    public IssueModel setIssueDefName(String issueDefName)
    {
        return setProp(ModelKeys.issueDefName, issueDefName);
    }

    public String getType()
    {
        return (String) _properties.get(ModelKeys.type);
    }

    public IssueModel setType(String type)
    {
        return setProp(ModelKeys.type, type);
    }

    public Long getPriority()
    {
        return (Long) _properties.get(ModelKeys.priority);
    }

    public IssueModel setPriority(Long priority)
    {
        return setProp(ModelKeys.priority, priority);
    }

    public String getComment()
    {
        return _properties.get(ModelKeys.comment).toString();
    }

    public IssueModel setComment(String comment)
    {
        return setProp(ModelKeys.comment, comment);
    }

    public String getResolution()
    {
        return (String) _properties.get(ModelKeys.resolution.toString());
    }

    public IssueModel setResolution(String resolution)
    {
        return setProp(ModelKeys.resolution, resolution);
    }

    public IssueModel setProp(ModelKeys propKey, Object value)
    {
        return setProp(propKey.toString(), value);
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

    public enum ModelKeys
    {
        title("title"),
        issueid("issueid"),
        issueDefName("issueDefName"),
        assignedto("assignedto"),
        action("action"),
        type("type"),
        priority("priority"),
        comment("comment"),
        notifyList("notifyList"),
        resolution("resolution"),
        resolved("resolved"),
        status("status");

        ModelKeys(String value)
        {
            _value = value;
        }
        private final String _value;
    }

    public enum IssueAction
    {
        insert("insert"),
        update("update"),
        resolve("resolve"),
        close("close"),
        reopen("reopen");

        IssueAction(String value)
        {
            _value = value;
        }
        private final String _value;
    }
}
