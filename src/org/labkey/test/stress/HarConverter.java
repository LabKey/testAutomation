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
import java.util.List;
import java.util.Set;

public class HarConverter
{
    private static final Logger LOG = LogManager.getLogger(HarConverter.class);
    private static final Set<ControllerActionId> excludedActions = Set.of(new ControllerActionId("login", "whoami"));

    private final String inputParam;

    public HarConverter(String inputParam)
    {
        this.inputParam = inputParam;
    }

    public static void main(String[] args) throws IOException
    {
        final String inputParam = args.length == 0 ? "-" : args[0];
        final String outputFileName = args.length < 2
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
            String name = i + " " + new ControllerActionId(requests.get(i).getUrl());
            TestCaseType testCase = apiTests.addNewTest();
            testCase.setName(name);
            requests.get(i).populateTestCase(testCase);
        }
        return apiTestsDoc;
    }

    private InputStream getInputStream(String inputParam) throws FileNotFoundException
    {
        if ("-".equals(inputParam))
        {
            LOG.info("Reading HAR file from stdin");
            return System.in;
        }
        else
        {
            File inputFile = new File(inputParam);
            LOG.info("Reading HAR file from " + inputFile.getAbsolutePath());
            return new FileInputStream(inputFile);
        }
    }

    private static OutputStream getOutputStream(String outputParam) throws FileNotFoundException
    {
        File outputFile = new File(outputParam);
        LOG.info("Writing converted HAR file to " + outputFile.getAbsolutePath());
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
                if (filterHarEntry(entry))
                {
                    requests.add(new HarRequest(entry));
                }
            }
            return requests;
        }
    }

    private boolean filterHarEntry(JSONObject entry)
    {
        boolean include = true;
        String url = entry.getJSONObject("request").getString("url");
        try
        {
            try
            {
                ControllerActionId actionId = new ControllerActionId(url);
                if (excludedActions.contains(actionId) || StringUtils.isBlank(actionId.getAction()) || "app".equals(actionId.getAction()))
                {
                    include = false;
                }
            }
            catch (IllegalArgumentException ignore)
            {
                include = false;
            }
            return include;
        }
        finally
        {
            if (include)
                LOG.info("Including request to " + url);
            else
                LOG.info("Skipping request to " + url);
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
