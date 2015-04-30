/*
 * Copyright (c) 2015 LabKey Corporation
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.labkey.test.credentials.Credentials;
import org.labkey.test.credentials.Login;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestCredentials
{
    private static final Map<String, Server> credentials;
    static
    {
        File credentialsFile = new File(System.getProperty("test.credentials.file", TestFileUtils.getLabKeyRoot() + "/server/test/test.credentials.json"));
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            Credentials parsedOutput = mapper.readValue(credentialsFile, Credentials.class);
            credentials = parsedOutput.getCredentials();
        }
        catch (IOException fail)
        {
            throw new RuntimeException("Unable to load test credentials", fail);
        }
    }

    public static String getHost(String serverKey)
    {
        return credentials.get(serverKey).getHost();
    }

    public static List<Login> getLogins(String serverKey)
    {
        return credentials.get(serverKey).getLogins();
    }
}
