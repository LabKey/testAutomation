package org.labkey.test.util;

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.olap.MdxCommand;
import org.labkey.remoteapi.olap.MdxResponse;
import org.labkey.test.WebTestHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: tgaluhn
 * Date: 3/31/14
 */
public class OlapHelper
{
    private String _baseUrl = WebTestHelper.getBaseURL();
    private String _username = PasswordUtil.getUsername();
    private String _password = PasswordUtil.getPassword();
    private String _configId;
    private String _schemaName;
    private String _folderPath;

    public OlapHelper(String configId, String schemaName, String folderPath)
    {
        _configId = configId;
        _schemaName = schemaName;
        _folderPath = folderPath;
    }

    /**
     * Execute an MDX query and return the full response
     * @param query
     * @return
     * @throws Exception
     */
    public MdxResponse executeMdx(String query) throws Exception
    {
        MdxResponse response;
        Connection cn = new Connection(_baseUrl, _username, _password);
        MdxCommand cmd = new MdxCommand(_configId, _schemaName, query);
        response = cmd.execute(cn, _folderPath);
        return response;
    }

    public Map<String, Object> executeAndGetMdxRowResults(String query, String field) throws Exception
    {
        MdxResponse response = executeMdx(query);

        Map<String, Object> results = new HashMap<>();
        int i = 0;
        for (Object row : response.getRowsAxis())
        {
            Object realRow = ((List)row).get(0);
            Object cellValue = ((List)response.getCells().get(i)).get(0);
            results.put(((Map)realRow).get(field).toString(), cellValue);
            i++;
        }

        return results;
    }

    public String getConfigId()
    {
        return _configId;
    }

    public void setConfigId(String configId)
    {
        _configId = configId;
    }

    public String getSchemaName()
    {
        return _schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        _schemaName = schemaName;
    }
}
