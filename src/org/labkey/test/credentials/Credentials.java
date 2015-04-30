/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty(required = true)
    private List<Server> servers;

    @JsonIgnore
    private Map<String, Server> credentials;

    public List<Server> getServers()
    {
        return servers;
    }

    public void setServers(List<Server> servers)
    {
        credentials = new HashMap<>();

        for (Server server : servers)
        {
            if (credentials.put(server.getKey(), server) != null)
                throw new RuntimeJsonMappingException("Duplicate server key in credentials file: " + server.getKey());
        }

        this.servers = servers;
    }

    public Map<String, Server> getCredentials()
    {
        return credentials;
    }
}
