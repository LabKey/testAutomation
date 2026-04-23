package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.test.WebTestHelper;

import java.io.IOException;

public class AttachmentHelper
{
    /**
     * Tells LabKey to log the first 20 orphaned attachments it detects at the ERROR level. Returns the total number of
     * orphaned attachments detected. Must be a root admin (site or app) to call this.
     */
    public static int logOrphanedAttachments()
    {
        SimpleGetCommand command = new SimpleGetCommand("admin", "logOrphanedAttachments");
        try
        {
            CommandResponse response = command.execute(WebTestHelper.getRemoteApiConnection(), "/");
            return response.getProperty("count");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }
}
