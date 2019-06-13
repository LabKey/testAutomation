package org.labkey.remoteapi.domain;

import org.json.simple.JSONObject;

public class GetDomainCommand  extends DomainCommand
{
    private Integer _domainId;

    public GetDomainCommand(String schemaName, String domainName)
    {
        super("property", "getDomain");
        _schemaName = schemaName;
        _domainName = domainName;
    }

    public GetDomainCommand(Integer domainId)
    {
        super("property", "getDomain");
        _domainId = domainId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();

        if (_schemaName != null && _domainName != null)
        {
            obj.put("schemaName", getSchemaName());
            obj.put("queryName", getDomainName());
        }
        else
        {
            obj.put("domainId", _domainId);
        }
        return obj;
    }
}
