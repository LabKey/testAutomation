/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.credentials.Credentials;
import org.labkey.test.credentials.Server;

import java.io.IOException;
import java.util.Map;

public class StressCredentialsUtils
{
    private static final String STRESS_CREDENTIALS = System.getProperty("webtest.stress.credentials");
    private static Map<String, Server> credentials;

    private static Map<String, Server> getCredentials() throws JsonProcessingException
    {
        if (null == credentials)
        {
            ObjectMapper mapper = new ObjectMapper();
            Credentials parsedOutput = mapper.readValue(STRESS_CREDENTIALS, Credentials.class);
            credentials = new CaseInsensitiveHashMap<>(parsedOutput.getCredentials());
        }
        return credentials;
    }

    public static Server getServer(String serverKey) throws IOException
    {
        Server server = getCredentials().get(serverKey);
        if (null == server)
        {
            throw new IllegalArgumentException(String.format("No server named '%s' found in stress credentials property. [%s]", serverKey, getCredentials().keySet()));
        }
        return server;
    }
}
