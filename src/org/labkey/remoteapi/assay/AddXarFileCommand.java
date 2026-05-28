/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.remoteapi.assay;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.labkey.remoteapi.PostCommand;

import java.io.File;
import java.net.URI;

public class AddXarFileCommand extends PostCommand
{
    private final File _file;

    public AddXarFileCommand(File xarFile)
    {
        super("experiment", "assayXarFile");
        this._file = xarFile;
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.addBinaryBody("file", _file, ContentType.TEXT_XML, _file.getName());

        HttpPost post = new HttpPost(uri);
        post.setEntity(builder.build());
        return post;
    }
}
