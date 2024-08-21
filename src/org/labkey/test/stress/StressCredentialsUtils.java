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
