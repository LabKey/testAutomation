/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.remoteapi.plate;

import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

public class CreatePlateSetCommand extends PostCommand<PlateSetResponse>
{
    private final CreatePlateSetParams _plateSetParams;
    public CreatePlateSetCommand(CreatePlateSetParams params)
    {
        super("plate", "createPlateSet");
        setRequiredVersion(0);
        _plateSetParams = params;
    }

    @Override
    protected PlateSetResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new PlateSetResponse(text, status, contentType, json);
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _plateSetParams.toJSON();
    }
}
