/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.util.InstallCert;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.SimpleHttpRequest;
import org.labkey.test.util.SimpleHttpResponse;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.URLBuilder;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * Static methods for getting properties of and communicating with a running LabKey server
 */
public class WebTestHelper
{

    private static final Logger LOG = LogManager.getLogger(WebTestHelper.class);

    public static final Random RANDOM = new Random();
    public static final String API_KEY = "apikey"; // Username for api/session key authentication

    private static final String DEFAULT_CONTEXT_PATH = "";
    private static final Integer DEFAULT_WEB_PORT = 8080;
    private static final String DEFAULT_TARGET_SERVER = "http://localhost";
    private static final Object SERVER_LOCK = new Object();
    private static String _targetServer = null;
    private static Integer _webPort = null;
    private static String _contextPath = null;
    public static final int MAX_LEAK_LIMIT = 0;
    public static final int GC_ATTEMPT_LIMIT = 6;
    private static boolean USE_CONTAINER_RELATIVE_URL = true;
    private static final Map<String, Map<String, Cookie>> savedCookies = new HashMap<>();
    private static final Map<String, String> savedSessionKeys = new HashMap<>();

    public static void setUseContainerRelativeUrl(boolean useContainerRelativeUrl)
    {
        USE_CONTAINER_RELATIVE_URL = useContainerRelativeUrl;
    }

    /**
     * Save cookies to be used by HTTP requests
     */
    public static void saveSession(String user, WebDriver driver)
    {
        HashMap<String, Cookie> currentCookies = new HashMap<>();
        driver.manage().getCookies().forEach(c -> currentCookies.put(c.getName(), c));
        savedCookies.put(user, currentCookies);
    }

    public static Map<String, Cookie> getCookies(String user)
    {
        return savedCookies.getOrDefault(user, Collections.emptyMap());
    }

    public static String getSessionKey(String user)
    {
        Cookie sessionCookie = getCookies(user).get(Connection.JSESSIONID);
        if (sessionCookie == null)
        {
            throw new IllegalStateException("No saved session for user: " + user);
        }
        String sessionId = sessionCookie.getValue();

        if (!savedSessionKeys.containsKey(sessionId))
        {
            Connection connection = getRemoteApiConnection(user, true);
            PostCommand<?> command = new PostCommand<>("security", "createApiKey");
            JSONObject json = new JSONObject();
            json.put("type", "session");
            command.setJsonObject(json);

            try
            {
                CommandResponse response = command.execute(connection, "/");
                Object apikey = response.getParsedData().get("apikey");
                if (apikey == null)
                {
                    TestLogger.error(response.getText());
                    throw new RuntimeException("Failed to generate session key");
                }
                savedSessionKeys.put(sessionId, (String) apikey);
            }
            catch (CommandException | IOException e)
            {
                throw new RuntimeException("Unable to generate session key", e);
            }
        }
        return savedSessionKeys.get(sessionId);
    }

    public static boolean isUseContainerRelativeUrl()
    {
        return USE_CONTAINER_RELATIVE_URL;
    }

    private static void acceptLocalhostCert() throws Exception
    {
        String keystorePassword = System.getProperty("keystore.password", "changeit");
        InstallCert.install("localhost", _webPort, keystorePassword.toCharArray());
    }

    public static String getServerHost()
    {
        try
        {
            return new URL(getTargetServer()).getHost();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException("Target server property  is not a valid URL", e);
        }
    }

    public static boolean isLocalServer()
    {
        return isLocalHost(getServerHost());
    }

    private static boolean isLocalHost(String serverHost)
    {
        InetAddress addr;
        try
        {
            addr = InetAddress.getByName(serverHost);
        }
        catch (UnknownHostException e)
        {
            return false;
        }

        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try
        {
            return NetworkInterface.getByInetAddress(addr) != null;
        }
        catch (SocketException e)
        {
            return false;
        }
    }

