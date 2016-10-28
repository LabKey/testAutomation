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
package org.labkey.remoteapi.experiment;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class LineageResponse extends CommandResponse
{
    LineageNode _seed;
    Map<String, LineageNode> _nodes;

    public LineageResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);

        String seedLsid = getProperty("seed");
        Map<String, Object> nodeMap = getProperty("nodes");

        Map<String, LineageNode> nodes = new HashMap<>();

        for (Map.Entry<String, Object> entry : nodeMap.entrySet())
        {
            String lsid = entry.getKey();
            LineageNode node = new LineageNode(lsid, (Map<String, Object>)entry.getValue());
            nodes.put(lsid, node);

            if (_seed == null && lsid.equals(seedLsid))
                _seed = node;
        }

        for (LineageNode node : nodes.values())
            node.fixup(nodes);

        _nodes = Collections.unmodifiableMap(nodes);
    }

    public LineageNode getSeed()
    {
        return _seed;
    }

    public Map<String, LineageNode> getNodes()
    {
        return _nodes;
    }

    public String dump()
    {
        StringBuilder sb = new StringBuilder();
        dump(sb);
        return sb.toString();
    }

    private void dump(StringBuilder sb)
    {
        if (_seed == null)
            sb.append("No seed found");

        _seed.dump(0, sb, new HashSet<>());
    }

}
