package org.labkey.test.util;

import org.labkey.test.TestFileUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleHttpResponse
{
    private int responseCode;
    private String responseBody;
    private String responseMessage;
    private Map<String, List<String>> responseHeaderFields;

    private SimpleHttpResponse(){}

    static SimpleHttpResponse readResponse(HttpURLConnection con)
    {
        try
        {
            SimpleHttpResponse response = new SimpleHttpResponse();
            response.responseCode = con.getResponseCode();
            response.responseMessage = con.getResponseMessage();
            try
            {
                response.responseBody = TestFileUtils.getStreamContentsAsString(con.getInputStream());
            }
            catch (IOException error)
            {
                response.responseBody = TestFileUtils.getStreamContentsAsString(con.getErrorStream());
            }
            response.responseHeaderFields = new HashMap<>(con.getHeaderFields());

            return response;
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public int getResponseCode()
    {
        return responseCode;
    }

    public String getResponseBody()
    {
        return responseBody;
    }

    public String getResponseMessage()
    {
        return responseMessage;
    }

    public Map<String, List<String>> getResponseHeaderFields()
    {
        return responseHeaderFields;
    }
}