    public static Integer getWebPort()
    {
        synchronized (SERVER_LOCK)
        {
            if (_webPort == null)
            {
                String webPortStr = System.getProperty("labkey.port");
                if (webPortStr == null || webPortStr.trim().length() == 0)
                {
                    LOG.info("Using default labkey port (" + DEFAULT_WEB_PORT +
                                        ").\nThis can be changed by setting the property 'labkey.port=[yourport]'.");
                    _webPort = DEFAULT_WEB_PORT;
                }
                else
                {
                    _webPort = Integer.parseInt(webPortStr);
                    LOG.info("Using labkey port '" + _webPort + "', as provided by system property 'labkey.port'.");
                }
            }
            return _webPort;
        }
    }

    public static String getTargetServer()
    {
        synchronized (SERVER_LOCK)
        {
            if (_targetServer == null || !_targetServer.equals(System.getProperty("labkey.server")))
            {
                _targetServer = System.getProperty("labkey.server");
                if (_targetServer == null || _targetServer.length() == 0)
                {
                    LOG.info("Using default target server (" + DEFAULT_TARGET_SERVER +
                                        ").\nThis can be changed by setting the property 'labkey.server=[yourserver]'.");
                    _targetServer = DEFAULT_TARGET_SERVER;
                }
                else
                    LOG.info("Using target server '" + _targetServer + "', as provided by system property 'labkey.server'.");
            }
            return _targetServer;
        }
    }

    public static String stripContextPath(String url)
    {
        String root = getContextPath() + "/";
        int rootLoc;
        if(root.length() == 1)
        {
            if(url.indexOf("//") > 0)
                rootLoc = url.indexOf(root, url.indexOf("//")+2);
            else
                rootLoc = -1;
        }
        else
        {
            rootLoc = url.indexOf(root);
        }

        int endOfAction = url.indexOf("?");
        if ((rootLoc != -1) && (endOfAction == -1 || rootLoc < endOfAction))
            url = url.substring(rootLoc + root.length());
        else if (url.indexOf("/") == 0)
            url = url.substring(1);
        return url;
    }

    public enum DatabaseType
    {
        PostgreSQL("org.postgresql.Driver", "pg", "postgres"),
        MicrosoftSQLServer("com.microsoft.sqlserver.jdbc.SQLServerDriver", "net.sourceforge.jtds.jdbc.Driver", "mssql", "sqlserver");

        private static final Map<String, DatabaseType> DATABASE_TYPE_MAP;

        static
        {
            Map<String, DatabaseType> tempMap = new HashMap<>();
            Arrays.stream(values())
                .forEach(dt -> Arrays.stream(dt._typeKeys)
                    .forEach(name -> tempMap.put(name, dt)));
            DATABASE_TYPE_MAP = Collections.unmodifiableMap(tempMap);
        }

        private final String[] _typeKeys;

        DatabaseType(String... typeKeys)
        {
            _typeKeys = typeKeys;
        }

        static @Nullable DatabaseType get(String driverClassName)
        {
            return DATABASE_TYPE_MAP.get(driverClassName);
        }
    }

