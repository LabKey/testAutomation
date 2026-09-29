/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.labkey.query.xml.ApiTestsDocument;
import org.labkey.query.xml.TestCaseType;
import org.labkey.test.util.Crawler.ControllerActionId;
import org.labkey.test.util.EscapeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This converts the JSON from a browser network recording (HAR file) into a {@link ApiTestsDocument} that can be
 * used to simulate UI activities via API.<br>
 * The relevant portions of the HAR file is the log.entries array:
 * <pre>{@code
 * {
 *   "log" : {
 *     "entries" : [
 *         // entries
 *     ]
 *   }
 * }
 * }</pre>
 *
 * Each HAR entry contains information about a single request. This tool will collect 'GET' and 'POST' requests to
 * LabKey actions (not source downloads). Each request is converted to a format that is understood by our API test
 * helpers ({@link org.labkey.test.util.APITestHelper} and {@link ApiTestCommand}).<br>
 * <br>
 * This is an example of a conversion of a 'POST' request with JSON data.<br>
 * HAR entry:
 * <pre>{@code
 * {
 *   "request": {
 *     "method": "post",
 *     "url": "http://localhost:8080/SMTestProject/query-getSelected.api",
 *     "postData": { // no postData for GET requests
 *       "mimeType": "application/json",
 *       "text": "{\"schemaName\":\"inventory\",\"queryName\":\"LocationCapacityByType\",\"query.selectionKey\":\"freezerListModel\",\"query.containerFilterName\":\"Current\",\"query.param.LocationTypeName\":\"Freezer\"}"
 *     }
 *   }
 * }
 * }</pre>
 * The resulting API test XML:
 * <pre>{@code
 * <test name="1 query-getSelected" type="post">
 *    <url>
 *        <![CDATA[@@CONTAINER@@/query-getSelected.api]]>
 *    </url>
 *    <formData>
 *      <![CDATA[{"schemaName":"inventory","queryName":"LocationCapacityByType","query.selectionKey":"freezerListModel","query.containerFilterName":"Current","query.param.LocationTypeName":"Freezer"}]]>
 *    </formData>
 * </test>
 * }</pre>
 */
public class HarConverter
{
    private static final Logger LOG = LogManager.getLogger(HarConverter.class);
    public static final Set<ControllerActionId> EXCLUDED_ACTIONS = Set.of(new ControllerActionId("login", "whoami"));

    private final String inputParam;
    private final String _baseUrlOverride;

    private final Map<String, String> _containerReplacements = new HashMap<>();

    public HarConverter(String inputParam)
    {
        this(inputParam, null);
    }

    /**
     * @param baseUrlOverride server root that recorded URLs are relative to, e.g. {@code https://example.com/labkey}.
     *                        Needed only when the deployment has a context path; otherwise it is read off the recording.
     */
    public HarConverter(String inputParam, String baseUrlOverride)
    {
        this.inputParam = inputParam;
        this._baseUrlOverride = baseUrlOverride;
    }

    /**
     * In order to produce replay files for stress testing, you must generate representative HAR files.<br>
     * <ol>
     *     <li>Initialize project to match test environment</li>
     *     <li>Create HAR file:<ol>
     *         <li>Start recording by opening browser developer tools and/or clearing network log</li>
     *         <li>Perform action(s) to be replayed during stress simulation</li>
     *         <li>Export network log to HAR file</li>
     *         <li>Repeat for other activities</li>
     *     </ol></li>
     *     <li>Convert HAR files to ApiTest XML files
     *         <pre>./gradlew :server:testAutomation:convertHarToStressXml -PharInFile=/path/to/some.har [-PharOutFile=/path/to/output.xml]</pre>
     *     </li>
     * </ol>
     */
    public static void main(String[] args) throws IOException
    {
        final String inputParam = args.length == 0 ? null : args[0];
        final String outputFileName = args.length == 1
            ? (inputParam.length() > 1 ? inputParam.replaceFirst("(.har)?$", ".xml") : "har.xml")
            : args[1];

        ApiTestsDocument apiTestsDoc = new HarConverter(inputParam, args.length > 2 ? args[2] : null).doConversion();

        try (OutputStream outputStream = getOutputStream(outputFileName))
        {
            XmlOptions opts = new XmlOptions();
            opts.setSaveCDataEntityCountThreshold(0);
            opts.setSaveCDataLengthThreshold(0);
            opts.setSavePrettyPrint();
            opts.setUseDefaultNamespace();
            opts.setSaveNoXmlDecl();
            apiTestsDoc.save(outputStream, opts);
        }
    }

