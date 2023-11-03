package org.labkey.test.util.login;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public static Map<String, Integer> getConfigurationIds(Connection connection)
    {
        return getAllConfigurations(connection).stream()
                .collect(Collectors.toMap(Configuration::getDescription, Configuration::getConfiguration));
    }

    private static List<Configuration> getAllConfigurations(Connection connection)
    {
        SimpleGetCommand initialMount = new SimpleGetCommand("login", "initialMount");
        try
        {
            CommandResponse response = initialMount.execute(connection, "/");
            List<Map<String, Object>> parsedConfigurations = new ArrayList<>();
            parsedConfigurations.addAll(response.getProperty("ssoConfigurations"));
            parsedConfigurations.addAll(response.getProperty("secondaryConfigurations"));
            parsedConfigurations.addAll(response.getProperty("formConfigurations"));

            List<Configuration> configurations = new ArrayList<>();
            for (Map<String, Object> configMap : parsedConfigurations)
            {
                int configuration = (int) configMap.get("configuration");
                String description = (String) configMap.get("description");
                String provider = (String) configMap.get("provider");
                Configuration config = new Configuration(description, configuration, provider);
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
        deleteConfigurations(configuration -> providerName.equals(configuration._provider), connection);
    }

    @LogMethod
    public static void deleteConfiguration(@LoggedParam String configurationDescription, Connection connection)
    {
        deleteConfigurations(configuration -> configurationDescription.equals(configuration._description), connection);
    }

    @LogMethod
    public static void deleteConfigurations(Predicate<Configuration> configurationFilter, Connection connection)
    {
        List<Configuration> configurations = getAllConfigurations(connection);
        configurations.stream().filter(configurationFilter)
                .forEach(configuration -> deleteConfiguration(configuration, connection));
    }

    private static void deleteConfiguration(Configuration configuration, Connection connection)
    {
        SimplePostCommand delete = new SimplePostCommand("login", "deleteConfiguration");
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

    public static class Configuration
    {
        private final String _description;
        private final int _configuration;
        private final String _provider;

        public Configuration(String description, int configuration, String provider)
        {
            _description = description;
            _configuration = configuration;
            _provider = provider;
        }

        public String getDescription()
        {
            return _description;
        }

        public int getConfiguration()
        {
            return _configuration;
        }

        public String getProvider()
        {
            return _provider;
        }
    }
}
