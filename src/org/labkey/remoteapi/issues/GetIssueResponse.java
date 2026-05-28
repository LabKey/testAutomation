/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.remoteapi.issues;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandResponse;

public class GetIssueResponse extends CommandResponse
{
    private final IssueResponseModel _issueModel;

    public GetIssueResponse(String text, int statusCode, String contentType, JSONObject json)
    {
        super(text, statusCode, contentType, json);

        // parse json into issueModel here
        _issueModel = new IssueResponseModel(json);
    }

    // expose response data in a getter here
    public IssueResponseModel getIssueModel()
    {
        return _issueModel;
    }
}
