package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.WebTestHelper;
import org.seleniumhq.jetty9.util.URIUtil;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.labkey.test.WebTestHelper.getBaseURL;

/**
 * Constructs a LabKey URL for use by tests. LabKey docs have more information about the structure of LabKey URLs.
 * (e.g. <code>https://www.labkey.org/Documentation/wiki-page.view?name=url</code>)
 * <br><br>
 * For this builder's purposes, a URL consists of the following:
 * <ul>
 *  <li>controller</li>
 *  <li>action</li>
 *  <li>containerPath [optional]</li>
 *  <li>query [optional]</li>
 *  <li>resourcePath [optional]</li>
 *  <li>secondaryQuery [optional]</li>
 * </ul>
 * <br>
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
    private String _fragment;
    private Map<String, ?> _secondaryQuery;

    /**
     * Intialize a URLBuilder for a LabKey URL
     * @param controller the controller name (e.g. "login" for "LoginController")
     * @param action the action name (e.g. "whoami" for "WhoAmIAction")
     *              The action type will be assumed to be ".view" if not specified.
     * @param containerPath the server containerPath (e.g. "Home/support"). 'null' indicates the root container.
     */
    public URLBuilder(String controller, String action, @Nullable String containerPath)
    {
        _controller = Objects.requireNonNull(controller);
        _action = Objects.requireNonNull(action);
        _containerPath = verifyContainerPath(containerPath);
    }

    public URLBuilder(String controller, String action)
    {
        this(controller, action, null);
    }

    /**
     * Strip leading/trailing slashes. Root container ends up as 'null'
     */
    private static String verifyContainerPath(String containerPath)
    {
        return StringUtils.stripToNull(StringUtils.strip(containerPath, "/ "));
    }

    /**
     * Appends the specified parameters to the URL.
     * Note: Names and values will not be encoded. Use {@link java.net.URLEncoder#encode(String, Charset)} if encoding
     * is needed (usually only necessary for comparing URLs)
     * @param query URL query parameters. 'null' values will be included as valueless parameters.
     * @return this builder
     */
    public URLBuilder setQuery(Map<String, ?> query)
    {
        _query = query;
        return this;
    }

    /**
     * Append the app path as a URL fragment. Parts will be encoded and joined.<br>
     * e.g. <code>setAppResourcePath("workbook", 5)</code> will append "#/workbook/5" to the built URL.<br>
     * Note: Will replace any previously set fragment.
     *
     * @param pathParts Parts to be combined into an app path. Most likely strings and/or Integers
     * @return this builder
     * @see #setFragment(String)
     */
    public URLBuilder setAppResourcePath(Object... pathParts)
    {
        List<String> encodedParts = Arrays.stream(pathParts).map(Objects::requireNonNull).map(String::valueOf)
                .map(EscapeUtil::encode).collect(Collectors.toList());
        setFragment("/" + String.join("/", encodedParts));
        return this;
    }

    /**
     * Append a fragment to the URL.<br>
     * e.g. <code>setResourcePath("marker")</code> will append "#marker" to the built URL
     *
     * @param fragment resource path to be appended. Will not be encoded or checked for validity.
     * @return this builder
     */
    public URLBuilder setFragment(String fragment)
    {
        _fragment = fragment;
        return this;
    }

    /**
     * Appends a secondary query to the URL. That is to say, a query that appears AFTER the resource path.<br>
     * If a resource path is not specified, an exception will be thrown when building the URL.
     *
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
     *
     * @return built URL
     */
    public String buildURL()
    {
        return getBaseURL() + buildRelativeURL();
    }

    /**
     * Build a relative URL. That is to say, excluding the server's host name, port, or context path.
     * Will have a leading slash.
     *
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

        if (_containerPath != null) // null is root container; nothing to append.
        {
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

        if (!StringUtils.isBlank(_fragment))
        {
            if (!_fragment.startsWith("#"))
            {
                url.append("#");
            }
            url.append(_fragment);
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
