package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.WebTestHelper;
import org.seleniumhq.jetty9.util.URIUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.labkey.test.WebTestHelper.getBaseURL;

/**
 * Constructs a LabKey URL for use by tests. LabKey docs have more information about the structure of LabKey URLs:
 * https://www.labkey.org/Documentation/wiki-page.view?name=url

 * For this builder's purposes, a URL consists of the following:
 *  - controller
 *  - action
 *  - containerPath [optional]
 *  - query [optional]
 *  - resourcePath [optional]
 *  - secondaryQuery [optional]
 *
 *  Here is an example builder:
 *  <pre>
 *      URLBuilder builder = new URLBuilder("dumbster", "setRecordEmail");
 *      builder.setQuery(Maps.of("record", true));
 *      builder.buildRelativeUrl(); // "/dumbster-setRecordEmail.view?record=true"
 *      builder.buildUrl(); // "http://localhost:8080/labkey/dumbster-setRecordEmail.view?record=true"
 *  </pre>
 *
 *  For an app URL you might use something like:
 *  <pre>
 *      URLBuilder builder = new URLBuilder("samplemanager", "app", "testContainer");
 *      builder.setAppResourcePath("assays", "general", "myassay");
 *      builder.setSecondaryQuery(Map.of("sort", "Name"));
 *      builder.buildRelativeUrl(); // "/testContainer/samplemanager-app.view#/assays/general/myassay?sort=Name"
 *  </pre>
 */
public class URLBuilder
{
    private final String _controller;
    private final String _action;
    @Nullable private final String _containerPath;

    private Map<String, ?> _query;
    private String _resourcePath;
    private Map<String, ?> _secondaryQuery;

    public URLBuilder(String controller, String action, @Nullable String containerPath)
    {
        _controller = controller;
        _action = action;
        _containerPath = containerPath;
    }

    public URLBuilder(String controller, String action)
    {
        this(controller, action, "/");
    }

    public URLBuilder setQuery(Map<String, ?> query)
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

    public URLBuilder setSecondaryQuery(Map<String, ?> secondaryQuery)
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
                    .replace("+", "%2B")
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
            throw new IllegalArgumentException("Must specify a resource path when using a secondary query");
        }

        return url.toString();
    }

    private void appendQueryString(StringBuilder url, Map<String, ?> params)
    {
        if (params != null && !params.isEmpty())
        {
            boolean firstParam = true;
            for (Map.Entry<String, ?> param : params.entrySet())
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
        else if (!WebTestHelper.isNoQuestionMarkUrl())
        {
            url.append("?");
        }
    }
}
