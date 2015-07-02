package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bean for a single API key
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiKey
{
    @JsonProperty(required = true) private String name;
    @JsonProperty(required = true) private String token;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }
}
