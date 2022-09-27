package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IssueModel
{
    private final Map<String, Object> _properties = new CaseInsensitiveHashMap<>();
    private final List<File> _attachments = new ArrayList<>();

    // https://www.labkey.org/Documentation/wiki-page.view?name=sampleJSscripts#issues
    public IssueModel()
    {
    }

    public IssueModel setProperties(Map<String, String> properties)
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
                .map(File::getName)
                .collect(Collectors.toList());

            json.put("attachment", String.join("|", names));
        }

        return json;
    }

    public IssueAction getAction()
    {
        return IssueAction.valueOf((String) getProp(ModelKeys.action));
    }

    public IssueModel setAction(IssueAction action)
    {
        return setProp(ModelKeys.action, action);
    }

    public Long getIssueId()
    {
        return (Long) getProp(ModelKeys.issueid);
    }

    public IssueModel setIssueId(Long issueId)
    {
        return setProp(ModelKeys.issueid, issueId);
    }

    public String getTitle()
    {
        return (String) getProp(ModelKeys.title);
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
        return (Integer) getProp(ModelKeys.assignedto);
    }

    public IssueModel setAssignedTo(Long assignedTo)
    {
        return setProp(ModelKeys.assignedto, assignedTo);
    }

    public String getIssueDefName()
    {
        return (String) getProp(ModelKeys.issueDefName);
    }

    public IssueModel setIssueDefName(String issueDefName)
    {
        return setProp(ModelKeys.issueDefName, issueDefName);
    }

    public String getType()
    {
        return (String) getProp(ModelKeys.type);
    }

    public IssueModel setType(String type)
    {
        return setProp(ModelKeys.type, type);
    }

    public Long getPriority()
    {
        return (Long) getProp(ModelKeys.priority);
    }

    public IssueModel setPriority(Long priority)
    {
        return setProp(ModelKeys.priority, priority);
    }

    public String getComment()
    {
        return (String) getProp(ModelKeys.comment);
    }

    public IssueModel setComment(String comment)
    {
        return setProp(ModelKeys.comment, comment);
    }

    public String getResolution()
    {
        return (String) getProp(ModelKeys.resolution);
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

    public Object getProp(ModelKeys modelKeys)
    {
        return  _properties.get(modelKeys.toString());
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
        title,
        issueid,
        issueDefName,
        assignedto,
        action,
        type,
        priority,
        comment,
        notifyList,
        resolution,
        resolved,
        status
    }

    public enum IssueAction
    {
        insert,
        update,
        resolve,
        close,
        reopen
    }
}
