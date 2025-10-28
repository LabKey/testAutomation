/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;

import static org.junit.Assert.assertTrue;

/**
 * User: tgaluhn
 * Date: 11/4/2016
 *
 * Helper methods for verifying a web services host ip address is reachable and
 * listening on any given port(s).
 */
public class WebServicesUtil
{
    static public void assertServicesAvailable(String hostIp, int... ports)
    {
        StringBuilder sb = new StringBuilder();

        if (!isHostReachable(hostIp))
        {
            sb.append("Host not reachable on ip address ").append(hostIp);
        }
        else
        {
            String comma = "";
            for (int port : ports)
            {
                if (!isPortListening(hostIp, port))
                {
                    sb.append(comma);
                    sb.append(port);
                    comma = ", ";
                }
            }
            if (!sb.isEmpty())
            {
                sb.insert(0, "Could not connect to port(s): ");
            }
        }

        assertTrue(sb.toString(), sb.isEmpty());
    }

    static public boolean isHostReachable(String hostIp)
    {
        try
        {
            InetAddress address = InetAddress.getByName(hostIp);
            return address.isReachable(5000);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    static public boolean isPortListening(String hostIp, int port)
    {
        try (Socket ignored = openSocket(hostIp, port))
        {
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    static public Socket openSocket(String hostIp, int port) throws IOException
    {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(hostIp, port), 2000);
        return socket;
    }

    /**
     * Checks whether the current page is the maintenance page for labkey.org.
     * This is usually indicated by a 502 response code but we will accept any 5xx response.
     * Allows tests that check links to labkey.org to not fail during maintenance periods.
     *
     * @param driver The WebDriver instance to check
     * @return true if the current page is the maintenance page for labkey.org, false otherwise.
     */
    public static boolean isLabKeyDotOrgMaintenance(WebDriver driver)
    {
        try
        {
            URL url = new URL(Objects.requireNonNull(driver.getCurrentUrl()));
            String title = driver.getTitle();
            int responseCode = Integer.parseInt(Objects.requireNonNull(title).substring(0, 3));
            return url.getHost().endsWith("labkey.org") && responseCode >= 500 && responseCode < 600;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
