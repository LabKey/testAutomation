/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.remoteapi.announcements;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class DeleteMessageThreadCommand extends PostCommand<DeleteMessageThreadResponse>
{
    private String _entityId;
    private Long _rowId;

    public DeleteMessageThreadCommand(String entityId)
    {
        super("announcements", "deleteThread");
        _entityId = entityId;
    }

    public DeleteMessageThreadCommand(Long rowId)
    {
        super("announcements", "deleteThread");
        _rowId = rowId;
    }

    @Override
    public JSONObject getJsonObject()
    {
        setRequiredVersion(0);
        JSONObject result = new JSONObject();
        if (getRowId() != null) result.put("rowId", getRowId());
        else if (getEntityId() != null) result.put("entityId", getEntityId());
        return result;
    }

    @Override
    protected DeleteMessageThreadResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new DeleteMessageThreadResponse(text, status, contentType, json);
    }

    public void setEntityId(String entityId)
    {
        _entityId = entityId;
    }

    public String getEntityId()
    {
        return _entityId;
    }

    public void setRowId(Long rowId)
    {
        _rowId = rowId;
    }

    public Long getRowId()
    {
        return _rowId;
    }
}
