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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;
import java.util.List;

public class IssuesCommand extends PostCommand<IssueResponse>
{
    private List<IssueModel> _issues;

    public IssuesCommand()
    {
        super("issues", "issues");
    }

    public IssuesCommand(List<IssueModel> issues)
    {
        this();
        _issues = issues;
    }

    @Override
    protected IssueResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new IssueResponse(text, status, contentType, json);
    }

    public void setIssues(List<IssueModel> issues)
    {
        _issues = issues;
    }

    @Override
    public JSONObject getJsonObject()
    {
       throw new IllegalStateException("This command should not use this method for constructing the request");
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        HttpPost request = new HttpPost(uri);

        JSONArray issuesArray = new JSONArray();
        for (IssueModel issue: _issues)
        {
            issuesArray.put(issue.toJSON());
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.LEGACY);
        builder.addTextBody("issues", issuesArray.toString(), ContentType.APPLICATION_JSON);

        for(IssueModel issue: _issues)
        {
            for (File attachment : issue.getAttachments())
            {
                builder.addBinaryBody(attachment.getName(), attachment, ContentType.APPLICATION_OCTET_STREAM, attachment.getName());
            }
            request.setEntity(builder.build());
        }
        return request;
    }
}


