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
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.Crawler.ControllerActionId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private final Map<String, String> _containerReplacements = new HashMap<>();

    public HarConverter(String inputParam)
    {
        this.inputParam = inputParam;
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

        ApiTestsDocument apiTestsDoc = new HarConverter(inputParam).doConversion();

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
                containerPath = "/" + containerPath; // Relative URLs will have a leading slash
                String replacementString = _containerReplacements.computeIfAbsent(containerPath, k -> "@@CONTAINER" + (_containerReplacements.isEmpty() ? "" : "_" + (_containerReplacements.size() + 1)) + "@@");
                String urlWithReplacementString = testCase.getUrl().replaceFirst("^" + Pattern.quote(containerPath), replacementString);
                testCase.setUrl(urlWithReplacementString);
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

            List<HarRequest> requests = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++)
            {
                JSONObject entry = entries.getJSONObject(i);
                if (shouldIncludeHarEntry(entry))
                {
                    requests.add(new HarRequest(entry));
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

    private boolean shouldIncludeHarEntry(JSONObject entry)
    {
        String url = entry.getJSONObject("request").getString("url");
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
            LOG.warn("Request doesn't target expected server ({}): {}", WebTestHelper.getBaseURL(), url);
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

        HarRequest(JSONObject harEntry)
        {
            JSONObject request = harEntry.getJSONObject("request");
            method = request.getString("method").toLowerCase();
            url = request.getString("url").substring(WebTestHelper.getBaseURL().length());
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
