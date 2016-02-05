/*
 * Copyright (c) 2014-2016 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebDriverWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelativeUrl
{
    private String _controller;
    private String _containerPath;
    private String _action;
    private Map<String, String> _parameters;
    private Integer _msTimeout;

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
        List<String> encodedPath = new ArrayList<>();

        for (String node : _containerPath.split("/"))
        {
            encodedPath.add(EscapeUtil.encode(node));
        }

        return StringUtils.join(encodedPath, "/");
    }

    public void addParameter(String param)
    {
        addParameter(param, null);
    }

    public void addParameter(String param, String value)
    {
        _parameters.put(param, value);
    }

    public void navigate(WebDriverWrapper test)
    {
        if (null == _msTimeout)
            test.beginAt(toString());
        else
            test.beginAt(toString(), _msTimeout);
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

    public void setTimeout(Integer msTimeout)
    {
        _msTimeout = msTimeout;
    }
}
