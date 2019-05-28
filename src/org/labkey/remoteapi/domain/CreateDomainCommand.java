package org.labkey.remoteapi.domain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CreateDomainCommand extends PostCommand<DomainResponse>
{
    protected String _schemaName;
    protected String _domainName;
    private String _domainKind;
    private List<Map<String, Object>> _columns = new ArrayList<>();

    public CreateDomainCommand(String domainKind, String domainName)
    {
        super("property", "createDomain");
        setDomainKind(domainKind);
        setDomainName(domainName);
    }

    @Override
    protected DomainResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DomainResponse(text, status, contentType, json, this);
    }

    public void setColumns(List<Map<String, Object>> columns)
    {
        _columns = columns;
    }

    public List<Map<String, Object>> getColumns()
    {
        return _columns;
    }

    /**
     * Returns the domain name.
     * @return The domain name.
     */
    public String getDomainName()
    {
        return _domainName;
    }

    /**
     * Sets the schema name
     * @param domainName The new domain name.
     */
    public void setDomainName(String domainName)
    {
        _domainName = domainName;
    }

    /**
     * Returns the domain kind.
     * @return The domain kind.
     */
    public String getDomainKind()
    {
        return _domainKind;
    }

    /**
     * Sets the domain kind
     * @param domainKind The new domain kind.
     */
    public void setDomainKind(String domainKind)
    {
        _domainKind = domainKind;
    }

    /**
     * Returns the schema name.
     * @return The schema name.
     */
    public String getSchemaName()
    {
        return _schemaName;
    }

    /**
     * Sets the schema name
     * @param schemaName The new schema name.
     */
    public void setSchemaName(String schemaName)
    {
        _schemaName = schemaName;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("schemaName", getSchemaName());
        obj.put("domainKind", getDomainKind());

        JSONArray fields = new JSONArray();
        fields.addAll(getColumns());

        JSONObject domainDesign = new JSONObject();
        domainDesign.put("name", getDomainName());
        domainDesign.put("fields", fields);

        obj.put("domainDesign", domainDesign);
        return obj;
    }
}
