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
package org.labkey.remoteapi.admin;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public class SaveAuditSettingsCommand extends PostCommand<CommandResponse>
{
    private static final String ACTION_NAME = "saveAuditSettings";
    private static final String CONTROLLER_NAME = "audit";
    private boolean _requireUserComments = false;

    public SaveAuditSettingsCommand(boolean requireUserComments)
    {
        super(CONTROLLER_NAME, ACTION_NAME);
        _requireUserComments = requireUserComments;
    }

    public boolean isRequireUserComments()
    {
        return _requireUserComments;
    }

    public void setRequireUserComments(boolean requireUserComments)
    {
        this._requireUserComments = requireUserComments;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("requireUserComments", isRequireUserComments());

        return json;
    }
}
