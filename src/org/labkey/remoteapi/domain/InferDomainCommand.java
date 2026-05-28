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
package org.labkey.remoteapi.domain;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.json.JSONObject;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;

public class InferDomainCommand extends PostCommand<InferDomainResponse>
{
    private final File _file;
    private final String _domainKindName;

    private int _numLinesToInclude = 4;

    public InferDomainCommand(File file, String domainKindName)
    {
        super("property", "inferDomain");

        _file = file;
        _domainKindName = domainKindName;
    }

    public void setNumLinesToInclude(int numLinesToInclude)
    {
        _numLinesToInclude = numLinesToInclude;
    }

    @Override
    protected InferDomainResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new InferDomainResponse(text, status, contentType, json);
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addTextBody("numLinesToInclude", String.valueOf(4));
        builder.addTextBody("domainKindName", _domainKindName);

        if (_file != null)
            builder.addBinaryBody("file", _file, ContentType.APPLICATION_OCTET_STREAM, _file.getName());

        HttpPost post = new HttpPost(uri);
        post.setEntity(builder.build());
        return post;
    }
}
