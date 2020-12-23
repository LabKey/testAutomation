package org.labkey.remoteapi.puppeteer;

import org.json.JSONObject;
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

    public org.json.simple.JSONObject toJSON()
    {
        var settings = new org.json.simple.JSONObject();

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

        var payload = new org.json.simple.JSONObject();
        payload.put("settings", settings);

        return payload;
    }
}
