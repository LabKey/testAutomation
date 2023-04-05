/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.Connection;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A test utility for performing arbitrary HTTP requests
 * Use if you want to avoid the URI verification forced by Apache HttpClient
 * Can copy session info from a running WebDriver instance, including CSRF
 */
public class SimpleHttpRequest
{
    private String _url;
    private String _requestMethod = "GET";
    private String _username = PasswordUtil.getUsername();
    private String _password = PasswordUtil.getPassword();
    private int _timeout = 30000;
    private Map<String, String> _cookies = Collections.emptyMap();

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

    public void setTimeout(int timeout)
    {
        _timeout = timeout;
    }

    public SimpleHttpResponse getResponse() throws IOException
    {
        HttpURLConnection con = null;

        try
        {
            URL url = new URL(_url);
            if (_username != null && _password != null)
            {
                Authenticator.setDefault(new Authenticator()
                {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(_username, _password.toCharArray());
                    }
                });
            }
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(_requestMethod);
            con.setReadTimeout(_timeout);

            if (!_cookies.isEmpty())
            {
                useCopiedSession(con);
            }
            else
            {
                // Authenticator.setDefault() call above doesn't seem to work (I don't know why), so add the basic auth header explicitly
                String encoded = Base64.getEncoder().encodeToString((_username + ":" + _password).getBytes(StandardCharsets.UTF_8));
                con.setRequestProperty("Authorization", "Basic " + encoded);
            }

            con.connect();
            return SimpleHttpResponse.readResponse(con);
        }
        finally
        {
            if (con != null)
                con.disconnect();
            Authenticator.setDefault(null);
        }
    }

    /**
     * Attempts to save the request's response to a file. If the target file is an existing directory, the response will
     * be streamed into a file within the directory. The file name will be extracted from response headers, if possible.
     * If the target file is an existing file, it will be overwritten. If the target file does not exist, it will be
     * created (parent directories will not be created).
     * @param targetFile Target file or directory
     * @return File pointer to the saved file
     * @throws IOException if the download fails for some reason
     */
    public File getResponseAsFile(File targetFile) throws IOException
    {
        String fileName = targetFile.isDirectory() ? null : targetFile.getName();

        HttpURLConnection con = null;

        try
        {
            URL url = new URL(_url);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(_requestMethod);
            con.setReadTimeout(_timeout);

            if (!_cookies.isEmpty())
            {
                useCopiedSession(con);
            }
            else
            {
                // Authenticator.setDefault() call above doesn't seem to work (I don't know why), so add the basic auth header explicitly
                String encoded = Base64.getEncoder().encodeToString((_username + ":" + _password).getBytes(StandardCharsets.UTF_8));
                con.setRequestProperty("Authorization", "Basic " + encoded);
            }

            con.connect();

            if (con.getResponseCode() != 200)
            {
                TestLogger.error(IOUtils.toString(con.getErrorStream(), StandardCharsets.UTF_8));
                throw new IOException("Failed to download file [%d]: %s".formatted(con.getResponseCode(), con.getResponseMessage()));
            }
            else
            {
                // Extract file name from response header.
                // Example:
                // attachment; filename="diagnostics_2023-03-31_12-36-31.zip"
                String contentDisposition = StringUtils.trimToEmpty(con.getHeaderField("Content-Disposition"));
                String[] splitDisposition = contentDisposition.split("; ");
                Map<String, String> params = new LinkedHashMap<>();
                for (String s : splitDisposition)
                {
                    String[] param = s.split("=", 2);
                    params.put(param[0], param.length == 2 ? StringUtils.strip(param[1], "\"") : null);
                }
                String responseFilename = params.get("filename");
                if (fileName == null)
                {
                    if (responseFilename == null)
                    {
                        throw new IOException("Unable to determine filename for download.");
                    }
                    fileName = responseFilename;
                    targetFile = new File(targetFile, fileName);
                }
                else if (responseFilename != null && fileName.contains("."))
                {
                    // Verify correct extension
                    String expectedExtension = fileName.split("\\.", 2)[1];
                    if (!responseFilename.endsWith("." + expectedExtension))
                    {
                        throw new IOException("Download didn't match expected extension.\n%s\n%s".formatted(responseFilename, fileName));
                    }
                }
                FileUtils.copyInputStreamToFile(con.getInputStream(), targetFile);
                TestLogger.info("%s: Downloaded [%s] from %s".formatted(targetFile.getName(), FileUtils.byteCountToDisplaySize(targetFile.length()), _url));
                return targetFile;
            }
        }
        finally
        {
            if (con != null)
                con.disconnect();
        }
    }

    private void useCopiedSession(HttpURLConnection con)
    {
        StringBuilder cookieString = new StringBuilder();
        for (Map.Entry cookie : _cookies.entrySet())
        {
            if (cookie.getKey().equals(Connection.X_LABKEY_CSRF))
                con.setRequestProperty((String)cookie.getKey(), (String)cookie.getValue());

            if (cookie.getKey().equals(Connection.JSESSIONID))
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
        setCookies(driver.manage().getCookies());
    }

    public void setCookies(Collection<Cookie> cookies)
    {
        _cookies = new HashMap<>();
        for (Cookie cookie : cookies)
        {
            _cookies.put(cookie.getName(), cookie.getValue());
        }
    }

    public void clearSession()
    {
        _cookies = Collections.emptyMap();
    }
}
