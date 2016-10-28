/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.labkey.remoteapi.PostCommand;
import org.labkey.test.util.Maps;

import java.util.HashMap;

public class TruncateTableCommand extends PostCommand
{
    public TruncateTableCommand(String schemaName, String queryName)
    {
        super("query", "truncateTable");
        setParameters(new HashMap<>(Maps.of(
                "schemaName", schemaName,
                "queryName", queryName
        )));
    }
}
