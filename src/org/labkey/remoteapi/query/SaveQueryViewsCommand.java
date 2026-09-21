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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves custom grid views via query-saveQueryViews.api.
 */
public class SaveQueryViewsCommand extends PostCommand<CommandResponse>
{
    private final String _schemaName;
    private final String _queryName;
    private final List<Map<String, Object>> _views = new ArrayList<>();

    public SaveQueryViewsCommand(String schemaName, String queryName)
    {
        super("query", "saveQueryViews");
        _schemaName = schemaName;
        _queryName = queryName;
    }

    /**
     * @param viewName the view name; empty for the default view
     * @param columns  field keys to show, in order; at least one is required
     * @param shared   make the view available to all users
     * @param inherit  make the view available in child folders
     */
    public SaveQueryViewsCommand addView(String viewName, List<String> columns, boolean shared, boolean inherit)
    {
        _views.add(view(viewName, columns, shared, inherit));
        return this;
    }

    /**
     * Adds a shared, inheritable view saved into a folder other than the one the command is executed against.
     *
     * @param containerPath the folder to save the view into
     */
    public SaveQueryViewsCommand addViewInContainer(String viewName, List<String> columns, String containerPath)
    {
        Map<String, Object> view = view(viewName, columns, true, true);
        view.put("containerPath", containerPath);
        _views.add(view);

        return this;
    }

    /** Adds a view held in the caller's HTTP session rather than the database. */
    public SaveQueryViewsCommand addSessionView(String viewName, List<String> columns)
    {
        Map<String, Object> view = view(viewName, columns, false, false);
        view.put("session", true);
        _views.add(view);

        return this;
    }

    private static Map<String, Object> view(String viewName, List<String> columns, boolean shared, boolean inherit)
    {
        List<Map<String, Object>> columnList = columns.stream()
                .map(fieldKey -> Map.<String, Object>of("fieldKey", fieldKey))
                .toList();

        Map<String, Object> view = new HashMap<>();
        view.put("name", viewName);
        view.put("columns", columnList);
        view.put("shared", shared);
        view.put("inherit", inherit);
        view.put("replace", true);

        return view;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject json = new JSONObject();
        json.put("schemaName", _schemaName);
        json.put("queryName", _queryName);
        json.put("views", _views);
        return json;
    }
}
