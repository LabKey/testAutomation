package org.labkey.remoteapi.domain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SaveDomainCommand extends DomainCommand
{

    private int _domainId;
    private String _domainURI;
    public SaveDomainCommand (String domainKind, String domainName)
    {
        super("property", "saveDomain");
        setDomainKind(domainKind);
        setDomainName(domainName);
    }

    public int getDomainId()
    {
        return _domainId;
    }

    public void setDomainId(int domainId)
    {
        _domainId = domainId;
    }

    public String getDomainURI()
    {
        return _domainURI;
    }

    public void setDomainURI(String domainURI)
    {
        _domainURI = domainURI;
    }


    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("schemaName", getSchemaName());
        obj.put("domainKind", getDomainKind());
        obj.put("queryName", getDomainName());

        JSONArray fields = new JSONArray();
        fields.addAll(getColumns());

        JSONObject domainDesign = new JSONObject();
        domainDesign.put("name", getDomainName());
        domainDesign.put("fields", fields);
        domainDesign.put("domainId", getDomainId());
        domainDesign.put("domainURI", getDomainURI());

        obj.put("domainDesign", domainDesign);
        return obj;
    }
}
