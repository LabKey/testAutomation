package org.labkey.remoteapi.security;

import org.labkey.remoteapi.PostCommand;

public class AddAssignmentCommand extends BaseUpdateAssignmentCommand
{
    public AddAssignmentCommand()
    {
        super("addAssignment");
    }

    public AddAssignmentCommand(AddAssignmentCommand source)
    {
        super(source);
    }

    @Override
    public PostCommand copy()
    {
        return new AddAssignmentCommand(this);
    }
}
