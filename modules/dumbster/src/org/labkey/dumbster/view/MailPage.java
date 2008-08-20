/*
 * Copyright (c) 2008 LabKey Corporation
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
package org.labkey.dumbster.view;

import com.dumbster.smtp.SmtpMessage;

/**
 * <code>MailPage</code>
 */
public class MailPage
{
    boolean enableRecorder;
    private SmtpMessage[] _messages;

    public boolean isEnableRecorder()
    {
        return enableRecorder;
    }

    public void setEnableRecorder(boolean enableRecorder)
    {
        this.enableRecorder = enableRecorder;
    }

    public SmtpMessage[] getMessages()
    {
        return _messages;
    }

    public void setMessages(SmtpMessage[] messageList)
    {
        _messages = messageList;
    }
}
