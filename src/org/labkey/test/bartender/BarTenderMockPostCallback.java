package org.labkey.test.bartender;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.TestFileUtils;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockserver.model.HttpResponse.response;

public class BarTenderMockPostCallback implements ExpectationResponseCallback
{
    public static final int SUCCESS = 200;
    public static final int FAILURE = 500;
    public static final int NOT_FOUND = 404;

    private static final String SUCCESS_RESPONSE_FILE = "successResponse.json";
    private static final String FAILURE_RESPONSE_FILE = "templateNotFound.json";

    private static final String NOT_FOUND_LABEL = "labelnotfound";
    private static final String SUCCESS_LABEL = "success";
    private static final String SERVICE_NOT_FOUND = "doesnotexist";

    private static final String FORMAT_SETUP_XPATH = "//Command/FormatSetup/Format";
    private static final String PRINT_SETUP_XPATH = "//Command/Print/Format";

    @Override
    public HttpResponse handle(HttpRequest httpRequest)
    {
        String path = httpRequest.getPath().getValue().trim();
        String body = httpRequest.getBodyAsString();

        if (httpRequest.getMethod().toString().equals("OPTIONS"))
            return response().withStatusCode(SUCCESS).withHeader("Access-Control-Allow-Origin", "*");


        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new StringReader(body)));

            // Parse the Label value from the request
            XPath xpath = XPathFactory.newInstance().newXPath();
            String label = StringUtils.trimToNull(xpath.evaluate(FORMAT_SETUP_XPATH, doc));
            if (label == null)
                label = xpath.evaluate(PRINT_SETUP_XPATH, doc);

            label = StringUtils.trimToNull(label);

            // Respond successfully to requests with blank labels. This isn't Ideal, but it simplifies the parsing of label value and needs to be allowed for the TestConnection scenario.
            if (label == null)
                return readFileResponse(SUCCESS_RESPONSE_FILE, "");

            switch (label)
            {
                // Return the label not found response body
                case NOT_FOUND_LABEL:
                    return readFileResponse(FAILURE_RESPONSE_FILE, label);

                // Respond with a 404 status code
                case SERVICE_NOT_FOUND:
                    return response("Not found").withStatusCode(NOT_FOUND);

                // Success or default value respond with the expected Successful print job body
                default:
                case SUCCESS_LABEL:
                    return readFileResponse(SUCCESS_RESPONSE_FILE, label);
            }
        }
        catch (Exception e)
        {
            return response("Failed to parse BTXML body").withStatusCode(FAILURE);
        }
    }

    private HttpResponse readFileResponse(String filename, @Nullable String label)
    {
        StringBuilder sb = new StringBuilder();
        Path filePath = TestFileUtils.getSampleData("/barTender/mockResponses/" + filename).toPath();
        try
        {
            Files.readAllLines(filePath).forEach(sb::append);
        }
        catch (IOException e)
        {
            return response(e.getMessage()).withStatusCode(FAILURE);
        }

        return response(String.format(sb.toString(), label)).withStatusCode(SUCCESS).withHeader("Access-Control-Allow-Origin", "*");
    }
}
