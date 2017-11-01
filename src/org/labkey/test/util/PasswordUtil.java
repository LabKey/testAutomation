/*
 * Copyright (c) 2005-2017 LabKey Corporation
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

import org.labkey.api.writer.PrintWriters;
import org.labkey.remoteapi.NetrcFileParser;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class PasswordUtil
{
    private static Credentials _cachedCredentials = null;

    private static class Credentials
    {
        private String _username;
        private String _password;
        public Credentials(String username, String password)
        {
            _username = username;
            _password = password;
        }
        public String getUsername()
        {
            return _username;
        }
        public String getPassword()
        {
            return _password;
        }
    }

    public static void main(String args[]) throws IOException
    {
        PasswordUtil util = new PasswordUtil();
        if (args.length == 0)
            util.outputUsage();
        else if ("set".equals(args[0]))
        {
            if (args.length == 3)
                setCredentials(args[1], args[2]);
            else
                setCredentials();
        }
        else if ("ensure".equals(args[0]))
            ensureCredentials();
        else if ("echo".equals(args[0]))
            echoCredentials();
        else if ("getSkipfishCredentials".equals(args[0]))
            getSkipfishCredentials();
        else
            util.outputUsage();
    }

    private void outputUsage()
    {
        System.out.println("Usage:");
        System.out.println("PasswordUtil set");
        System.out.println("  Prompts for and stores a password on this machine.");
        System.out.println("PasswordUtil ensure");
        System.out.println("  Checks for a stored password, and prompts if one does not exist.");
        System.out.println("PasswordUtil echo");
        System.out.println("  Echos the stored credentials.");
    }

    private static File getNetrcFile()
    {
        return new File(System.getProperty("user.home"), System.getProperty("os.name").toLowerCase().contains("win") ? "_netrc" : ".netrc");
    }

    private static String getHost()
    {
        String host = WebTestHelper.getTargetServer();
        return host.replaceFirst("https?://", "");
    }
    
    public static void setCredentials() throws IOException
    {
        int c;
        StringBuilder username = new StringBuilder();
        System.out.println("Enter your username (e.g. user@domain.org): ");
        while ((c = System.in.read()) != '\n')
        {
            if (c != '\r')
            {
                username.append((char) c);
            }
        }
        StringBuilder password = new StringBuilder();
        System.out.println("Enter your password: ");
        while ((c = System.in.read()) != '\n')
        {
            if (c != '\r')
            {
                password.append((char) c);
            }
        }
        setCredentials(username.toString(), password.toString());
    }

    public static void setCredentials(String username, String password) throws IOException
    {
        _cachedCredentials = null;

        try
        {
            File netrcFile = getNetrcFile();

            if(new NetrcFileParser().getEntry(netrcFile, getHost()) != null)
            {
                System.err.println("netrc file already has an entry for " + getHost() + ". Please delete entry and retry or update it manually");
                return;
            }

            boolean alreadyExists = netrcFile.exists();

            try (PrintWriter pw = PrintWriters.getPrintWriter(new FileOutputStream(netrcFile, alreadyExists)))
            {
                if (alreadyExists)
                    pw.append("\n");
                pw.append("machine ");
                pw.append(getHost());
                pw.append("\nlogin ");
                pw.append(username);
                pw.append("\npassword ");
                pw.append(password);
                pw.append('\n');
            }
        }
        catch (IOException ioe)
        {
            System.err.println("failed trying to create a .netrc file " + ioe.getMessage());
        }
    }

    public static void echoCredentials()
    {
        Credentials credentials = getCredentials();
        if (credentials == null)
        {
            System.out.println("No stored credentials");
        }
        else
        {
            System.out.println("Username: " + credentials.getUsername());
            System.out.println("Password: " + credentials.getPassword());
        }
    }

    public static void getSkipfishCredentials()
    {
        Credentials credentials = getCredentials();
        if (credentials == null)
        {
            System.setProperty("skipfishNoCredentials", "true");
        }
        else
        {
            System.out.println(credentials.getUsername() + ":" + credentials.getPassword());
        }
    }

    private static Credentials getCredentials()
    {
        if (_cachedCredentials == null)
        {
            try
            {
                NetrcFileParser.NetrcEntry entry = ensureCredentials();
                _cachedCredentials = new Credentials(entry.getLogin(), entry.getPassword());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load credentials", e);
            }
        }
        return _cachedCredentials;
    }

    public static String getUsername()
    {
        return getCredentials().getUsername();
    }

    public static String getPassword()
    {
        return getCredentials().getPassword();
    }

    public static NetrcFileParser.NetrcEntry ensureCredentials() throws IOException
    {
        File file = getNetrcFile();
        try
        {
            if (file.exists())
            {
                NetrcFileParser parser = new NetrcFileParser();
                NetrcFileParser.NetrcEntry entry = parser.getEntry(getNetrcFile(), getHost());
                if (entry != null)
                    return entry;
                throw new IOException("Credentials for " + getHost() + " not found in " + getNetrcFile());
            }
            throw new FileNotFoundException("Credentials have not been saved for this server.");
        }
        catch (IOException e)
        {
            System.out.flush(); // Make sure log is readable
            throw new IOException("Run the command 'gradlew :server:test:setPassword'.", e);
        }
    }
}
