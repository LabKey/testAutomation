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
package org.labkey.remoteapi.security;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkUpdateGroupCommand extends PostCommand<BulkUpdateGroupResponse>
{
    @Nullable private Integer _groupId;     // used first as identifier for group;
    @Nullable private String _groupName;    // required for creating a group
    @Nullable private List<Map<String, Object>> _members;      // can be used to provide more data than just email address; can be empty; can include groups, but group creation is not recursive
    private Boolean _createGroup = false;   // if true, the group should be created if it doesn't exist; otherwise the operation will fail if the group does not exist
    private Method _method = Method.add;    // indicates the action to be performed with the given users in this group

    public enum Method {
        add,        // add the given members; do not fail if already exist
        replace,    // replace the current members with the new list (same as delete all then add)
        delete,     // delete the given members; does not fail if member does not exist in group; does not delete group if it becomes empty
    }

    private BulkUpdateGroupCommand()
    {
        super("security", "bulkUpdateGroup");
    }

    public BulkUpdateGroupCommand(BulkUpdateGroupCommand source)
    {
        this();
        _groupId = source._groupId;
        _groupName = source._groupName;
        _members = source._members;
        _createGroup = source._createGroup;
        _method = source._method;
    }

    public BulkUpdateGroupCommand(@NotNull String groupName)
    {
        this();
        _groupName = groupName;
    }

    public BulkUpdateGroupCommand(@NotNull Integer groupId)
    {
        this();
        _groupId = groupId;
    }

    public void setGroupId(Integer groupId)
    {
        _groupId = groupId;
    }

    public void setGroupName(String groupName)
    {
        _groupName = groupName;
    }

    public void setMembers(List<Map<String, Object>> members)
    {
        _members = members;
    }

    public void addMemberUser(Integer userId)
    {
        Map<String, Object> props = new HashMap<>();
        props.put("userId", userId);
        addMember(props);
    }

    public void addMemberUser(String email)
    {
        addMemberUser(email, null);
    }

    public void addMemberUser(String email, Map<String, Object> props)
    {
        props = null == props ? new HashMap<>() : new HashMap<>(props);
        props.put("email", email);
        addMember(props);
    }

    public void addMemberGroup(long groupId)
    {
        Map<String, Object> props = new HashMap<>();
        props.put("groupId", groupId);
        addMember(props);
    }

    private void addMember(Map<String, Object> props)
    {
        if (_members == null)
            _members = new ArrayList<>();
        _members.add(props);
    }

    public void setCreateGroup(Boolean createGroup)
    {
        _createGroup = createGroup;
    }

    public void setMethod(Method method)
    {
        _method = method;
    }

    @Override
    protected BulkUpdateGroupResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new BulkUpdateGroupResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = super.getJsonObject();
        if (result == null)
        {
            result = new JSONObject();
        }
        if (_groupId != null) result.put("groupId", _groupId);
        if (_groupName != null) result.put("groupName", _groupName);
        result.put("createGroup", _createGroup);
        result.put("method", _method.toString());
        if (_members != null) result.put("members", _members);
        return result;
    }

    @Override
    public double getRequiredVersion()
    {
        return 16.1;
    }

    @Override
    public PostCommand copy()
    {
        return new BulkUpdateGroupCommand(this);
    }
}
