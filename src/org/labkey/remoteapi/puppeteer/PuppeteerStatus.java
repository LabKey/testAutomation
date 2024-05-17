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

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.Map;

public class PuppeteerStatus
{
    final private boolean _isAvailable;
    final private boolean _pingSuccess;
    final private JSONObject _service;

    public PuppeteerStatus(JSONObject payload)
    {
        _isAvailable = payload.getBoolean("isAvailable");
        _pingSuccess = payload.getBoolean("pingSuccess");
        _service = payload.getJSONObject("service");
    }

    public PuppeteerStatus(CommandResponse response)
    {
        this(new JSONObject((Map<String, Object>)response.getParsedData().get("data")));
    }

    public boolean isAvailable()
    {
        return _isAvailable;
    }

    public boolean isPingSuccess()
    {
        return _pingSuccess;
    }

    public JSONObject getService()
    {
        return _service;
    }
}
