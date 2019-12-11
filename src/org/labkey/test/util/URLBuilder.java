package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.WebTestHelper;
import org.seleniumhq.jetty9.util.URIUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.labkey.test.WebTestHelper.getBaseURL;

public class URLBuilder
{
    private final String _controller;
    private final String _action;
    private final String _containerPath;

    private Map<String, Object> _query;
    private String _resourcePath;
    private Map<String, Object> _secondaryQuery;

    public URLBuilder(String controller, String action, String containerPath)
    {
        _controller = controller;
        _action = action;
        _containerPath = containerPath;
    }

    public URLBuilder(String controller, String action)
    {
        this(controller, action, "/");
    }

    public URLBuilder setQuery(Map<String, Object> query)
    {
        _query = query;
        return this;
    }

    public URLBuilder setAppResourcePath(String... pathParts)
    {
        List<String> encodedParts = Arrays.stream(pathParts).map(EscapeUtil::encode).collect(Collectors.toList());
        _resourcePath = "/" + String.join("/", encodedParts);
        return this;
    }

    public URLBuilder setResourcePath(String resourcePath)
    {
        _resourcePath = resourcePath;
        return this;
    }

    public URLBuilder setSecondaryQuery(Map<String, Object> secondaryQuery)
    {
        _secondaryQuery = secondaryQuery;
        return this;
    }

    public String buildURL()
    {
        return getBaseURL() + buildRelativeURL();
    }

    public String buildRelativeURL()
    {
        StringBuilder url = new StringBuilder();

        if (!WebTestHelper.isUseContainerRelativeUrl())
        {
            url.append("/");
            url.append(_controller);
        }

        if (_containerPath != null)
        {
            if (!_containerPath.startsWith("/"))
                url.append("/");
            url.append(URIUtil.encodePath(_containerPath)
                    .replace("[", "%5B")
                    .replace("]", "%5D"));
        }

        url.append("/");
        if (WebTestHelper.isUseContainerRelativeUrl())
        {
            url.append(_controller);
            url.append("-");
        }
        url.append(_action);
        if (!_action.contains("."))
            url.append(".view");

        appendQueryString(url, _query);

        if (!StringUtils.isBlank(_resourcePath))
        {
            if (!_resourcePath.startsWith("#"))
            {
                url.append("#");
            }
            url.append(_resourcePath);
            appendQueryString(url, _secondaryQuery);
        }
        else if (_secondaryQuery != null && !_secondaryQuery.isEmpty())
        {
            throw new IllegalArgumentException("Must supply a resource path");
        }

        return url.toString();
    }

    private void appendQueryString(StringBuilder url, Map<String, Object> params)
    {
        if (params != null)
        {
            boolean firstParam = true;
            for (Map.Entry param : params.entrySet())
            {
                if (null != param.getKey())
                {
                    url.append(firstParam ? "?" : "&");
                    url.append(param.getKey());
                    if (null != param.getValue())
                    {
                        url.append("=");
                        url.append(param.getValue());
                    }
                    firstParam = false;
                }
            }
        }
    }
}
