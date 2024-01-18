package org.labkey.test.util.wiki;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.test.params.wiki.SaveWikiParams;

import java.io.IOException;

public class ApiWikiHelper
{
    public void createWiki(String containerPath, SaveWikiParams wikiParams) throws CommandException
    {
        SimplePostCommand command = new SimplePostCommand("wiki", "saveWiki");
        command.setJsonObject(wikiParams.toJSON());
        Connection connection = WebTestHelper.getRemoteApiConnection(true);

        try
        {
            CommandResponse response = command.execute(connection, containerPath);
            Assert.assertTrue(response.getText(), response.getProperty("success"));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create wiki", e);
        }
    }
}
