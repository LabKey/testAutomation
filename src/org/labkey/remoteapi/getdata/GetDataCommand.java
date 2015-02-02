/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.remoteapi.getdata;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.labkey.remoteapi.PostCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Minimal wrapper for VisualizationController.GetDataAction usage. Grabs pre-defined JSON to send as the POST body.
 *
 * User: jeckels
 * Date: 4/16/14
 */
public class GetDataCommand extends PostCommand<GetDataResponse>
{
    private final JSONObject _payload;

    public GetDataCommand(InputStream inputJSON) throws IOException, ParseException
    {
        super("visualization", "getData");
        JSONParser parser = new JSONParser();
        _payload = (JSONObject)parser.parse(new InputStreamReader(inputJSON, StandardCharsets.UTF_8));
        inputJSON.close();
    }

    @Override
    protected GetDataResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new GetDataResponse(text, status, contentType, json, this);
    }

    @Override
    public JSONObject getJsonObject()
    {
        return _payload;
    }
}
