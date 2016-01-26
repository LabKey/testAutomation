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
