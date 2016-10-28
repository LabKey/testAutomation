/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.remoteapi.admin;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

public class GetModulesCommand extends Command<GetModulesResponse>
{
    public GetModulesCommand()
    {
        super("admin", "getModules");
    }

    protected GetModulesResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetModulesResponse(text, status, contentType, json, copy());
    }

    @Override
    public GetModulesCommand copy()
    {
        return new GetModulesCommand();
    }
}
