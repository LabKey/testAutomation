package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.WebTestHelper;
import org.seleniumhq.jetty9.util.URIUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
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
    // More permissive than reality but good enough to prevent egregious values.
    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("[a-zA-Z\\-]+");
    private static final Pattern ACTION_PATTERN = Pattern.compile("[0-9a-zA-Z\\-.]+");

    private final String _controller;
    private final String _action;
    @Nullable private final String _containerPath;

    private Map<String, ?> _query = Collections.emptyMap();
    private String _resourcePath;
    private Map<String, ?> _secondaryQuery;

    private boolean _questionMarkUrl = !WebTestHelper.isNoQuestionMarkUrl();

    /**
     * Intialize a URLBuilder for a LabKey URL
     * @param controller the controller name (e.g. "login" for "LoginController")
     * @param action the action name (e.g. "whoami" for "WhoAmIAction")
     *              The action type will be assumed to be ".view" if not specified.
     * @param containerPath the server containerPath (e.g. "Home/support"). 'null' indicates the root container.
     */
    public URLBuilder(String controller, String action, @Nullable String containerPath)
    {
        _controller = verifyController(controller);
        _action = verifyAction(action);
        _containerPath = verifyContainerPath(containerPath);
    }

    public URLBuilder(String controller, String action)
    {
        this(controller, action, null);
    }

    private static String verifyController(String controller)
    {
        if (!CONTROLLER_PATTERN.matcher(controller).matches())
        {
            throw new IllegalArgumentException("Invalid controller: " + controller);
        }
        return controller;
    }

    private static String verifyAction(String action)
    {
        if (!ACTION_PATTERN.matcher(action).matches())
        {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        return action;
    }

    /**
     * Strip leading/trailing slashes. Root container ends up as 'null'
     */
    private static String verifyContainerPath(String containerPath)
    {
        return StringUtils.stripToNull(StringUtils.strip(containerPath, "/ "));
    }

    public URLBuilder setQuery(Map<String, ?> query)
    {
        _query = query;
        return this;
    }

    /**
     * Override the setting for whether to always include a '?' on URLs
     */
    public URLBuilder setQuestionMarkUrl(boolean questionMarkUrl)
    {
        _questionMarkUrl = questionMarkUrl;
        return this;
    }

    public URLBuilder setAppResourcePath(Object... pathParts)
    {
        List<String> encodedParts = Arrays.stream(pathParts).map(Objects::requireNonNull)
                .map(String::valueOf).map(EscapeUtil::encode).collect(Collectors.toList());
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
            url.append("?"); // We have a '?' after URLs even if there's no query
            boolean firstParam = true;
            for (Map.Entry<String, ?> param : params.entrySet())
            {
                if (null != param.getKey())
                {
                    url.append(firstParam ? "" : "&");
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
