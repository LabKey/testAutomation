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
 * Renames a custom grid view via query-renameQueryView.api.
 */
public class RenameQueryViewCommand extends PostCommand<CommandResponse>
{
    private final String _schemaName;
    private final String _queryName;
    private final String _viewName;
    private final String _newName;

    public RenameQueryViewCommand(String schemaName, String queryName, String viewName, String newName)
    {
        super("query", "renameQueryView");
        _schemaName = schemaName;
        _queryName = queryName;
        _viewName = viewName;
        _newName = newName;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("schemaName", _schemaName);
        json.put("queryName", _queryName);
        json.put("viewName", _viewName);
        json.put("newName", _newName);
        return json;
    }
}
