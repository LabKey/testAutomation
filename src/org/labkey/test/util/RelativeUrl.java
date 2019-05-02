/*
 * Copyright (c) 2014-2018 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RelativeUrl
{
    private final String _controller;
    private String _containerPath = null;
    private final String _action;
    private final Map<String, String> _parameters;
    private Integer _msTimeout;

    public RelativeUrl(String controller, String action)
    {
        _controller = controller;
        if (action.endsWith("Action"))
            action = action.substring(0, action.indexOf("Action"));
        _action = action;
        _parameters = new HashMap<>();
    }

    private RelativeUrl(RelativeUrl copy)
    {
        _controller = copy._controller;
        _containerPath = copy._containerPath;
        _action = copy._action;
        _parameters = new HashMap<>(copy._parameters);
        _msTimeout = copy._msTimeout;
    }

    public RelativeUrl copy()
    {
        return new RelativeUrl(this);
    }

    public String toString()
    {
        return WebTestHelper.buildRelativeUrl(_controller, _containerPath, _action, _parameters);
    }

    public RelativeUrl addParameter(String param)
    {
        addParameter(param, null);
        return this;
    }

    public RelativeUrl addParameters(Map<String, String> params)
    {
        _parameters.putAll(params);
        return this;
    }

    public RelativeUrl addParameter(String param, String value)
    {
        _parameters.put(param, value);
        return this;
    }

    public void navigate(WebDriverWrapper test)
    {
        if (null == _msTimeout)
            test.beginAt(toString());
        else
            test.beginAt(toString(), _msTimeout);
    }

    public RelativeUrl setContainerPath(String containerPath)
    {
        _containerPath = containerPath;
        return this;
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public RelativeUrl setTimeout(Integer msTimeout)
    {
        _msTimeout = msTimeout;
        return this;
    }

    public <P extends LabKeyPage> PageFactory<P> getPageFactory(Function<WebDriver, P> pageConstructor)
    {
        return new PageFactory<>(this, pageConstructor);
    }
}