    public ApiTestsDocument doConversion() throws IOException
    {
        List<HarRequest> requests = readRequestsFromHar();

        ApiTestsDocument apiTestsDoc = ApiTestsDocument.Factory.newInstance();
        ApiTestsDocument.ApiTests apiTests = apiTestsDoc.addNewApiTests();

        for (int i = 0; i < requests.size(); i++)
        {
            ControllerActionId actionId = new ControllerActionId(requests.get(i).getUrl());
            TestCaseType testCase = apiTests.addNewTest();
            testCase.setName(i + " " + actionId);
            requests.get(i).populateTestCase(testCase);
            String containerPath = actionId.getContainerPath();
            if (containerPath != null && !containerPath.isBlank())
            {
                // getContainerPath() decodes, so 'My Project' has to be matched against the '/My%20Project' in the URL
                String encodedPrefix = encodedContainerPrefix(testCase.getUrl(), containerPath);
                if (encodedPrefix == null)
                {
                    LOG.warn("Could not locate container '{}' within '{}'; leaving it hard-coded", containerPath, testCase.getUrl());
                }
                else
                {
                    String replacementString = _containerReplacements.computeIfAbsent("/" + containerPath,
                            k -> "@@CONTAINER" + (_containerReplacements.isEmpty() ? "" : "_" + (_containerReplacements.size() + 1)) + "@@");
                    testCase.setUrl(testCase.getUrl().replaceFirst("^" + Pattern.quote(encodedPrefix), replacementString));
                }
            }
        }
        if (!_containerReplacements.isEmpty())
        {
            LOG.info("Use the following containerPath replacements for these requests:");
            for (Map.Entry<String, String> entry : _containerReplacements.entrySet())
            {
                LOG.info("    '{}' => '{}'", entry.getValue(), entry.getKey());
            }
        }
        return apiTestsDoc;
    }

    public Map<String, String> getContainerReplacements()
    {
        return _containerReplacements;
    }

    /**
     * The leading portion of {@code url} that encodes {@code containerPath}, or null if no path segment boundary
     * decodes to it.
     */
    private static String encodedContainerPrefix(String url, String containerPath)
    {
        String target = "/" + containerPath;
        String path = url.split("\\?", 2)[0];
        for (int slash = path.indexOf('/', 1); slash >= 0; slash = path.indexOf('/', slash + 1))
        {
            String candidate = path.substring(0, slash);
            if (EscapeUtil.decode(candidate).equals(target))
            {
                return candidate;
            }
        }
        return null;
    }

    private InputStream getInputStream(String inputParam) throws FileNotFoundException
    {
        File inputFile = new File(inputParam);
        LOG.info("Reading HAR file from {}", inputFile.getAbsolutePath());
        return new FileInputStream(inputFile);
    }

    private static OutputStream getOutputStream(String outputParam) throws FileNotFoundException
    {
        File outputFile = new File(outputParam);
        if (outputFile.exists())
        {
            LOG.warn("Specified output file ({}) already exists. Not writing file.", outputFile.getAbsolutePath());
        }
        else
        {
            LOG.info("Writing converted HAR file to {}", outputFile.getAbsolutePath());
        }
        return new FileOutputStream(outputFile);
    }

