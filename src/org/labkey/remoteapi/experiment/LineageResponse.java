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
