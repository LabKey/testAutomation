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
package org.labkey.remoteapi.admin;

import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.util.HashMap;
import java.util.Map;

public class ClearCachesCommand extends PostCommand<CommandResponse>
{
    private final boolean _clearCaches;
    private final boolean _gc;

    public ClearCachesCommand(boolean clearCaches, boolean gc)
    {
        super("admin", "clearCaches");
        _clearCaches = clearCaches;
        _gc = gc;
    }

    @Override
    protected Map<String, Object> createParameterMap()
    {
        Map<String, Object> params = new HashMap<>();
        if (_clearCaches)
            params.put("clearCaches", true);
        if (_gc)
            params.put("gc", true);
        return params;
    }
}
