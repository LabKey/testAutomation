package org.labkey.remoteapi.security;

import org.labkey.remoteapi.PostCommand;

public class ClearAssignedRolesCommand extends BaseUpdateAssignmentCommand
{
    public ClearAssignedRolesCommand()
    {
        super("clearAssignedRoles");
        super.setRoleClassName(null);
    }

    public ClearAssignedRolesCommand(ClearAssignedRolesCommand source)
    {
        super(source);
    }

    @Override
    public void setRoleClassName(String roleClassName){ }

    @Override
    public PostCommand copy()
    {
        return new ClearAssignedRolesCommand(this);
    }
}
