package org.labkey.remoteapi.query;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.AuditLogHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ImportExperimentDataCommand extends ImportDataCommand
{
    private AuditLogHelper.AuditBehaviorType _auditBehavior;
    private Boolean _crossTypeImport;
    private String _containerPath;

    public ImportExperimentDataCommand(String schemaName, String queryName, String containerPath)
    {
        super(schemaName, queryName);
        _containerPath = containerPath;
    }

    public AuditLogHelper.AuditBehaviorType getAuditBehavior()
    {
        return _auditBehavior;
    }

    public void setAuditBehavior(AuditLogHelper.AuditBehaviorType auditBehavior)
    {
        _auditBehavior = auditBehavior;
    }

    public Boolean getCrossTypeImport()
    {
        return _crossTypeImport;
    }

    public void setCrossTypeImport(Boolean crossTypeImport)
    {
        _crossTypeImport = crossTypeImport;
    }

    @Override
    protected HttpPost createRequest(URI uri) {
        HttpPost post = super.createRequest(uri);
        String action = "samples".equalsIgnoreCase(getSchemaName()) ? "importSamples" : "importData";
        Map<String, String> params = new HashMap<>();
        if (_auditBehavior != null)
            params.put("auditBehavior", _auditBehavior.name());
        if (_crossTypeImport)
            params.put("crossTypeImport", "true");
        String url = WebTestHelper.buildURL("experiment", _containerPath, action, params);
        try
        {
            post.setUri(new URI(url));
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        return post;
    }

}
