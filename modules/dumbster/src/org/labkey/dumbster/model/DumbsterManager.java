/*
 * Copyright (c) 2008 LabKey Software Foundation
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
package org.labkey.dumbster.model;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>DumbsterManager</code>
 */
public class DumbsterManager
{
    static DumbsterManager instance;

    public static DumbsterManager get()
    {
        return instance;
    }

    public static void setInstance(DumbsterManager instance)
    {
        DumbsterManager.instance = instance;
    }

    SimpleSmtpServer _server;

    public void start()
    {
        _server = SimpleSmtpServer.start();
    }

    public void stop()
    {
        // Stop the server, if there is one, but leave it around for
        // viewing until the next call to start() overwrites.
        if (_server != null)
            _server.stop();
    }

    public SmtpMessage[] getMessages()
    {
        if (_server == null)
            return new SmtpMessage[0];

        List<SmtpMessage> messageList = new ArrayList<SmtpMessage>();

        // Dumbster returns iterator on list which requires synchronization.
        synchronized (_server)
        {
            Iterator it = _server.getReceivedEmail();
            while (it.hasNext())
            {
                messageList.add((SmtpMessage) it.next());
            }
        }

        // Reverse the list to put most recent at the top
        SmtpMessage[] messages = new SmtpMessage[messageList.size()];
        for (int i = 0; i < messages.length; i++)
            messages[i] = messageList.get(messages.length - i - 1);

        return messages;
    }
}
