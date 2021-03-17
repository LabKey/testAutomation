package org.labkey.remoteapi.workflow;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test params for server-side bean: org.labkey.samplemanagement.model.Task
 */
public class Task implements JSONAware
{
    private String _name;
    private String _description;
    private String _dueDate;
    private String _assayTypes;
    private List<TaskComment> _comments;
    private Long _assignee;
    private Long _status;

    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();

        json.put("name", _name);
        json.put("description", _description);
        json.put("dueDate", _dueDate);
        json.put("assayTypes", _assayTypes);
        if (_comments != null)
        {
            json.put("comments", JSONArray.toJSONString(_comments)); // Comments are stored on the server as JSON
        }
        json.put("assignee", _assignee);
        json.put("status", _status);

        return json;
    }

    @Override
    public String toJSONString()
    {
        return toJSONObject().toJSONString();
    }

    public String getName()
    {
        return _name;
    }

    public Task setName(String name)
    {
        _name = name;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public Task setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getDueDate()
    {
        return _dueDate;
    }

    public Task setDueDate(String dueDate)
    {
        _dueDate = dueDate;
        return this;
    }

    public String getAssayTypes()
    {
        return _assayTypes;
    }

    public Task setAssayTypes(Long... assayTypes)
    {
        _assayTypes = Arrays.stream(assayTypes).map(Object::toString).collect(Collectors.joining(","));
        return this;
    }

    public List<TaskComment> getComments()
    {
        return _comments;
    }

    public Task setComments(TaskComment... comments)
    {
        _comments = Arrays.asList(comments);
        return this;
    }

    public Long getAssignee()
    {
        return _assignee;
    }

    public Task setAssignee(Long assignee)
    {
        _assignee = assignee;
        return this;
    }

    public Long getStatus()
    {
        return _status;
    }

    public Task setStatus(Long status)
    {
        _status = status;
        return this;
    }
}
