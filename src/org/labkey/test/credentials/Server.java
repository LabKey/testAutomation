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