    public static DatabaseType getDatabaseType()
    {
        String typeKey = getServerProperty("databaseType");
        if (StringUtils.isBlank(typeKey))
        {
            // Use driver class name from 'config.properties' if 'databaseType' isn't specified
            typeKey = getServerProperty("jdbcDriverClassName");
        }

        if (StringUtils.isBlank(typeKey))
        {
            throw new IllegalStateException("Can't determine database type: Neither 'jdbcDriverClassName' nor 'databaseType' property is not set");
        }

        DatabaseType dt = DatabaseType.get(typeKey);

        if (null == dt)
            throw new IllegalStateException("Unknown database type: " + typeKey);

        return dt;
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

    public static String getBaseUrlWithoutContextPath()
    {
        int defaultPort = getTargetServer().startsWith("https") ? 443 : 80;
        String portPortion = defaultPort == getWebPort() ? "" : ":" + getWebPort();

        return getTargetServer() + portPortion;
    }

    public static String getBaseURL()
    {
        return getBaseUrlWithoutContextPath() + getContextPath();
    }

    public static String getContextPath()
    {
        synchronized (SERVER_LOCK)
        {
            if (_contextPath == null)
            {
                _contextPath = System.getProperty("labkey.contextpath");
                if (_contextPath == null)
                {
                    LOG.info("Using default labkey context path (" + DEFAULT_CONTEXT_PATH +
                                        ").\nThis can be changed by setting the property 'labkey.contextpath=[yourpath]'.");
                    _contextPath = DEFAULT_CONTEXT_PATH;
                }
                else
                    LOG.info("Using labkey context path '" + _contextPath + "', as provided by system property 'labkey.contextPath'.");

                if ("/".equals(_contextPath))
                {
                    _contextPath = "";
                }
            }
            return _contextPath;
        }
    }

    public static String buildURL(String controller, String action)
    {
        return buildURL(controller, null, action, Collections.emptyMap());
    }

    public static String buildURL(String controller, String action, @Nullable Map<String, ?> params)
    {
        return buildURL(controller, null, action, params);
    }

    public static String buildURL(String controller, @Nullable String containerPath, String action)
    {
        return buildURL(controller, containerPath, action, Collections.emptyMap());
    }

    public static String buildURL(String controller, @Nullable String containerPath, String action, Map<String, ?> params)
    {
        return getBaseURL() + buildRelativeUrl(controller, containerPath, action, params);
    }

    public static String buildRelativeUrl(String controller, String action)
    {
        return buildRelativeUrl(controller, null, action, Collections.emptyMap());
    }

    public static String buildRelativeUrl(String controller, String action, @Nullable Map<String, ?> params)
    {
        return buildRelativeUrl(controller, null, action, params);
    }

    public static String buildRelativeUrl(String controller, @Nullable String containerPath, String action)
    {
        return buildRelativeUrl(controller, containerPath, action, Collections.emptyMap());
    }

    public static String buildRelativeUrl(String controller, @Nullable String containerPath, String action, Map<String, ?> params)
    {
        URLBuilder builder = new URLBuilder(controller, action, containerPath);
        builder.setQuery(params);
        return builder.buildRelativeURL();
    }

    public static Map<String, String> parseUrlQuery(URL url)
    {
        if (url != null)
            return parseUrlQueryString(url.getQuery());

        return Collections.emptyMap();
    }

    public static Map<String, String> parseUrlQueryString(String query)
    {
        if (query != null)
        {
            if (query.startsWith("?"))
                query = query.substring(1);
            String[] queryArgs = query.split("&");

            Map<String, String> parsedQuery = new HashMap<>(queryArgs.length);

            for (String arg : queryArgs)
            {
                String[] split = arg.split("=", 2);
                parsedQuery.put(split[0], split.length > 1 ? split[1] : null);
            }

            return parsedQuery;
        }

        return Collections.emptyMap();
    }

    public static Connection getRemoteApiConnection()
    {
        return getRemoteApiConnection(true);
    }

    public static Connection getRemoteApiConnection(boolean includeCookiesFromPrimaryUser)
    {
        String username = PasswordUtil.getUsername();
        Connection connection = new Connection(getBaseURL(), username, PasswordUtil.getPassword());

        if (includeCookiesFromPrimaryUser)
            addCachedCookies(connection, username);

        return connection;
    }

    public static Connection getRemoteApiConnection(String username, boolean includeCookies)
    {
        Connection connection = new Connection(getBaseURL(), username, PasswordUtil.getPassword());

        if (includeCookies)
            addCachedCookies(connection, username);

        return connection;
    }

    private static void addCachedCookies(Connection connection, String username)
    {
        for (Cookie cookie : getCookies(username).values())
        {
            connection.addCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getExpiry(), cookie.isSecure());
        }
    }

    public static void logToServer(String message)
    {
        logToServer(message, getRemoteApiConnection());
    }

    // Writes message to the labkey server log. Message parameter is output as sent
    public static void logToServer(String message, Connection connection)
    {
        if (message.contains("\n"))
        {
            String [] splitMessage = message.split("\n");
            for (String thisMessage: splitMessage)
            {
                if (thisMessage.length() > 0)
                    logToServer(thisMessage, connection);
            }
            return;
        }

        PostCommand<?> command = new PostCommand<>("admin", "log");
        Map<String, Object> params = new HashMap<>();
        params.put("message", message);
        command.setParameters(params);
        try
        {
            command.execute(connection, "/");
        }
        catch (IOException e)
        {
            TestLogger.log("Unable to log message to server: " + e.getMessage());
        }
        catch (CommandException e)
        {
            TestLogger.log("Unable to log message to server: " + e.getStatusCode());
        }
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
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setSocketTimeout(60000)
                .setConnectTimeout(60000)
                .setConnectionRequestTimeout(60000);
        return getHttpClientBuilder(username, password, requestConfigBuilder);
    }

