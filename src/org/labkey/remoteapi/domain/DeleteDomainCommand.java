package org.labkey.remoteapi.domain;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteDomainCommand extends PostCommand<DeleteDomainResponse>
{
    private String _schemaName;
    private String _queryName;

    public DeleteDomainCommand(String schema, String queryName)
    {
        super("property", "deleteDomain");
        _queryName = queryName;
        _schemaName = schema;
    }

    /**
     * Returns the domain name.
     * @return The domain name.
     */
    public String getQueryName()
    {
        return _queryName;
    }

    /**
     * Sets the schema name
     * @param queryName The new domain name.
     */
    public void setQueryName(String queryName)
    {
        _queryName = queryName;
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
    protected DeleteDomainResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DeleteDomainResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("schemaName", getSchemaName());
        obj.put("queryName", getQueryName());

        return obj;
    }
}
