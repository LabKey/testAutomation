/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

import java.io.File;
import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Feb 7, 2007
 * Time: 11:16:41 PM
 */
public class WebTestHelper
{
    public static final String DEFAULT_CONTEXT_PATH = "/labkey";
    public static final String DEFAULT_WEB_PORT = "8080";
    public static final String DEFAULT_LABKEY_ROOT = "C:/cpas";
    public static final String DEFAULT_TARGET_SERVER = "http://localhost";
    private static String _webPort = null;
    private static String _contextPath = null;
    public static final int MAX_LEAK_LIMIT = 0;
    public static final int GC_ATTEMPT_LIMIT = 5;
    public static final int DEFAULT_BUTTON_FONT_SIZE = 11;

    public static String getWebPort()
    {
        synchronized (DEFAULT_WEB_PORT)
        {
            if (_webPort == null)
            {
                _webPort = System.getProperty("labkey.port");
                if (_webPort == null || _webPort.length() == 0)
                {
                    log("Using default labkey port (" + DEFAULT_WEB_PORT +
                            ").\nThis can be changed by passing VM arg '-Dlabkey.port=[yourport]'.");
                    _webPort = DEFAULT_WEB_PORT;
                }
                else
                    log("Using labkey port '" + _webPort + "', as provided by system property 'labkey.port'.");
            }
            return _webPort;
        }
    }

    public static String getTargetServer()
    {
        synchronized (DEFAULT_TARGET_SERVER)
        {
            if (_targetServer == null)
            {
                _targetServer = System.getProperty("labkey.server");
                if (_targetServer == null || _targetServer.length() == 0)
                {
                    log("Using default target server (" + DEFAULT_TARGET_SERVER +
                            ").\nThis can be changed by passing VM arg '-Dlabkey.server=[yourserver]'.");
                    _targetServer = DEFAULT_TARGET_SERVER;
                }
                else
                    log("Using target server '" + _targetServer + "', as provided by system property 'labkey.server'.");
            }
            return _targetServer;
        }
    }

    public static String getLabKeyRoot()
    {
        synchronized (DEFAULT_LABKEY_ROOT)
        {
            if (_labkeyRoot == null)
            {
                _labkeyRoot = canonicalizePath(System.getProperty("labkey.root"));
                if (_labkeyRoot == null || _labkeyRoot.length() == 0)
                {
                    _labkeyRoot = canonicalizePath(DEFAULT_LABKEY_ROOT);
                    log("Using default labkey root (" + _labkeyRoot +
                            ").\nThis can be changed by passing VM arg '-Dlabkey.root=[yourroot]'.");
                }
                else
                    log("Using labkey root '" + _labkeyRoot + "', as provided by system property 'labkey.root'.");
            }
            return _labkeyRoot;
        }
    }

    public static String canonicalizePath(String path)
    {
        if (path == null)
            return null;
        File f = new File(path);
        if (f.exists())
        {
            try
            {
                return  f.getCanonicalPath();
            }
            catch (IOException e)
            {
                log("WARNING: '" + path + "' may not exist.");
                return null;
            }
        }
        else
        {
            log("WARNING: '" + path + "' may not exist.");
            return null;
        }
    }


    private static String _labkeyRoot = null;
    private static String _targetServer = null;

    public static String getBaseURL()
    {
        String portPortion = "80".equals(getWebPort()) ? "" : ":" + getWebPort();

        return getTargetServer() + portPortion + getContextPath();
    }


    public static String getContextPath()
    {
        synchronized (DEFAULT_CONTEXT_PATH)
        {
            if (_contextPath == null)
            {
                _contextPath = System.getProperty("labkey.contextpath");
                if (_contextPath == null)
                {
                    log("Using default labkey context path (" + DEFAULT_CONTEXT_PATH +
                            ").\nThis can be changed by passing VM arg '-Dlabkey.contextpath=[yourpath]'.");
                    _contextPath = DEFAULT_CONTEXT_PATH;
                }
                else
                    log("Using labkey context path '" + _contextPath + "', as provided by system property 'labkey.contextpath'.");
            }
            return _contextPath;
        }
    }

    public static void log(String message)
    {
        System.out.println(message);
    }

    public static String getTabLinkId(String tabName)
    {
        return tabName + "Tab";
    }

    public static class FolderIdentifier
    {
        private String _projectName;
        private String _folderName;

        public FolderIdentifier(String projectName, String folderName)
        {
            _projectName = projectName;
            _folderName = folderName;
        }

        public String getFolderName()
        {
            return _folderName;
        }

        public String getProjectName()
        {
            return _projectName;
        }
    }

    public static class MapArea
    {
        private String _shape;
        private String _href;
        private String _title;
        private String _alt;
        private String _coords;

        public MapArea(String shape, String href, String title, String alt, String coords)
        {
            _shape = shape;
            _href = href;
            _title = title;
            _alt = alt;
            _coords = coords;
        }

        public String getAlt()
        {
            return _alt;
        }

        public String getCoords()
        {
            return _coords;
        }

        public String getHref()
        {
            return _href;
        }

        public String getShape()
        {
            return _shape;
        }

        public String getTitle()
        {
            return _title;
        }
    }
}
