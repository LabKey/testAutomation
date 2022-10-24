/*
 * Copyright (c) 2020 LabKey Corporation
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
package org.labkey.remoteapi.puppeteer;

import org.json.old.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class PuppeteerSettings
{
    private Boolean _enabled;
    private String _mode;
    private String _dockerImage;
    private Integer _dockerPort;
    private String _remoteUrl;

    public PuppeteerSettings()
    {
    }

    public PuppeteerSettings(JSONObject json)
    {
        this();
        _enabled = json.getBoolean("enabled");
        _mode = json.getString("mode");
        _dockerImage = json.getString("docker.image");

        try
        {
            _dockerPort = Integer.parseInt(json.getString("docker.port"));
        }
        catch (NumberFormatException ignored)
        {
        }

        _remoteUrl = json.getString("remote.url");
    }

    public PuppeteerSettings(CommandResponse response)
    {
        this(new JSONObject(response.getParsedData().get("data")));
    }

    public Boolean getEnabled()
    {
        return _enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        _enabled = enabled;
    }

    public String getMode()
    {
        return _mode;
    }

    public void setMode(String mode)
    {
        _mode = mode;
    }

    public String getDockerImage()
    {
        return _dockerImage;
    }

    public void setDockerImage(String dockerImage)
    {
        _dockerImage = dockerImage;
    }

    public Integer getDockerPort()
    {
        return _dockerPort;
    }

    public void setDockerPort(Integer dockerPort)
    {
        _dockerPort = dockerPort;
    }

    public String getRemoteUrl()
    {
        return _remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl)
    {
        _remoteUrl = remoteUrl;
    }

    public org.json.JSONObject toJSON()
    {
        var settings = new org.json.JSONObject();

        if (getEnabled() != null)
            settings.put("enabled", getEnabled());
        if (getMode() != null)
            settings.put("mode", getMode());
        if (getDockerImage() != null)
            settings.put("docker.image", getDockerImage());
        if (getDockerPort() != null)
            settings.put("docker.port", getDockerPort());
        if (getRemoteUrl() != null)
            settings.put("remote.url", getRemoteUrl());

        var payload = new org.json.JSONObject();
        payload.put("settings", settings);

        return payload;
    }
}
