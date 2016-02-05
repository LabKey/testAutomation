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

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

public abstract class BaseUpdateAssignmentCommand extends PostCommand<CommandResponse>
{
    private Integer principalId;
    private String email;
    private String roleClassName;

    protected BaseUpdateAssignmentCommand(String action)
    {
        super("security", action);
    }

    protected BaseUpdateAssignmentCommand(BaseUpdateAssignmentCommand source)
    {
        this(source.getActionName());
        principalId = source.principalId;
        email = source.email;
        roleClassName = source.roleClassName;
    }

    public void setPrincipalId(Integer principalId)
    {
        this.principalId = principalId;
    }

    public void setRoleClassName(String roleClassName)
    {
        this.roleClassName = roleClassName;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Override
    public double getRequiredVersion()
    {
        return 16.1;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = new JSONObject();

        result.put("principalId", principalId);
        result.put("email", email);
        if (roleClassName != null && !roleClassName.isEmpty()) result.put("roleClassName", roleClassName);
        return result;
    }
}
