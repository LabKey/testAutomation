package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A test utility for performing arbitrary HTTP requests
 * Use if you want to avoid the URI verification forced by Apache HttpClient
 * Can copy session info from a running WebDriver instance, including CSRF
 */
public class SimpleHttpRequest
{
    String _url;
    String _requestMethod = "GET";
    String _username = PasswordUtil.getUsername();
    String _password = PasswordUtil.getPassword();
    Map<String, String> _cookies = Collections.emptyMap();

    public SimpleHttpRequest(String url)
    {
        _url = url;
    }

    public SimpleHttpRequest(String url, String requestMethod)
    {
        this(url);
        _requestMethod = requestMethod;
    }

    public void setUrl(String url)
    {
        _url = url;
    }

    public void setLogin(String username, String password)
    {
        _username = username;
        _password = password;
    }

    public void clearLogin()
    {
        setLogin(null, null);
    }

    public void setRequestMethod(String requestMethod)
    {
        _requestMethod = requestMethod;
    }

    public SimpleHttpResponse getResponse()
    {
        HttpURLConnection con = null;

        int responseCode;
        try
        {
            URL url = new URL(_url);
            if (_username != null && _password != null)
            {
                Authenticator.setDefault(new Authenticator()
                {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(_username, _password.toCharArray());
                    }
                });
            }
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(_requestMethod);
            con.setReadTimeout(BaseWebDriverTest.WAIT_FOR_PAGE);
            useCopiedSession(con);
            con.connect();
            return SimpleHttpResponse.readResponse(con);
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            if (con != null)
                con.disconnect();
            Authenticator.setDefault(null);
        }
    }

    private void useCopiedSession(HttpURLConnection con)
    {
        StringBuilder cookieString = new StringBuilder();
        for (Map.Entry cookie : _cookies.entrySet())
        {
            if (cookie.getKey().equals("X-LABKEY-CSRF"))
                con.setRequestProperty((String)cookie.getKey(), (String)cookie.getValue());

            if (cookieString.length() > 0)
                cookieString.append("; ");

            cookieString.append(cookie.getKey());
            cookieString.append("=");
            cookieString.append(cookie.getValue());
        }

        con.setRequestProperty("Cookie", cookieString.toString());
    }

    public void copySession(WebDriver driver)
    {
        _cookies = new HashMap<>();
        for (Cookie cookie : driver.manage().getCookies())
        {
            _cookies.put(cookie.getName(), cookie.getValue());
        }
    }

    public void clearSession()
    {
        _cookies = Collections.emptyMap();
    }
}
