package org.labkey.test.util.login;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthenticationAPIUtils
{
    private AuthenticationAPIUtils() {} // private constructor to prevent instantiation

    /*
    {
      "helpLink": "https://www.labkey.org/Documentation/20.0/wiki-page.view?name=authenticationModule",
      "globalSettings": {
        "SelfRegistration": false,
        "SelfServiceEmailChanges": false,
        "AutoCreateAccounts": true
      },
      "primaryProviders": {
        "TestSSO": {
          "helpLink": "https://www.labkey.org/Documentation/20.0/wiki-page.view?name=authenticationModule",
          "saveLink": "/labkey/testsso-testSsoSaveConfiguration.view?",
          "settingsFields": [],
          "description": "A trivial, insecure SSO authentication provider (for test purposes only)",
          "sso": true
        }
      },
      "ssoConfigurations": [
        {
          "provider": "TestSSO",
          "configuration": 5,
          "headerLogoUrl": null,
          "loginLogoUrl": null,
          "description": "TestSSO Configuration",
          "details": null,
          "enabled": true
        }
      ],
      "canEdit": true,
      "secondaryConfigurations": [
        {
          "provider": "TestSecondary",
          "configuration": 8,
          "description": "TestSecondary Configuration",
          "details": null,
          "enabled": true
        }
      ],
      "formConfigurations": [
        {
          "provider": "Database",
          "configuration": 0,
          "description": "Standard database authentication",
          "details": null,
          "enabled": true
        }
      ],
      "secondaryProviders": {
        "TestSecondary": {
          "helpLink": "https://www.labkey.org/Documentation/20.0/wiki-page.view?name=authenticationModule",
          "saveLink": "/labkey/testsecondary-testSecondarySaveConfiguration.view?",
          "settingsFields": [],
          "description": "Adds a trivial, insecure secondary authentication requirement (for test purposes only)"
        }
      }
    }
     */

    private static List<Configuration> getAllConfigurations(Connection connection)
    {
        Command<?> initialMount = new Command<>("login", "initialMount");
        try
        {
            CommandResponse response = initialMount.execute(connection, "/");
            List<Map<String, Object>> parsedConfigurations = new ArrayList<>();
            parsedConfigurations.addAll(response.getProperty("ssoConfigurations"));
            parsedConfigurations.addAll(response.getProperty("secondaryConfigurations"));

            List<Configuration> configurations = new ArrayList<>();
            for (Map<String, Object> configMap : parsedConfigurations)
            {
                Configuration config = new Configuration();
                config._configuration = (long) configMap.get("configuration");
                config._description = (String) configMap.get("description");
                config._provider = (String) configMap.get("provider");
                configurations.add(config);
            }

            return configurations;
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public static void deleteAllConfigurations(Connection connection)
    {
        List<Configuration> configurations = getAllConfigurations(connection);
        configurations.forEach(configuration -> deleteConfiguration(configuration, connection));
    }

    @LogMethod
    public static void deleteConfigurations(@LoggedParam String providerName, Connection connection)
    {
        List<Configuration> configurations = getAllConfigurations(connection);
        configurations.stream().filter(configuration -> providerName.equals(configuration._provider))
                .forEach(configuration -> deleteConfiguration(configuration, connection));
    }

    @LogMethod
    public static void deleteConfiguration(@LoggedParam String configurationDescription, Connection connection)
    {
        List<Configuration> configurations = getAllConfigurations(connection);
        configurations.stream().filter(configuration -> configurationDescription.equals(configuration._description))
                .forEach(configuration -> deleteConfiguration(configuration, connection));
    }

    private static void deleteConfiguration(Configuration configuration, Connection connection)
    {
        PostCommand<?> delete = new PostCommand<>("login", "deleteConfiguration");
        JSONObject json = new JSONObject();
        json.put("configuration", configuration._configuration);
        delete.setJsonObject(json);

        try
        {
            TestLogger.log("Deleting authentication configuration: " + configuration._description);
            delete.execute(connection, "/");
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class Configuration
    {
        private String _description;
        private long _configuration;
        private String _provider;
    }
}
