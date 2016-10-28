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


import org.labkey.remoteapi.ResponseObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LineageNode extends ResponseObject
{
    private final String _lsid;
    private final Integer _rowId;
    private final String _name;
    private final String _url;
    private final String _type;
    private final String _cpasType;

    private List<Edge> _parents;
    private List<Edge> _children;


    public LineageNode(String lsid, Map<String, Object> map)
    {
        super(map);
        _lsid = lsid;
        _rowId = ((Long)map.get("rowId")).intValue();
        _name = (String)map.get("name");
        _type = (String)map.get("type");
        _cpasType = (String)map.get("cpasType");
        _url = (String)map.get("url");
    }

    void fixup(Map<String, LineageNode> nodes)
    {
        _children = fixupEdges(nodes, (List<Map<String, Object>>) getAllProperties().get("children"));
        _parents = fixupEdges(nodes, (List<Map<String, Object>>) getAllProperties().get("parents"));
    }

    List<Edge> fixupEdges(Map<String, LineageNode> nodes, List<Map<String, Object>> edges)
    {
        return edges
                .stream()
                .map(m -> {
                    String role = (String)m.get("role");
                    String lsid = (String)m.get("lsid");
                    return new Edge(role, nodes.get(lsid));
                })
                .collect(Collectors.toList());
    }

    public String getLsid()
    {
        return _lsid;
    }

    public Integer getRowId()
    {
        return _rowId;
    }

    public String getName()
    {
        return _name;
    }

    public String getUrl()
    {
        return _url;
    }

    public String getType()
    {
        return _type;
    }

    public String getCpasType()
    {
        return _cpasType;
    }

    public List<Edge> getParents()
    {
        return _parents;
    }

    public List<Edge> getChildren()
    {
        return _children;
    }

    void dump(int indent, StringBuilder sb, Set<String> seen)
    {
        indent(indent, sb).append("> ").append(getName()).append(" (").append(getRowId()).append(")");
        if (seen.contains(getLsid()))
        {
            sb.append(" **\n");
            return;
        }
        sb.append("\n");
        seen.add(getLsid());

        indent(indent+1, sb).append("lsid: ").append(getLsid()).append("\n");
        indent(indent+1, sb).append("type: ").append(getType()).append("\n");
        indent(indent+1, sb).append("cpas: ").append(getCpasType()).append("\n");
        indent(indent+1, sb).append("url:  ").append(getUrl()).append("\n");

        indent(indent+1, sb).append("parents:");
        if (_parents.isEmpty())
            sb.append(" (none)");
        sb.append("\n");
        for (Edge edge : _parents)
        {
            indent(indent+2, sb).append("role: ").append(edge.getRole()).append("\n");
            edge.getNode().dump(indent+3, sb, seen);
        }

        indent(indent+1, sb).append("children:");
        if (_children.isEmpty())
            sb.append(" (none)");
        sb.append("\n");
        for (Edge edge : _children)
        {
            indent(indent+2, sb).append("role: ").append(edge.getRole()).append("\n");
            edge.getNode().dump(indent+3, sb, seen);
        }
    }

    StringBuilder indent(int indent, StringBuilder sb)
    {
        for (int i = 0; i < indent; i++)
            sb.append("  ");
        return sb;
    }


    public static class Edge
    {
        private final String role;
        private final LineageNode node;

        Edge(String role, LineageNode node)
        {
            this.role = role;
            this.node = node;
        }

        public String getRole()
        {
            return role;
        }

        public LineageNode getNode()
        {
            return node;
        }
    }
}