    public static HttpClientBuilder getHttpClientBuilder(String username, String password, RequestConfig.Builder requestConfigBuilder)
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

        RequestConfig requestConfig = requestConfigBuilder
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setCharset(StandardCharsets.UTF_8)
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

        Map<String, Cookie> cookies = getCookies(username);
        if (!cookies.isEmpty())
        {
            CookieStore cookieStore = new BasicCookieStore();
            cookies.values().forEach(c -> cookieStore.addCookie(seleniumCookieToApacheCookie(c)));
            clientBuilder.setDefaultCookieStore(cookieStore);
        }

        return clientBuilder;
    }

    @NotNull
    private static org.apache.http.cookie.Cookie seleniumCookieToApacheCookie(Cookie c)
    {
        return new org.apache.http.cookie.Cookie()
        {
            @Override
            public String getName()
            {
                return c.getName();
            }

            @Override
            public String getValue()
            {
                return c.getValue();
            }

            @Override
            public String getComment()
            {
                return c.toString();
            }

            @Override
            public String getCommentURL()
            {
                return null;
            }

            @Override
            public Date getExpiryDate()
            {
                return c.getExpiry();
            }

            @Override
            public boolean isPersistent()
            {
                return false;
            }

            @Override
            public String getDomain()
            {
                return c.getDomain();
            }

            @Override
            public String getPath()
            {
                return c.getPath();
            }

            @Override
            public int[] getPorts()
            {
                return new int[0];
            }

            @Override
            public boolean isSecure()
            {
                return c.isSecure();
            }

            @Override
            public int getVersion()
            {
                return 0;
            }

            @Override
            public boolean isExpired(Date date)
            {
                return getExpiryDate() != null && date.compareTo(getExpiryDate()) > 0;
            }
        };
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

    /**
     * Get response to GET using default username:password and cached session
     * @param url Absolute URL or relative LabKey URL
     */
    public static SimpleHttpResponse getHttpResponse(String url)
    {
        return getHttpResponse(url, "GET", PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    /**
     * Get response to request using default username:password and cached session
     * @param url Absolute URL or relative LabKey URL
     * @param requestMethod e.g. "GET" or "POST"
     */
    public static SimpleHttpResponse getHttpResponse(String url, String requestMethod)
    {
        return getHttpResponse(url, requestMethod, PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    /**
     * Get response to GET using provided username:password and no session
     * @param url Absolute URL or relative LabKey URL
     */
    public static SimpleHttpResponse getHttpResponse(String url, String username, String password)
    {
        return getHttpResponse(url, "GET", username, password);
    }

    /**
     * Get response to request using provided username:password and no session
     * @param url Absolute URL or relative LabKey URL
     * @param requestMethod e.g. "GET" or "POST"
     */
    public static SimpleHttpResponse getHttpResponse(String url, String requestMethod, String username, String password)
    {
        return getHttpResponse(url, requestMethod, username, password, savedCookies.get(username));
    }

    /**
     * @param url Absolute URL or relative LabKey URL
     * @param requestMethod e.g. "GET" or "POST"
     * @param cookies Provided by {@link WebDriver.Options#getCookies()}
     */
    @LogMethod(quiet = true)
    public static SimpleHttpResponse getHttpResponse(@LoggedParam String url, String requestMethod, String username, String password, @Nullable Map<String, Cookie> cookies)
    {
        if (url.startsWith("/"))
            url = getBaseURL() + url;

        SimpleHttpRequest request = new SimpleHttpRequest(url);
        request.setRequestMethod(requestMethod);
        request.setLogin(username, password);
        if (cookies != null)
            request.setCookies(cookies.values());
        try
        {
            return request.getResponse();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
}
