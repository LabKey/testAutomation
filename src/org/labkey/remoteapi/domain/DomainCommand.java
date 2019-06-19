package org.labkey.remoteapi.domain;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomainCommand extends PostCommand<DomainResponse>
{
    protected String _schemaName;
    protected String _domainName;
    private String _domainKind;
    private List<Map<String, Object>> _columns = new ArrayList<>();

    public DomainCommand(String controllerName, String actionName)
    {
        super(controllerName, actionName);
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
     * @param domainName The domain name.
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
     * @param domainKind The domain kind.
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
     * @param schemaName The schema name.
     */
    public void setSchemaName(String schemaName)
    {
        _schemaName = schemaName;
    }

}
