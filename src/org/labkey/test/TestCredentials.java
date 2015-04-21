package org.labkey.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.labkey.test.credentials.Credentials;
import org.labkey.test.credentials.Server;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestCredentials
{
    private static Map<String, Server> credentials;
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

    public static String getUsername(String serverKey)
    {

        return credentials.get(serverKey).getLogin().getUsername();
    }

    public static String getPassword(String serverKey)
    {

        return credentials.get(serverKey).getLogin().getPassword();
    }

    public static String getCASServer()
    {
        return getHost("CAS");
    }

    public static String getCASUserID()
    {
        return getUsername("CAS");
    }

    public static String getCASUserPassword()
    {
        return getPassword("CAS");
    }
}
