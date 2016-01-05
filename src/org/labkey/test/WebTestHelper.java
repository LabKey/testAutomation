/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.reader.Readers;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.util.InstallCert;
import org.labkey.test.util.PasswordUtil;
import org.seleniumhq.jetty9.util.URIUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Static methods for getting properties of and communicating with a running LabKey server
 */
public class WebTestHelper
{
    private static final String DEFAULT_CONTEXT_PATH = "/labkey";
    private static final Integer DEFAULT_WEB_PORT = 8080;
    private static final String DEFAULT_TARGET_SERVER = "http://localhost";
    private static String _targetServer = null;
    private static Integer _webPort = null;
    private static String _contextPath = null;
    public static final int MAX_LEAK_LIMIT = 0;
    public static final int GC_ATTEMPT_LIMIT = 6;
    public static long leakCRC = 0;

    private static void acceptLocalhostCert() throws Exception
    {
        String keystorePassword = System.getProperty("keystore.password", "changeit");
        InstallCert.install("localhost", _webPort, keystorePassword.toCharArray());
    }

    public static boolean isLocalServer()
    {
        return getTargetServer().contains("localhost") || getTargetServer().contains("127.0.0.1");
    }

    public static Integer getWebPort()
    {
        synchronized (DEFAULT_WEB_PORT)
        {
            if (_webPort == null)
            {
                String webPortStr = System.getProperty("labkey.port");
                if (webPortStr == null || webPortStr.length() == 0)
                {
                    System.out.println("Using default labkey port (" + DEFAULT_WEB_PORT +
                                        ").\nThis can be changed by passing VM arg '-Dlabkey.port=[yourport]'.");
                    _webPort = DEFAULT_WEB_PORT;
                }
                else
                {
                    _webPort = Integer.parseInt(webPortStr);
                    System.out.println("Using labkey port '" + _webPort + "', as provided by system property 'labkey.port'.");
                }
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
                    System.out.println("Using default target server (" + DEFAULT_TARGET_SERVER +
                                        ").\nThis can be changed by passing VM arg '-Dlabkey.server=[yourserver]'.");
                    _targetServer = DEFAULT_TARGET_SERVER;
                }
                else
                    System.out.println("Using target server '" + _targetServer + "', as provided by system property 'labkey.server'.");
            }
            return _targetServer;
        }
    }

    public static String stripContextPath(String url)
    {
        String root = getContextPath() + "/";
        int rootLoc = url.indexOf(root);
        int endOfAction = url.indexOf("?");
        if ((rootLoc != -1) && (endOfAction == -1 || rootLoc < endOfAction))
            url = url.substring(rootLoc + root.length());
        else if (url.indexOf("/") == 0)
            url = url.substring(1);
        return url;
    }

    public enum DatabaseType {PostgreSQL, MicrosoftSQLServer}

    public static DatabaseType getDatabaseType()
    {
        String databaseType = getServerProperty("databaseType");

        if (null == databaseType)
            throw new IllegalStateException("Can't determine database type: databaseType property is not set");

        if ("postgres".equals(databaseType) || "pg".equals(databaseType))
            return DatabaseType.PostgreSQL;

        if ("sqlserver".equals(databaseType) || "mssql".equals(databaseType))
            return DatabaseType.MicrosoftSQLServer;

        throw new IllegalStateException("Unknown database type: " + databaseType);
    }

    private static String getServerProperty(String property)
    {
        String val = System.getProperty(property);

        if (val == null)
        {
            File propertiesFile = new File(TestFileUtils.getLabKeyRoot(), "server/config.properties");

            try (InputStream in = new FileInputStream(propertiesFile))
            {
                Properties prop = new Properties();

                prop.load(in);

                val = prop.getProperty(property);
            }
            catch (IOException ignore)
            {
            }
        }
        return val;
    }

    public static String getDatabaseVersion()
    {
        return getServerProperty("databaseVersion");
    }

    public static String getBaseURL()
    {
        String portPortion = 80 == getWebPort() ? "" : ":" + getWebPort();

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
                    System.out.println("Using default labkey context path (" + DEFAULT_CONTEXT_PATH +
                                        ").\nThis can be changed by passing VM arg '-Dlabkey.contextpath=[yourpath]'.");
                    _contextPath = DEFAULT_CONTEXT_PATH;
                }
                else
                    System.out.println("Using labkey context path '" + _contextPath + "', as provided by system property 'labkey.contextPath'.");
            }
            return _contextPath;
        }
    }

    public static String buildURL(String controller, String action)
    {
        return buildURL(controller, null, action, Collections.EMPTY_MAP);
    }

    public static String buildURL(String controller, String action, @Nullable Map<String, String> params)
    {
        return buildURL(controller, null, action, params);
    }

    public static String buildURL(String controller, @Nullable String containerPath, String action)
    {
        return buildURL(controller, containerPath, action, Collections.EMPTY_MAP);
    }

    public static String buildURL(String controller, @Nullable String containerPath, String action, Map<String, String> params)
    {
        StringBuilder url = new StringBuilder(getBaseURL());

        url.append("/");
        url.append(controller);

        if (containerPath != null)
        {
            url.append("/");
            url.append(URIUtil.encodePath(containerPath));
        }

        url.append("/");
        url.append(action);
        if (!action.contains("."))
            url.append(".view");

        boolean firstParam = true;
        for (Map.Entry param : params.entrySet())
        {
            url.append(firstParam ? "?" : "&");
            url.append(param.getKey());
            url.append("=");
            url.append(param.getValue());
            firstParam = false;
        }

        return url.toString();
    }

    // Writes message to the labkey server log. Message parameter is output as sent, except that \\n is translated to newline.
    public static void logToServer(String message) throws IOException, CommandException
    {
        try(CloseableHttpClient client = (CloseableHttpClient)getHttpClient())
        {
            logToServer(message, client, WebTestHelper.getBasicHttpContext());
        }
    }

    public static void logToServer(String message, HttpClient client, HttpContext context) throws CommandException, IOException
    {
        if (message.contains("\n"))
        {
            String [] splitMessage = message.split("\n");
            for (String thisMessage: splitMessage)
            {
                if (thisMessage.length() > 0)
                    logToServer(thisMessage, client, context);
            }
            return;
        }
        String encodedUrl = getBaseURL() + "/admin/log.view?message=" + encodeURI(message);
        HttpResponse response = null;
        int responseCode;
        String responseStatusLine = "";
        try
        {
            HttpGet get = new HttpGet(encodedUrl);
            response = client.execute(get, context);
            responseCode = response.getStatusLine().getStatusCode();
            responseStatusLine = response.getStatusLine().toString();
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
        if (responseCode != HttpStatus.SC_OK)
            throw new CommandException("Contacting server at URL " + encodedUrl + " failed: " + responseStatusLine);
    }

    private static String encodeURI(String parameter)
    {
        // Percent-escape any characters that cause GetMethod to throw an exception.
        return parameter.replaceAll(" ", "%20");
    }

    public static HttpClient getHttpClient()
    {
        return getHttpClient(PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static HttpClient getHttpClient(String username, String password)
    {
        return getHttpClientBuilder(username, password).build();
    }

    public static HttpClientBuilder getHttpClientBuilder()
    {
        return getHttpClientBuilder(PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static HttpClientBuilder getHttpClientBuilder(String username, String password)
    {
        URI target;
        try
        {
            target = new URI(getBaseURL());
        }
        catch (URISyntaxException ex)
        {
            throw new RuntimeException(ex);
        }

        HttpHost targetHost = new HttpHost(target.getHost(), target.getPort(), target.getScheme());
        AuthScope authScope = new AuthScope(targetHost.getHostName(), target.getPort());
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(authScope, credentials);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(60000)
                .setConnectTimeout(60000)
                .setConnectionRequestTimeout(60000)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setCharset(Charset.forName("UTF-8"))
                .build();

        SSLConnectionSocketFactory socketFactory;
        try
        {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            socketFactory = new SSLConnectionSocketFactory(builder.build());
        }
        catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex)
        {
            throw new RuntimeException(ex);
        }

        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultConnectionConfig(connectionConfig)
                .setSSLSocketFactory(socketFactory);

        return clientBuilder;
    }

    public static HttpClientContext getBasicHttpContext()
    {
        try
        {
            URI target = new URI(getBaseURL());
            HttpHost targetHost = new HttpHost(target.getHost(), target.getPort(), target.getScheme());

            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            // Add AuthCache to the execution context
            HttpClientContext localcontext = HttpClientContext.create();
            localcontext.setAuthCache(authCache);

            return localcontext;
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String getHttpResponseBody(HttpResponse response)
    {
        StringBuilder builder = new StringBuilder();
        try
        {
            BufferedReader br = Readers.getReader(response.getEntity().getContent());

            String thisLine;
            while ((thisLine = br.readLine()) != null)
            {
                builder.append(thisLine);
            }
        }
        catch (IOException ex)
        {/*ignore*/}

        return builder.toString();
    }

    public static int getHttpGetResponse(String url) throws IOException
    {
        return getHttpGetResponse(url, PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static int getHttpGetResponse(String url, String username, String password) throws IOException
    {
        HttpResponse response = null;
        int status;

        try (CloseableHttpClient client = (CloseableHttpClient)getHttpClient(username, password))
        {
            HttpGet get = new HttpGet(url);

            response = client.execute(get, getBasicHttpContext());
            status = response.getStatusLine().getStatusCode();
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
        return status;
    }

    public static int getHttpPostResponse(String url) throws IOException
    {
        return getHttpPostResponse(url, PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static int getHttpPostResponse(String url, String username, String password) throws IOException
    {
        HttpResponse response = null;
        int status;

        try (CloseableHttpClient client = (CloseableHttpClient)getHttpClient(username, password))
        {
            HttpPost post = new HttpPost(url);
            HttpClientContext context = getBasicHttpContext();
            response = client.execute(post, context);
            status = response.getStatusLine().getStatusCode();
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
        return status;
    }

    public static String getHttpGetResponseBody(String url) throws HttpException, IOException
    {
        return getHttpGetResponseBody(url, PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static String getHttpGetResponseBody(String url, String username, String password) throws HttpException, IOException
    {
        HttpResponse response = null;
        String responseBody;

        try (CloseableHttpClient client = (CloseableHttpClient)getHttpClient(username, password))
        {
            HttpGet method = new HttpGet(url);

            response = client.execute(method, getBasicHttpContext());
            responseBody = getHttpResponseBody(response);
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
        return responseBody;
    }

    public static String getHttpPostResponseBody(String url) throws HttpException, IOException
    {
        return getHttpPostResponseBody(url, PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    public static String getHttpPostResponseBody(String url, String username, String password) throws HttpException, IOException
    {
        HttpResponse response = null;
        String responseBody;

        try (CloseableHttpClient client = (CloseableHttpClient)getHttpClient(username, password))
        {
            HttpPost post = new HttpPost(url);

            response = client.execute(post, getBasicHttpContext());
            responseBody = getHttpResponseBody(response);
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
        return responseBody;
    }

    public static class FolderIdentifier
    {
        private final String _projectName;
        private final String _folderName;

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

        public boolean equals(FolderIdentifier o)
        {
            return getProjectName().equalsIgnoreCase(o.getProjectName()) &&
                   getFolderName().equalsIgnoreCase(o.getFolderName());
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof FolderIdentifier) && equals((FolderIdentifier)o);
        }

        @Override
        public int hashCode() {
            return (41 * (41 + getProjectName().hashCode()) + getFolderName().hashCode());
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
