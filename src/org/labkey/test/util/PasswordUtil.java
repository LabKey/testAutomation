/*
 * Copyright (c) 2005-2015 LabKey Corporation
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

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PasswordUtil
{
    private static final String PASSWORD_FILE_NAME = ".cpasDRTPassword";
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
        else if ("delete".equals(args[0]))
            deleteStoredPassword();
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
        System.out.println("PasswordUtil delete");
        System.out.println("  Deletes a stored password on this machine.");
        System.out.println("PasswordUtil set");
        System.out.println("  Prompts for and stores a password on this machine.");
        System.out.println("PasswordUtil ensure");
        System.out.println("  Checks for a stored password, and prompts if one does not exist.");
        System.out.println("PasswordUtil echo");
        System.out.println("  Echos the stored credentials.");
    }

    private static File verifyDir(String dirName)
    {
        if (dirName != null)
        {
            File dir = new File(dirName);
            if (dir.exists())
                return dir;
        }
        return null;
    }

    private static File findPasswordFile()
    {
        File dir = verifyDir(System.getProperty("user.home"));
        if (dir == null)
        {
            System.out.println("User home couldn't be found.  Using working directory instead.");
            dir = verifyDir(System.getProperty("user.dir"));
        }
        if (dir == null)
            throw new IllegalStateException("System property for user.home or user.dir must be set to enable password storage.");

        return new File(dir, PASSWORD_FILE_NAME);
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
        File file = deleteStoredPassword();
        StringBuilder credentials = new StringBuilder();
        credentials.append(username).append("\n").append(password);

        byte[] bytes = credentials.toString().getBytes(StandardCharsets.UTF_8);
        byte[] inverted = invertBytes(bytes, bytes.length);

        try (FileOutputStream ostream = new FileOutputStream(file))
        {
            ostream.write(inverted);
        }
    }

    public static File deleteStoredPassword()
    {
        File file = findPasswordFile();
        if (file != null && file.exists())
            file.delete();
        return file;
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
            File file = findPasswordFile();

            try (InputStream istream = new FileInputStream(file))
            {
                byte[] bytes = new byte[1024];
                int len = istream.read(bytes);
                String data = new String(invertBytes(bytes, len));
                String[] credentialString = data.split("\n");
                _cachedCredentials = new Credentials(credentialString[0], credentialString[1]);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
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

    private static byte[] invertBytes(byte[] data, int length)
    {
        byte[] inverted = new byte[length];
        for (int i = 0; i < length; i++)
            inverted[i] = (byte) (255 ^ data[i]);
        return inverted;
    }

    public static void ensureCredentials() throws IOException
    {
        File file = findPasswordFile();
        if (file == null || !file.exists())
        {
            System.err.println("Credentials have not been saved for the current user.");
            System.err.println("Do an 'ant setPassword' from the server/test directory.");
            System.exit(1);
        }
    }
}
