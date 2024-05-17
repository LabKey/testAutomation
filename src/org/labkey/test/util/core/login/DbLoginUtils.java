package org.labkey.test.util.core.login;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.util.LogMethod;

import java.io.IOException;

public class DbLoginUtils
{
    private static DbLoginProperties initialLoginProperties = null;

    private DbLoginUtils() {}

    public static void initDbLoginConfig(DatabaseAuthConfigureDialog dialog)
    {
        if (initialLoginProperties != null)
        {
            initialLoginProperties = new DbLoginProperties(dialog.getPasswordStrength(), dialog.getPasswordExpiration());
        }
    }

    @LogMethod
    public static DbLoginProperties getDbLoginConfig(Connection connection)
    {
        DbLoginProperties dbLoginProperties;

        SimplePostCommand postCommand = new SimplePostCommand("login", "getDbLoginProperties");
        try
        {
            CommandResponse response = postCommand.execute(connection, "/");
            dbLoginProperties = new DbLoginProperties(
                    PasswordStrength.valueOf(response.getProperty("currentSettings.strength")),
                    PasswordExpiration.valueOf(response.getProperty("currentSettings.expiration")));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get login configuration", e);
        }

        return dbLoginProperties;
    }

    @LogMethod
    public static void setDbLoginConfig(Connection connection, PasswordStrength strength, PasswordExpiration expiration)
    {
        if (initialLoginProperties == null)
        {
            getDbLoginConfig(connection);
        }
        JSONObject params = new JSONObject();
        params.put("strength", strength);
        params.put("expiration", expiration);
        SimplePostCommand postCommand = new SimplePostCommand("login", "SaveDbLoginProperties");
        postCommand.setJsonObject(params);
        try
        {
            postCommand.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public static void resetDbLoginConfig(Connection connection)
    {
        if (initialLoginProperties != null)
        {
            setDbLoginConfig(connection, initialLoginProperties.strength, initialLoginProperties.expiration);
            initialLoginProperties = null;
        }
    }

    public enum PasswordStrength implements OptionSelect.SelectOption
    {
        Good("Your password must be at least eight non-whitespace characters, include a mix of lowercase letters, uppercase letters, digits, and symbols, and cannot include portions of your personal information."),
        Strong("Secure passwords are long and use multiple character types. The password strength gauge will turn green when your new password meets the complexity requirements.");

        private final String _guidance;

        PasswordStrength(String guidance)
        {
            _guidance = guidance;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        // General password guidance displayed on the set/change password page for this rule
        public String getGuidance()
        {
            return _guidance;
        }
    }

    public enum PasswordExpiration implements OptionSelect.SelectOption
    {
        Never, FiveSeconds, ThreeMonths, SixMonths, OneYear;

        @Override
        public String getValue()
        {
            return name();
        }
    }

    public record DbLoginProperties(@NotNull PasswordStrength strength, @NotNull PasswordExpiration expiration) { }
}
