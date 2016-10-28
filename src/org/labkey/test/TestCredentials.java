/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
import org.labkey.api.util.FileUtil;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.credentials.Credentials;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestCredentials
{
    private static File credentialsFile;
    private static Map<String, Server> credentials;

    public static void setCredentialsFile(File credentialsFile)
    {
        TestCredentials.credentialsFile = credentialsFile;
        credentials = null;

        if (!credentialsFile.exists())
        {
            String error = String.format("Unable to load test credentials [%s]. Use 'server/test/test.credentials.json.template' as a basis and/or specify credentials file with test.credentials.file property.", credentialsFile.getAbsolutePath());
            TestCredentials.credentialsFile = null;
            throw new IllegalArgumentException(error);
        }
    }

    public static File getCredentialsFile()
    {
        if (null == credentialsFile)
        {
            setCredentialsFile(FileUtil.getAbsoluteCaseSensitiveFile(new File(System.getProperty("test.credentials.file", TestFileUtils.getLabKeyRoot() + "/server/test/test.credentials.json"))));
        }
        return credentialsFile;
    }

    private static Map<String, Server> getCredentials() throws IOException
    {
        if (null == credentials)
        {
            ObjectMapper mapper = new ObjectMapper();
            Credentials parsedOutput = mapper.readValue(getCredentialsFile(), Credentials.class);
            credentials = new CaseInsensitiveHashMap<>(parsedOutput.getCredentials());
        }
        return credentials;
    }

    public static boolean hasCredentials(String serverKey)
    {
        try
        {
            return getCredentials().containsKey(serverKey);
        }
        catch (IOException | IllegalArgumentException ignore) {}

        return false;
    }

    public static Server getServer(String serverKey) throws IOException
    {
        Server server = getCredentials().get(serverKey);
        if (null == server)
        {
            throw new IllegalArgumentException(String.format("No server named '%s' found in credentials file. [%s]", serverKey, getCredentialsFile().getAbsolutePath()));
        }
        return server;
    }
}
