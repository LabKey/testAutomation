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
        return getPath(StringUtils.stripEnd(EscapeUtil.encode(relativeParent), "/") + "/" + fileName);
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

