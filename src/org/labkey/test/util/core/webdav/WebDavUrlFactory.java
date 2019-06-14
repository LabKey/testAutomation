/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.test.util.core.webdav;


import org.apache.commons.lang3.StringUtils;
import org.labkey.test.util.EscapeUtil;

import static org.labkey.test.util.core.webdav.WebDavUtils.buildBaseWebDavUrl;
import static org.labkey.test.util.core.webdav.WebDavUtils.buildBaseWebfilesUrl;

public class WebDavUrlFactory
{
    private final String baseUrl;

    protected WebDavUrlFactory(String baseUrl)
    {
        this.baseUrl = StringUtils.stripEnd(baseUrl, "/") + "/";
    }

    public String getPath(String relativePath)
    {
        return baseUrl + StringUtils.stripStart(relativePath, "/");
    }

    public String getPath(String relativeParent, String fileName)
    {
        String encodedParent = EscapeUtil.encode(relativeParent).replace("%2F", "/");
        return getPath(StringUtils.stripEnd(encodedParent, "/") + "/" + fileName);
    }

    public static WebDavUrlFactory pipelineUrlFactory(String containerPath)
    {
        return new WebDavUrlFactory(buildBaseWebDavUrl(containerPath, "@pipeline"));
    }

    public static WebDavUrlFactory webDavUrlFactory(String containerPath)
    {
        return new WebDavUrlFactory(buildBaseWebDavUrl(containerPath, "@files"));
    }

    public static WebDavUrlFactory webFilesUrlFactory(String containerPath)
    {
        return new WebDavUrlFactory(buildBaseWebfilesUrl(containerPath));
    }
}

