package org.labkey.test.util.announcements;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// POJO for working with responses from Announcements APIs.
// This is modeled after AnnouncementModel on the server.
public class TestAnnouncementModel
{
    private String _approved;
    private String _body;
    private String _containerId;
    private String _containerPath;
    private String _created;
    private Integer _createdBy;
    private String _modified;
    private Integer _modifiedBy;
    private String _discussionSrcIdentifier;
    private String _entityId;
    private String _formattedHtml;
    private String _rendererType;
    private List<TestAnnouncementModel> _responses = new ArrayList<>();
    private Integer _rowId;
    private String _title;

    public TestAnnouncementModel()
    {
    }

    public TestAnnouncementModel(JSONObject json)
    {
        if (json.get("approved") != null)
            _approved = json.get("approved").toString();
        if (json.get("body") != null)
            _body = json.get("body").toString();
        if (json.get("containerId") != null)
            _containerId = json.get("containerId").toString();
        if (json.get("containerPath") != null)
            _containerPath = json.get("containerPath").toString();
        if (json.get("created") != null)
            _created = json.get("created").toString();
        if (json.get("createdBy") != null)
            _createdBy = (Integer) json.get("createdBy");
        if (json.get("modified") != null)
            _modified = json.get("modified").toString();
        if (json.get("modifiedBy") != null)
            _modifiedBy = (Integer) json.get("modifiedBy");
        if (json.get("discussionSrcIdentifier") != null)
            _discussionSrcIdentifier = json.get("discussionSrcIdentifier").toString();
        if (json.get("entityId") != null)
            _entityId = json.get("entityId").toString();
        if (json.get("formattedHtml") != null)
            _formattedHtml = json.get("formattedHtml").toString();
        if (json.get("rendererType") != null)
            _rendererType = json.get("rendererType").toString();
        if (json.get("rowId") != null)
            _rowId = (Integer) json.get("rowId");
        if (json.get("title") != null)
            _title = json.get("title").toString();

        if (json.get("responses") != null)
        {
            var responses = new ArrayList<TestAnnouncementModel>();
            var rawResponses = (JSONArray) json.get("responses");
            for (int i = 0; i < rawResponses.length(); i++)
                responses.add(new TestAnnouncementModel((JSONObject) rawResponses.get(i)));
            _responses = responses;
        }
    }

    public JSONObject toJSON()
    {
        var json = new JSONObject();
        if (getApproved() != null) json.put("approved", getApproved());
        if (getBody() != null) json.put("body", getBody());
        if (getContainerId() != null) json.put("containerId", getContainerId());
        if (getContainerPath() != null) json.put("containerPath", getContainerPath());
        if (getCreated() != null) json.put("created", getCreated());
        if (getCreatedBy() != null) json.put("createdBy", getCreatedBy());
        if (getModified() != null) json.put("modified", getModified());
        if (getModifiedBy() != null) json.put("modifiedBy", getModifiedBy());
        if (getDiscussionSrcIdentifier() != null) json.put("discussionSrcIdentifier", getDiscussionSrcIdentifier());
        if (getEntityId() != null) json.put("entityId", getEntityId());
        if (getFormattedHtml() != null) json.put("formattedHtml", getFormattedHtml());
        if (getRendererType() != null) json.put("rendererType", getRendererType());
        if (getResponses() != null && getResponses().size() > 0) json.put("responses", getResponses());
        if (getRowId() != null) json.put("rowId", getRowId());
        if (getTitle() != null) json.put("title", getTitle());
        return json;
    }

    public String getApproved()
    {
        return _approved;
    }

    public void setApproved(String approved)
    {
        _approved = approved;
    }

    public String getBody()
    {
        return _body;
    }

    public void setBody(String body)
    {
        _body = body;
    }

    public String getContainerId()
    {
        return _containerId;
    }

    public void setContainerId(String containerId)
    {
        _containerId = containerId;
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public void setContainerPath(String containerPath)
    {
        _containerPath = containerPath;
    }

    public String getCreated()
    {
        return _created;
    }

    public void setCreated(String created)
    {
        _created = created;
    }

    public Integer getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(Integer createdBy)
    {
        _createdBy = createdBy;
    }

    public String getModified()
    {
        return _modified;
    }

    public void setModified(String modified)
    {
        _modified = modified;
    }

    public Integer getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(Integer modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public String getDiscussionSrcIdentifier()
    {
        return _discussionSrcIdentifier;
    }

    public void setDiscussionSrcIdentifier(String discussionSrcIdentifier)
    {
        _discussionSrcIdentifier = discussionSrcIdentifier;
    }

    public String getEntityId()
    {
        return _entityId;
    }

    public void setEntityId(String entityId)
    {
        _entityId = entityId;
    }

    public String getFormattedHtml()
    {
        return _formattedHtml;
    }

    public void setFormattedHtml(String formattedHtml)
    {
        _formattedHtml = formattedHtml;
    }

    public String getRendererType()
    {
        return _rendererType;
    }

    public void setRendererType(String rendererType)
    {
        _rendererType = rendererType;
    }

    public List<TestAnnouncementModel> getResponses()
    {
        return _responses;
    }

    public void setResponses(List<TestAnnouncementModel> responses)
    {
        _responses = responses;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public void setRowId(Integer rowId)
    {
        _rowId = rowId;
    }

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(String title)
    {
        _title = title;
    }
}
