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
