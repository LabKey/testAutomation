/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

/**
 * Moves a session view into the database via query-saveSessionView.api. The session view must have been created on the
 * same {@link org.labkey.remoteapi.Connection}, since it lives in that connection's HTTP session.
 */
public class SaveSessionViewCommand extends PostCommand<CommandResponse>
{
    private final String _schemaName;
    private final String _queryName;
    private final String _viewName;
    private final String _newName;
    private boolean _shared;
    private boolean _inherit;
    private boolean _replace = true;
    private String _containerPath;

    /**
     * @param viewName the session view to save
     * @param newName  the name to save it under
     */
    public SaveSessionViewCommand(String schemaName, String queryName, String viewName, String newName)
    {
        super("query", "saveSessionView");
        _schemaName = schemaName;
        _queryName = queryName;
        _viewName = viewName;
        _newName = newName;
    }

    public SaveSessionViewCommand setShared(boolean shared)
    {
        _shared = shared;
        return this;
    }

    public SaveSessionViewCommand setInherit(boolean inherit)
    {
        _inherit = inherit;
        return this;
    }

    /** @param replace overwrite an existing view of the same name instead of reporting a name collision */
    public SaveSessionViewCommand setReplace(boolean replace)
    {
        _replace = replace;
        return this;
    }

    /** @param containerPath the folder to save the view into; honored only alongside {@link #setInherit(boolean)} */
    public SaveSessionViewCommand setContainerPath(String containerPath)
    {
        _containerPath = containerPath;
        return this;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("schemaName", _schemaName);
        json.put("queryName", _queryName);
        json.put("viewName", _viewName);
        json.put("newName", _newName);
        json.put("shared", _shared);
        json.put("inherit", _inherit);
        json.put("replace", _replace);
        if (_containerPath != null)
            json.put("containerPath", _containerPath);
        return json;
    }
}
