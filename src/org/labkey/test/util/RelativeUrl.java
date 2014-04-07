package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelativeUrl
{
    private String _controller;
    private String _containerPath;
    private String _action;
    private Map<String, String> _parameters;

    public RelativeUrl(String controller, String action)
    {
        setController(controller);
        setAction(action);
        setContainerPath(null);
        _parameters = new HashMap<>();
    }

    private String paramString()
    {
        StringBuilder paramString = new StringBuilder();

        for (Map.Entry<String, String> param : _parameters.entrySet())
        {
            if (paramString.length() == 0)
                paramString.append("?");
            else
                paramString.append("&");

            paramString.append(param.getKey());

            if (param.getValue() != null)
            {
                paramString.append("=");
                paramString.append(param.getValue());
            }
        }

        return paramString.toString();
    }

    public String toString()
    {
        StringBuilder relativeURL = new StringBuilder();

        relativeURL.append(_controller);
        relativeURL.append("/");

        if (_containerPath != null)
        {
            relativeURL.append(encodeContainerPath());
            relativeURL.append("/");
        }

        relativeURL.append(_action);
        relativeURL.append(".view");

        relativeURL.append(paramString());

        return relativeURL.toString();
    }

    private String encodeContainerPath()
    {
        String[] splitPath = _containerPath.split("/");

        for (String node : splitPath)
        {
            node = EscapeUtil.encode(node);
        }

        return StringUtils.join(splitPath, "/");
    }

    public void addParameter(String param)
    {
        addParameter(param, null);
    }

    public void addParameter(String param, String value)
    {
        _parameters.put(param, value);
    }

    public void navigate(BaseWebDriverTest test)
    {
        test.beginAt(toString());
    }

    public void setController(String controller)
    {
        _controller = controller;
    }

    public void setContainerPath(String containerPath)
    {
        _containerPath = containerPath;
    }

    public void setAction(String action)
    {
        if (action.endsWith("Action"))
            action = action.substring(0, action.indexOf("Action"));

        _action = action;
    }
}
