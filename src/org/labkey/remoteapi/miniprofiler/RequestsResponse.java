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
package org.labkey.remoteapi.miniprofiler;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.stream.StreamSupport;

public class RequestsResponse extends CommandResponse
{
    private final List<RequestInfo> _requestInfos;
    private final String _sessionId;

    public RequestsResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);
        _requestInfos = StreamSupport.stream(json.getJSONArray("requests").spliterator(), false)
                .map(o -> new RequestInfo((JSONObject)o)).toList();
        _sessionId = json.optString("sessionId", null);
    }

    public List<RequestInfo> getRequestInfos()
    {
        return _requestInfos;
    }

    public String getSessionId()
    {
        return _sessionId;
    }
}
