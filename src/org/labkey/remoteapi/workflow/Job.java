package org.labkey.remoteapi.workflow;

import org.json.simple.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test params for server-side bean: org.labkey.samplemanagement.model.Job
 */
public class Job
{
    private String _name;
    private String _description;
    private Long _assignee;
    private String _notifyList; // Stored as a comma-separated list of user IDs
    private String _startDate;
    private String _dueDate;
    private Long _priority;
    private Long _ordinal;
    private List<Long> _sampleIds = new ArrayList<>();
    private List<Task> _tasks = new ArrayList<>();

    public static Job fromMap(Map<String, Object> map)
    {
        Job job = new Job();
        job.setName((String) map.get("name"));
        job.setDescription((String) map.get("description"));
        job.setStartDate((String) map.get("startDate"));
        job.setDueDate((String) map.get("dueDate"));
        job.setNotifyList((String) map.get("notifyList"));
        job.setAssignee((Long) map.get("assignee"));
        job.setPriority((Long) map.get("priority"));
        job.setOrdinal((Long) map.get("ordinal"));
        job.setSampleIds(new ArrayList<>((List<Long>) map.get("sampleIds")));
        // job.setTasks(map.get("tasks")); // TODO: Not currently needed for testing

        return job;
    }
    
    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject();

        json.put("name", _name);
        json.put("description", _description);
        json.put("startDate", _startDate);
        json.put("dueDate", _dueDate);
        json.put("notifyList", _notifyList);
        json.put("assignee", _assignee);
        json.put("priority", _priority);
        json.put("ordinal", _ordinal);
        json.put("sampleIds", _sampleIds);
        json.put("tasks", _tasks);

        return json;
    }

    public String getName()
    {
        return _name;
    }

    public Job setName(String name)
    {
        _name = name;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public Job setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getStartDate()
    {
        return _startDate;
    }

    public Job setStartDate(String startDate)
    {
        _startDate = startDate;
        return this;
    }

    public String getDueDate()
    {
        return _dueDate;
    }

    public Job setDueDate(String dueDate)
    {
        _dueDate = dueDate;
        return this;
    }

    public List<Integer> getNotifyList()
    {
        return _notifyList == null ? null :
                Arrays.stream(_notifyList.split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    public Job setNotifyList(Integer... userIds)
    {
        _notifyList = Arrays.stream(userIds).map(Long::toString).collect(Collectors.joining(","));
        return this;
    }

    public Job setNotifyList(String notifyList)
    {
        _notifyList = notifyList;
        return this;
    }

    public Long getAssignee()
    {
        return _assignee;
    }

    public Job setAssignee(Long assignee)
    {
        _assignee = assignee;
        return this;
    }

    public Long getPriority()
    {
        return _priority;
    }

    public Job setPriority(Long priority)
    {
        _priority = priority;
        return this;
    }

    public Long getOrdinal()
    {
        return _ordinal;
    }

    public Job setOrdinal(Long ordinal)
    {
        _ordinal = ordinal;
        return this;
    }

    public List<Long> getSampleIds()
    {
        return _sampleIds;
    }

    public Job setSampleIds(List<Long> sampleIds)
    {
        _sampleIds = sampleIds;
        return this;
    }

    public Job setSampleIds(Long... sampleIds)
    {
        _sampleIds = Arrays.asList(sampleIds);
        return this;
    }

    public List<Task> getTasks()
    {
        return _tasks;
    }

    public Job setTasks(List<Task> tasks)
    {
        _tasks = tasks;
        return this;
    }

    public Job setTasks(Task... tasks)
    {
        _tasks = Arrays.asList(tasks);
        return this;
    }

    public Job setFiles(File... files)
    {
        throw new UnsupportedOperationException("Use AddJobAttachmentsCommand");
    }
}
