/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

    static SimpleHttpResponse readResponse(HttpURLConnection con) throws IOException
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
