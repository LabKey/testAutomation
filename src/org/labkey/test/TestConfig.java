package org.labkey.test;

import org.labkey.test.testpicker.TestHelper;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: ulberge
 * Date: Aug 27, 2007
 * Time: 4:41:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestConfig implements Serializable
{
    static final long serialVersionUID = -1490695783905269342L;
    
    String _name;
    boolean _clean;
    boolean _linkCheck;
    boolean _memCheck;
    boolean _loop;
    boolean _cleanOnly;
    String _port;
    String _contextPath;
    String _server;
    String _root;
    List<String> _checkedNodes;


    public TestConfig(String name, boolean clean, boolean linkCheck, boolean memCheck, boolean loop, boolean cleanOnly, String port, String contextPath, String server, String labkeyRoot, List<String> checkedNodes)
    {
        _name = name;
        _clean = clean;
        _linkCheck = linkCheck;
        _memCheck = memCheck;
        _loop = loop;
        _cleanOnly = cleanOnly;
        _port = port;
        _contextPath = contextPath;
        _server = server;
        _root = labkeyRoot;
        _checkedNodes = checkedNodes;
    }

    public TestConfig()
    {
        _name = "";
        _clean = true;
        _linkCheck = false;
        _memCheck = false;
        _loop = false;
        _cleanOnly = false;
        _port = TestHelper.DEFAULT_PORT;
        _contextPath = TestHelper.DEFAULT_CONTEXT_PATH;
        _server = TestHelper.DEFAULT_SERVER;
        _root = TestHelper.DEFAULT_ROOT;
        _checkedNodes = new ArrayList<String>();
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

    public boolean isCleanOnly()
    {
        return _cleanOnly;
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
