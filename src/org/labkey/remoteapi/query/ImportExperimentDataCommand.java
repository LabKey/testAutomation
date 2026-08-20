/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private final String _containerPath;

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
