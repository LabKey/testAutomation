package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The top level bean for server credentials used by various tests
 *
{
  "servers": [
    {
      "key": "labkey",
      "host": "localhost:8080",
      "logins": [
        {
          "username": "test@labkey.test",
          "password": "sasasa"
        }
      ]
    },
    {
      "key": "Postgres",
      "host": "localhost:5432",
      "logins": [
        {
          "username": "postgres",
          "password": "sasa"
        }
      ]
    },
    {
      "key": "CAS",
      "host": "https://testauth.epi.usf.edu/cas",
      "logins": [
        {
          "username": "",
          "password": ""
        }
      ]
    }
  ]
}
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Credentials
{
    private List<Server> servers;

    public List<Server> getServers()
    {
        return servers;
    }

    public void setServers(List<Server> servers)
    {
        this.servers = servers;
    }

    public Map<String, Server> getCredentials()
    {
        Map<String, Server> credentials = new HashMap<>();

        for (Server server : servers)
        {
            if (credentials.put(server.getKey(), server) != null)
                throw new RuntimeJsonMappingException("Duplicate server key in credentials file: " + server.getKey());
        }

        return credentials;
    }
}
