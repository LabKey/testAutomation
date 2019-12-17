package org.labkey.remoteapi.assay;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.ResponseObject;
import org.labkey.remoteapi.domain.Domain;

import java.util.ArrayList;
import java.util.List;

public class Protocol extends ResponseObject
{
    private Long _protocolId;
    private String _name;
    private String _description;
    private String _providerName;
    private List<Domain> _domains = new ArrayList<>();

    public Protocol()
    {
        super(null);
    }

    public Protocol(JSONObject json)
    {
        super(json);

        _protocolId = (Long)json.get("protocolId");
        _name = (String)json.get("name");
        _description = (String)json.get("description");
        _providerName = (String)json.get("providerName");

        if (json.get("domains") instanceof JSONArray)
        {
            for (Object domain : ((JSONArray)json.get("domains")))
                _domains.add(new Domain((JSONObject)domain));
        }

        // TODO lots more properties to be added here
    }

    public JSONObject toJSONObject()
    {
        JSONObject result = new JSONObject();
        result.put("protocolId", _protocolId);
        result.put("name", _name);
        result.put("description", _description);
        result.put("providerName", _providerName);

        JSONArray domains = new JSONArray();
        result.put("domains", domains);
        for (Domain domain : _domains)
            domains.add(domain.toJSONObject(true));

        // TODO lots more properties to be added here

        return result;
    }

    public Long getProtocolId()
    {
        return _protocolId;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getProviderName()
    {
        return _providerName;
    }

    public void setProviderName(String providerName)
    {
        _providerName = providerName;
    }

    public List<Domain> getDomains()
    {
        return _domains;
    }

    public void setDomains(List<Domain> domains)
    {
        _domains = domains;
    }
}
