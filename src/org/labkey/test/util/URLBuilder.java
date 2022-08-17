package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.WebTestHelper;
import org.seleniumhq.jetty9.util.URIUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private boolean _questionMarkUrl = !WebTestHelper.isNoQuestionMarkUrl();

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

    /**
     * Appends a secondary query to the URL. That is to say, a query that appears AFTER the resource path.
     * If a resource path is not specified, an exception will be thrown with building the URL.
     * @param query URL query parameters. 'null' values will be included as valueless parameters.
     * @return this builder
     */
    public URLBuilder setQuery(Map<String, ?> query)
    {
        _query = query;
        return this;
    }

    /**
     * Override the setting for whether to always include a '?' on URLs
     * The default setting is controlled by the server's 'noQuestionMarkUrl' experimental feature
     */
    public URLBuilder setQuestionMarkUrl(boolean questionMarkUrl)
    {
        _questionMarkUrl = questionMarkUrl;
        return this;
    }

    /**
     * Parts to be combined into an app path. Will replace any existing resource path.<br>
     * e.g. <code>setAppResourcePath("workbook", 5)</code> will append "#/workbook/5" to the built URL
     * @param pathParts Most likely strings or Integers
     * @return this builder
     */
    public URLBuilder setAppResourcePath(Object... pathParts)
    {
        List<String> encodedParts = Arrays.stream(pathParts).map(Objects::requireNonNull).map(String::valueOf)
                .map(EscapeUtil::encode).collect(Collectors.toList());
        _resourcePath = "/" + String.join("/", encodedParts);
        return this;
    }

    /**
     * Append a resource path to the URL.<br>
     * e.g. <code>setResourcePath("marker")</code> will append "#marker" to the built URL
     *
     * @param resourcePath resource path to be appended. Will not be encoded or checked for validity.
     * @return this builder
     */
    public URLBuilder setResourcePath(String resourcePath)
    {
        _resourcePath = resourcePath;
        return this;
    }

    /**
     * Appends a secondary query to the URL. That is to say, a query that appears AFTER the resource path.
     * If a resource path is not specified, an exception will be thrown with building the URL.
     * @param secondaryQuery URL query parameters. 'null' values will be included as valueless parameters.
     * @return this builder
     */
    public URLBuilder setSecondaryQuery(Map<String, ?> secondaryQuery)
    {
        _secondaryQuery = secondaryQuery;
        return this;
    }

    /**
     * Build the full URL
     * @return built URL
     */
    public String buildURL()
    {
        return getBaseURL() + buildRelativeURL();
    }

    /**
     * Build a relative URL. That is to say, excluding the server's host name, port, or context path.
     * @return built URL
     */
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
        if (_questionMarkUrl && Maps.isBlank(_query))
        {
            url.append("?");
        }

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
        if (!Maps.isBlank(params))
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
    }
}
