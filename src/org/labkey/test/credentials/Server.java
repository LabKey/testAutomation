package org.labkey.test.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Bean for an individual server's credentials
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class Server
{
    @JsonProperty(required = true) private String key;
    private String host;
    private List<Login> logins;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public List<Login> getLogins()
    {
        return logins;
    }

    public void setLogins(List<Login> logins)
    {
        this.logins = logins;
    }

    /**
     * Multiple logins are not currently necessary, just use this for now.
     */
    public Login getLogin()
    {
        return getLogins().get(0);
    }
}
