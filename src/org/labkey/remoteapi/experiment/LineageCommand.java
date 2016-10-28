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

import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.Connection;

import java.util.HashMap;
import java.util.Map;


public class LineageCommand extends Command<LineageResponse>
{
    // One of rowId or LSID required
    private final Integer _rowId;
    private final String _lsid;

    private final Boolean _parents;
    private final Boolean _children;
    private final Integer _depth;
    private final String _expType;
    private final String _cpasType;

    private LineageCommand(@Nullable Integer rowId, String lsid, Boolean parents, Boolean children, Integer depth, String cpasType, String expType)
    {
        super("experiment", "lineage");
        if (rowId == null && lsid == null)
            throw new IllegalArgumentException("One of rowId or lsid required");

        if (lsid != null && rowId != null)
            throw new IllegalArgumentException("Only one of rowId or lsid allowed");

        _rowId = rowId;
        _lsid = lsid;
        _depth = depth;
        _parents = parents;
        _children = children;
        _expType = expType;
        _cpasType = cpasType;
    }

    public static final class Builder
    {
        private final Integer _rowId;
        private final String _lsid;

        private Integer _depth;
        private Boolean _parents;
        private Boolean _children;
        private String _expType;
        private String _cpasType;

        public Builder(String lsid)
        {
            this(lsid, null);
        }

        public Builder(Integer rowId)
        {
            this(null, rowId);
        }

        public Builder(String lsid, Integer rowId)
        {
            if (lsid == null && rowId == null)
                throw new IllegalArgumentException("One of rowId or lsid required");

            if (lsid != null && rowId != null)
                throw new IllegalArgumentException("Only one of rowId or lsid allowed");

            _rowId = rowId;
            _lsid = lsid;
        }

        public Builder setDepth(Integer depth)
        {
            _depth = depth;
            return this;
        }

        public Builder setParents(Boolean parents)
        {
            _parents = parents;
            return this;
        }

        public Builder setChildren(Boolean children)
        {
            _children = children;
            return this;
        }

        public Builder setExpType(String expType)
        {
            _expType = expType;
            return this;
        }

        public Builder setCpasType(String cpasType)
        {
            _cpasType = cpasType;
            return this;
        }

        public LineageCommand build()
        {
            return new LineageCommand(_rowId, _lsid, _parents, _children, _depth, _cpasType, _expType);
        }
    }

    protected LineageResponse createResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        return new LineageResponse(text, statusCode, contentType, json, this);
    }

    public Map<String, Object> getParameters()
    {
        Map<String,Object> params = new HashMap<>();
        if (null != _rowId)
            params.put("rowId", _rowId);
        if (null != _lsid)
            params.put("lsid", _lsid);
        if (null != _parents)
            params.put("parents", _parents);
        if (null != _children)
            params.put("children", _children);
        if (null != _depth)
            params.put("depth", _depth);
        if (null != _expType)
            params.put("expType", _expType);
        if (null != _cpasType)
            params.put("cpasType", _cpasType);

        return params;
    }


    @Override
    public LineageCommand copy()
    {
        return new LineageCommand(_rowId, _lsid, _parents, _children, _depth, _cpasType, _expType);
    }

    public static void main(String[] args) throws Exception
    {
        String url = "http://localhost:8080/labkey";
        String folderPath = "/bl";
        String user = "kevink@labkey.com";
        String password = "xxxxxx";

        String lsid = null;
        Integer rowId = 7523;
        Boolean parents = false;
        Boolean children = null;

        Builder builder = new Builder(lsid, rowId);
        if (parents != null)
            builder.setParents(parents);
        if (children != null)
            builder.setChildren(children);

        LineageCommand cmd = builder.build();
        Connection conn = new Connection(url, user, password);
        LineageResponse resp = cmd.execute(conn, folderPath);
        System.out.println(resp.dump());
    }
}
