/*
 * Copyright (c) 2026 LabKey Corporation
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
package org.labkey.test.util;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.WebTestHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reads and updates site settings through the admin-getSiteSettings and admin-updateSiteSettings APIs.
 * Settings changed through {@link #setSettings} are put back by {@link #restoreSettings}, which navigates nowhere, so
 * it's safe to call from {@code doCleanup}.
 */
public class SiteSettingsHelper
{
    private final Supplier<Connection> _connectionSupplier;
    private final Map<String, Object> _originalValues = new LinkedHashMap<>();

    public SiteSettingsHelper()
    {
        this(WebTestHelper::getRemoteApiConnection);
    }

    public SiteSettingsHelper(Supplier<Connection> connectionSupplier)
    {
        _connectionSupplier = connectionSupplier;
    }

    public Map<String, Object> getSettings()
    {
        try
        {
            return getSettings(new SimpleGetCommand("admin", "getSiteSettings").execute(_connectionSupplier.get(), "/"));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get site settings", e);
        }
    }

    public Object getSetting(String name)
    {
        return getSettings().get(name);
    }

    /** Updates settings without remembering their original values. Returns every setting's value after the update. */
    public Map<String, Object> updateSettings(Map<String, ?> settings) throws IOException, CommandException
    {
        SimplePostCommand command = new SimplePostCommand("admin", "updateSiteSettings");
        // JSONObject(Map) drops null values, which would leave a null-valued setting unrestored
        JSONObject json = new JSONObject();
        settings.forEach((name, value) -> json.put(name, value == null ? JSONObject.NULL : value));
        command.setJsonObject(new JSONObject().put("settings", json));
        return getSettings(command.execute(_connectionSupplier.get(), "/"));
    }

    /** Updates settings, remembering the value each had before its first change through this helper */
    @LogMethod
    public void setSettings(@LoggedParam Map<String, ?> settings)
    {
        Map<String, Object> current = getSettings();
        settings.keySet().forEach(name -> _originalValues.putIfAbsent(name, current.get(name)));
        try
        {
            updateSettings(settings);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to update site settings " + settings.keySet(), e);
        }
    }

    public void setSetting(String name, Object value)
    {
        // Map.of() rejects null values
        Map<String, Object> settings = new HashMap<>();
        settings.put(name, value);
        setSettings(settings);
    }

    /** Puts back every setting changed through {@link #setSettings} since the last restore */
    @LogMethod
    public void restoreSettings()
    {
        if (_originalValues.isEmpty())
            return;

        try
        {
            updateSettings(_originalValues);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to restore site settings " + _originalValues.keySet(), e);
        }
        _originalValues.clear();
    }

    private static Map<String, Object> getSettings(CommandResponse response)
    {
        return response.getProperty("settings");
    }
}
