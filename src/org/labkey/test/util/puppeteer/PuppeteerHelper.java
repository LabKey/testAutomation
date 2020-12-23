package org.labkey.test.util.puppeteer;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.WebTestHelper;
import org.labkey.remoteapi.puppeteer.PuppeteerSettings;
import org.labkey.remoteapi.puppeteer.PuppeteerStatus;

import java.io.IOException;

public class PuppeteerHelper
{
    public static void enableRemoteService(Connection connection) throws IOException, CommandException
    {
        var puppeteerSettings = new PuppeteerSettings();
        puppeteerSettings.setEnabled(true);
        puppeteerSettings.setMode("remote");
        puppeteerSettings.setRemoteUrl(getRemoteServiceURL());

        updateSettings(connection, puppeteerSettings);
    }

    public static String getRemoteServiceURL()
    {
        return WebTestHelper.getTargetServer() + ":3031";
    }

    public static PuppeteerStatus getStatus(Connection connection) throws IOException, CommandException
    {
        var statusCmd = new PostCommand<>("puppeteer", "status.api");
        var response = statusCmd.execute(connection, "/");
        return new PuppeteerStatus(response);
    }

    public static PuppeteerSettings updateSettings(Connection connection, PuppeteerSettings settings) throws IOException, CommandException
    {
        var updateCmd = new PostCommand<>("puppeteer", "updateSettings.api");
        updateCmd.setRequiredVersion(0);
        updateCmd.setJsonObject(settings.toJSON());
        var response = updateCmd.execute(connection, "/");
        return new PuppeteerSettings(response);
    }
}
