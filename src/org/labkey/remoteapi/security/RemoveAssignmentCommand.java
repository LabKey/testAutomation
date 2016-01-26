package org.labkey.remoteapi.security;

import org.labkey.remoteapi.PostCommand;

public class RemoveAssignmentCommand extends BaseUpdateAssignmentCommand
{
    public RemoveAssignmentCommand()
    {
        super("removeAssignment");
    }

    public RemoveAssignmentCommand(RemoveAssignmentCommand source)
    {
        super(source);
    }

    @Override
    public PostCommand copy()
    {
        return new RemoveAssignmentCommand(this);
    }
}
