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

public class GetMessageThreadCommand extends AbstractMessageThreadCommand
{
    private final AnnouncementModel _announcementModel;

    public GetMessageThreadCommand(AnnouncementModel params)
    {
        super("getThread");
       _announcementModel = params;
    }

    public GetMessageThreadCommand(String entityId)
    {
        super("getThread");
        _announcementModel = new AnnouncementModel();
        _announcementModel.setEntityId(entityId);
    }

    public GetMessageThreadCommand(Long rowId)
    {
        super("getThread");
        _announcementModel = new AnnouncementModel();
        _announcementModel.setRowId(rowId);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();

        if (_announcementModel.getEntityId() != null)
            result.put("entityId", _announcementModel.getEntityId());
        else if (_announcementModel.getRowId() != null)
            result.put("rowId", _announcementModel.getRowId());

        return result;
    }
}
