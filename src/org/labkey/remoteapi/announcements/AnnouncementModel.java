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
        _approved = json.optString("approved", null);
        _body = json.optString("body", null);
        _containerId = json.optString("containerId", null);
        _containerPath = json.optString("containerPath", null);
        _created = json.optString("created", null);
        if (json.has("createdBy"))
            _createdBy = json.getLong("createdBy");
        _modified = json.optString("modified", null);
        if (json.has("modifiedBy"))
            _modifiedBy = json.getLong("modifiedBy");
        _discussionSrcIdentifier = json.optString("discussionSrcIdentifier", null);
        _entityId = json.optString("entityId", null);
        _formattedHtml = json.optString("formattedHtml", null);
        _rendererType = json.optString("rendererType", null);
        if (json.has("rowId"))
            _rowId = json.getLong("rowId");
        _title = json.optString("title", null);
        _parent = json.optString("parent", null);

        if (json.has("responses"))
        {
            List<AnnouncementModel> responses = new ArrayList<>();
            JSONArray rawResponses = json.getJSONArray("responses");
            for (int i = 0; i < rawResponses.length(); i++)
                responses.add(new AnnouncementModel(rawResponses.getJSONObject(i)));
            _responses = responses;
        }
    }

    public JSONObject toJSON()
    {
        var json = new JSONObject();
        json.put("approved", getApproved());
        json.put("body", getBody());
        json.put("containerId", getContainerId());
        json.put("containerPath", getContainerPath());
        json.put("created", getCreated());
        json.put("createdBy", getCreatedBy());
        json.put("modified", getModified());
        json.put("modifiedBy", getModifiedBy());
        json.put("discussionSrcIdentifier", getDiscussionSrcIdentifier());
        json.put("entityId", getEntityId());
        json.put("formattedHtml", getFormattedHtml());
        json.put("rendererType", getRendererType());
        json.put("responses", getResponses());
        json.put("rowId", getRowId());
        json.put("title", getTitle());
        json.put("parent", getParent());
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
