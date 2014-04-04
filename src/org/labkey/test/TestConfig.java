/*
 * Copyright (c) 2007-2014 LabKey Corporation
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

package org.labkey.test;

import org.labkey.test.testpicker.TestHelper;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class TestConfig implements Serializable
{
    static final long serialVersionUID = -1490695783905269342L;
    
    String _name;
    boolean _clean;
    boolean _linkCheck;
    boolean _memCheck;
    boolean _loop;
    boolean _cleanOnly;
    boolean _bestBrowser;
    boolean _chrome;
    boolean _firefox;
    boolean _ie;
    boolean _haltOnError;
    String _port;
    String _contextPath;
    String _server;
    String _root;
    List<String> _checkedNodes;


    public TestConfig(String name, boolean clean, boolean linkCheck, boolean memCheck, boolean loop, boolean cleanOnly, boolean bestBrowser, boolean chrome, boolean firefox, boolean ie, String port, String contextPath, String server, String labkeyRoot, List<String> checkedNodes, Boolean haltOnError)
    {
        _name = name;
        _clean = clean;
        _linkCheck = linkCheck;
        _memCheck = memCheck;
        _loop = loop;
        _cleanOnly = cleanOnly;
        _bestBrowser = bestBrowser;
        _chrome = chrome;
        _firefox = firefox;
        _ie = ie;
        _port = port;
        _contextPath = contextPath;
        _server = server;
        _root = labkeyRoot;
        _checkedNodes = checkedNodes;
        _haltOnError = haltOnError;
    }

    public TestConfig()
    {
        _name = "";
        _clean = true;
        _linkCheck = false;
        _memCheck = false;
        _loop = false;
        _cleanOnly = false;
        _bestBrowser = true;
        _chrome = false;
        _firefox = false;
        _ie = false;
        _port = TestHelper.DEFAULT_PORT;
        _contextPath = TestHelper.DEFAULT_CONTEXT_PATH;
        _server = TestHelper.DEFAULT_SERVER;
        _root = TestHelper.DEFAULT_ROOT;
        _checkedNodes = new ArrayList<>();
        _haltOnError = true;
    }

    public String getName()
    {
        return _name;
    }

    public boolean isClean()
    {
        return _clean;
    }

    public boolean isLinkCheck()
    {
        return _linkCheck;
    }

    public boolean isMemCheck()
    {
        return _memCheck;
    }

    public boolean isLoop()
    {
        return _loop;
    }

    public Boolean isHaltOnError()
    {
        return _haltOnError;
    }

    public boolean isCleanOnly()
    {
        return _cleanOnly;
    }

    public boolean isBestBrowser()
    {
        return _bestBrowser;
    }

    public void setBestBrowser(boolean bestBrowser)
    {
        _bestBrowser = bestBrowser;
    }

    public boolean isChrome()
    {
        return _chrome;
    }

    public void setChrome(boolean chrome)
    {
        _chrome = chrome;
    }

    public boolean isFirefox()
    {
        return _firefox;
    }

    public void setFirefox(boolean firefox)
    {
        _firefox = firefox;
    }

    public boolean isIe()
    {
        return _ie;
    }

    public void setIe(boolean ie)
    {
        _ie = ie;
    }

    public String getPort()
    {
        return _port;
    }

    public String getContextPath()
    {
        return _contextPath;
    }

    public String getServer()
    {
        return _server;
    }

    public String getRoot()
    {
        return _root;
    }

    public List<String> getConfigCheckedNodes()
    {
        return _checkedNodes;
    }
}
