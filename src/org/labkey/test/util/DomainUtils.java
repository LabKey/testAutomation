package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.DropDomainCommand;
import org.labkey.remoteapi.domain.GetDomainDetailsCommand;
import org.labkey.test.WebTestHelper;

import java.io.IOException;

public final class DomainUtils
{
    private DomainUtils()
    {
        /* Don't instantiate utility class */
    }

    /**
     * Delete a domain.
     *
     * @param containerPath Container where the domain exists.
     * @param schema The schema of the domain. For example for sample types it would be 'exp.data'.
     * @param queryName The name of the query to delete. For example for a sample type it would be its name.
     * @return The command response after executing the delete. Calling function would need to validate the response.
     * @throws CommandException Thrown if there is some kind of exception deleting the domain.
     */
    public static CommandResponse deleteDomain(final String containerPath, final String schema, final String queryName)
            throws CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        DropDomainCommand delCmd = new DropDomainCommand(schema, queryName);

        try
        {
            return delCmd.execute(connection, containerPath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to delete domain.", e);
        }

    }

    /**
     * Check to see if a domain exists.
     *
     * @param containerPath The container to look for the domain/query.
     * @param schema The schema of the domain. For example for sample types it would be 'exp.data'.
     * @param queryName The name of the query to look for. For example for a sample type it would be its name.
     * @return True if it exists in the given container false otherwise.
     */
    public static boolean doesDomainExist(final String containerPath, final String schema, final String queryName)
    {
        Connection connection = WebTestHelper.getRemoteApiConnection();
        GetDomainDetailsCommand cmd = new GetDomainDetailsCommand(schema, queryName);
        try
        {
            cmd.execute(connection, containerPath);
            return true;
        }
        catch (CommandException ce)
        {
            if(ce.getStatusCode() == 404)
            {
                return false;
            }
            throw new RuntimeException("Exception while looking for domain.", ce);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("IO exception while looking for the domain.", ioe);
        }

    }

    /**
     * Removes the specified domain if it exists
     */
    public static void ensureDeleted(String containerPath, String schema, String table)
    {
        try
        {
            if (doesDomainExist(containerPath, schema, table))
            {
                deleteDomain(containerPath, schema, table);
            }

        }
        catch (CommandException ex)
        {
            throw new RuntimeException(String
                    .format("Failed to delete '%s'.", table), ex);
        }
    }

}
