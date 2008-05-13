/*
 * Copyright (c) 2007 LabKey Corporation
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Mar 22, 2007
 * Time: 1:32:25 PM
 */
public class StopSelenium
{
    private static final int DEFAULT_SELENIUM_PORT = 4444;
    private static final String DEFAULT_SELENIUM_SERVER = "localhost";
    public static void main(String[] params) throws Exception
    {
        String server = System.getenv("selenium.server");
        if (null == server)
            server = DEFAULT_SELENIUM_SERVER;

        int port;
        String portStr = System.getenv("selenium.port");
        if (null != portStr)
            port = Integer.parseInt(portStr);
        else
            port = DEFAULT_SELENIUM_PORT;

        URL url = new URL("http", server, port, "/selenium-server/driver/?cmd=shutDown");
        InputStream is = (InputStream) url.getContent();
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        System.out.println(r.readLine());
        r.close();
        is.close();
    }
}
