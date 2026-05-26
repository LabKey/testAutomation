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
package org.labkey.test.util.query;

import org.labkey.remoteapi.query.RowsResponse;

import java.util.Collections;
import java.util.Map;

public class MoveRowsResponse
{
    private final String _containerPath;
    private final Boolean _reselectRowCount;
    private final RowsResponse _rowsResponse;
    private final Boolean _success;
    private final Integer _transactionAuditId;
    private final Map<String, Object> _updateCounts;

    public MoveRowsResponse(RowsResponse response)
    {
        _rowsResponse = response;
        Map<String, Object> data = _rowsResponse.getParsedData();
        if (data == null)
            data = Collections.emptyMap();

        _containerPath = (String) data.get("containerPath");
        _reselectRowCount = (Boolean) data.get("reselectRowCount");
        _success = (Boolean) data.get("success");
        _transactionAuditId = (Integer) data.get("transactionAuditId");
        _updateCounts = (Map<String, Object>) data.get("updateCounts");
    }

    public String getCommand()
    {
        return _rowsResponse.getCommand();
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public String getQueryName()
    {
        return _rowsResponse.getQueryName();
    }

    public Boolean getReselectRowCount()
    {
        return _reselectRowCount;
    }

    public Number getRowsAffected()
    {
        return _rowsResponse.getRowsAffected();
    }

    public RowsResponse getRowsResponse()
    {
        return _rowsResponse;
    }

    public String getSchemaName()
    {
        return _rowsResponse.getSchemaName();
    }

    public Boolean getSuccess()
    {
        return _success;
    }

    public Integer getTransactionAuditId()
    {
        return _transactionAuditId;
    }

    public Map<String, Object> getUpdateCounts()
    {
        return _updateCounts;
    }
}