    private List<HarRequest> readRequestsFromHar() throws IOException
    {
        try (InputStream inputStream = getInputStream(inputParam))
        {
            JSONTokener jsonTokener = new JSONTokener(inputStream);
            JSONObject harJson = new JSONObject(jsonTokener);
            JSONArray entries = harJson.getJSONObject("log").getJSONArray("entries");

            String baseUrl = _baseUrlOverride != null ? _baseUrlOverride : determineBaseUrl(entries);
            LOG.info("Treating '{}' as the server root", baseUrl);

            List<HarRequest> requests = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++)
            {
                JSONObject entry = entries.getJSONObject(i);
                String url = entry.getJSONObject("request").getString("url");
                if (!url.startsWith(baseUrl))
                {
                    LOG.warn("Skipping request to a different server: {}", url);
                    continue;
                }
                String relativeUrl = url.substring(baseUrl.length());
                if (shouldIncludeHarEntry(relativeUrl))
                {
                    requests.add(new HarRequest(entry, relativeUrl));
                }
            }

            if (requests.isEmpty())
            {
                throw new IllegalArgumentException("No requests included from har file: " + inputParam);
            }

            LOG.info("Including %d of %d entries from %s".formatted(requests.size(), entries.length(), inputParam));
            return requests;
        }
    }

    /**
     * The origin the recording was made against. HAR entries carry absolute URLs, so the recording itself is the only
     * reliable source: 'test.properties' describes whichever server the UI tests point at, which is rarely the server
     * that was recorded.
     */
    private static String determineBaseUrl(JSONArray entries)
    {
        Map<String, Integer> origins = new HashMap<>();
        for (int i = 0; i < entries.length(); i++)
        {
            URI uri = URI.create(entries.getJSONObject(i).getJSONObject("request").getString("url"));
            String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            origins.merge(uri.getScheme() + "://" + uri.getHost() + port, 1, Integer::sum);
        }
        if (origins.isEmpty())
        {
            throw new IllegalArgumentException("No requests in har file");
        }
        if (origins.size() > 1)
        {
            LOG.warn("Recording spans multiple servers {}; using the most frequent. Pass an explicit base URL to override.", origins.keySet());
        }
        return origins.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    private boolean shouldIncludeHarEntry(String url)
    {
        try
        {
            ControllerActionId actionId = new ControllerActionId(url);
            if (EXCLUDED_ACTIONS.contains(actionId) || StringUtils.isBlank(actionId.getAction()) || "app".equals(actionId.getAction()))
            {
                LOG.info("Skipping request: {}", url);
                return false;
            }
            else
            {
                LOG.info("Including request: {}", url);
                return true;
            }
        }
        catch (IllegalArgumentException ignore)
        {
            LOG.warn("Unparseable request URL: {}", url);
            return false;
        }
    }

    static class HarRequest
    {
        private final String method;
        private final String url;
        private final String postMime;
        private final String postText;
        private final int responseCode;

        HarRequest(JSONObject harEntry, String relativeUrl)
        {
            JSONObject request = harEntry.getJSONObject("request");
            method = request.getString("method").toLowerCase();
            url = relativeUrl;
            JSONObject postData = request.optJSONObject("postData", new JSONObject());
            postMime = postData.optString("mimeType");
            postText = postData.optString("text");
            responseCode = harEntry.getJSONObject("response").getInt("status");
        }

        public String getMethod()
        {
            return method;
        }

        public String getUrl()
        {
            return url;
        }

        public String getPostMime()
        {
            return postMime;
        }

        public String getPostText()
        {
            return postText;
        }

        public int getResponseCode()
        {
            return responseCode;
        }

        public TestCaseType populateTestCase(TestCaseType testCase)
        {
            testCase.setUrl(url);
            if ("get".equals(method))
            {
                testCase.setType("get");
            }
            else if ("post".equals(method))
            {
                if (ApiTestCommand.CONTENT_TYPE_JSON.equals(postMime))
                {
                    testCase.setType("post");
                    testCase.setFormData(new JSONObject(postText).toString(2));
                }
                else
                {
                    testCase.setType("post_form");
                    testCase.setFormData(postText);
                }
            }
            else
            {
                throw new IllegalStateException("Unhandled request method: " + method);
            }
            return testCase;
        }
    }
}
