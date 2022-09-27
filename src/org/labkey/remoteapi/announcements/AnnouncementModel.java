package org.labkey.remoteapi.announcements;

import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.test.util.WikiHelper;

import java.util.ArrayList;
import java.util.List;

// POJO for working with responses from Announcements APIs.
// This is modeled after AnnouncementModel on the server.
public class AnnouncementModel
{
    private String _approved;
    private String _body;
    private String _containerId;
    private String _containerPath;
    private String _created;
    private Long _createdBy;
    private String _modified;
    private Long _modifiedBy;
    private String _discussionSrcIdentifier;
    private String _entityId;
    private String _formattedHtml;
    private String _rendererType;
    private List<AnnouncementModel> _responses = new ArrayList<>();
    private Long _rowId;
    private String _title;
    private String _parent;

    public AnnouncementModel()
    {
    }

    public AnnouncementModel(JSONObject json)
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
            _createdBy = (Long) json.get("createdBy");
        if (json.get("modified") != null)
            _modified = json.get("modified").toString();
        if (json.get("modifiedBy") != null)
            _modifiedBy = (Long) json.get("modifiedBy");
        if (json.get("discussionSrcIdentifier") != null)
            _discussionSrcIdentifier = json.get("discussionSrcIdentifier").toString();
        if (json.get("entityId") != null)
            _entityId = json.get("entityId").toString();
        if (json.get("formattedHtml") != null)
            _formattedHtml = json.get("formattedHtml").toString();
        if (json.get("rendererType") != null)
            _rendererType = json.get("rendererType").toString();
        if (json.get("rowId") != null)
            _rowId = (Long) json.get("rowId");
        if (json.get("title") != null)
            _title = json.get("title").toString();
        if (json.get("parent") != null)
            _parent = json.get("parent").toString();

        if (json.get("responses") != null)
        {
            var responses = new ArrayList<AnnouncementModel>();
            var rawResponses = (JSONArray) json.get("responses");
            for (int i = 0; i < rawResponses.length(); i++)
                responses.add(new AnnouncementModel(rawResponses.getJSONObject(i)));
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
        if (getParent() != null) json.put("parent", getParent());
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

    public AnnouncementModel setBody(String body)
    {
        _body = body;
        return this;
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

    public Long getCreatedBy()
    {
        return _createdBy;
    }

    public void setCreatedBy(Long createdBy)
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

    public Long getModifiedBy()
    {
        return _modifiedBy;
    }

    public void setModifiedBy(Long modifiedBy)
    {
        _modifiedBy = modifiedBy;
    }

    public String getDiscussionSrcIdentifier()
    {
        return _discussionSrcIdentifier;
    }

    public AnnouncementModel setDiscussionSrcIdentifier(String discussionSrcIdentifier)
    {
        _discussionSrcIdentifier = discussionSrcIdentifier;
        return this;
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

    public AnnouncementModel setRendererType(WikiHelper.WikiRendererType rendererType)
    {
        _rendererType = rendererType.toString();
        return this;
    }

    public List<AnnouncementModel> getResponses()
    {
        return _responses;
    }

    public void setResponses(List<AnnouncementModel> responses)
    {
        _responses = responses;
    }

    public Long getRowId()
    {
        return _rowId;
    }

    public void setRowId(Long rowId)
    {
        _rowId = rowId;
    }

    public String getTitle()
    {
        return _title;
    }

    public AnnouncementModel setTitle(String title)
    {
        _title = title;
        return this;
    }

    public String getParent()
    {
        return _parent;
    }

    public void setParent(String parent)
    {
        _parent = parent;
    }
}
