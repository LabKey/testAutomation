package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bean for a single username/password combination
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Login
{
    @JsonProperty(required = true) public String username;
    @JsonProperty(required = true) public String password;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
