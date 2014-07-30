/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import static org.junit.Assert.*;

public class EmailRecordTable extends DataRegionTable
{
    private static final String RECORDER_CHECKBOX_NAME = "emailRecordOn";

    private boolean _recordOn;

    public EmailRecordTable(BaseWebDriverTest test)
    {
        super("EmailRecord", test, false, false);
    }

    @Override
    public int getDataRowCount()
    {
        // This mock data region always has a hidden row at the end.
        return super.getDataRowCount() - 1;
    }

    public void startRecording()
    {
        _test.checkCheckbox(Locator.checkboxByName(RECORDER_CHECKBOX_NAME));
    }

    public void stopRecording()
    {
        _test.uncheckCheckbox(Locator.checkboxByName(RECORDER_CHECKBOX_NAME));
    }

    public void saveRecorderState()
    {
        _recordOn = _test.isChecked(Locator.checkboxByName(RECORDER_CHECKBOX_NAME));
    }

    public void restoreRecorderState()
    {
        if (_recordOn)
            startRecording();
        else
            stopRecording();        
    }

    public void clearAndRecord()
    {
        // Make sure the email recorder is on (to avoid sending mail) and clear
        // the cache of previously recorded messages.
        stopRecording();
        startRecording();
        
        _test.sleep(1000);
        String error = _test.getText(Locator.id("emailRecordError"));
        assertTrue("Error setting email recorder: " + error, StringUtils.trimToNull(error) == null);
    }

    public void clickMessage(EmailMessage message)
    {
        _test.click(Locator.linkWithText(message.getSubject()));
        _test.sleep(100);   // Should be pretty quick
    }

    public EmailMessage getMessage(String subjectPart)
    {
        int rows = getDataRowCount();

        if (rows > 0)
        {
            int colTo      = getColumn("To");
            int colFrom    = getColumn("From");
            int colMessage = getColumn("Message");
            for (int i = 0; i < rows; i++)
            {
                String message = getDataAsText(i, colMessage);
                String[] lines = trimAll(StringUtils.split(message, "\n"));
                String subjectLine = lines[0];
                if (subjectLine.contains(subjectPart))
                {
                    EmailMessage em = new EmailMessage();
                    em.setFrom(trimAll(StringUtils.split(getDataAsText(i, colFrom), ',')));
                    em.setTo(trimAll(StringUtils.split(getDataAsText(i, colTo), ',')));
                    em.setSubject(subjectLine);
                    em.setBody(StringUtils.join(lines, "\n", 1, lines.length - 1));
                    return em;
                }
            }
        }

        return null;
    }

    private static String[] trimAll(String[] strings)
    {
        for (int i = 0; i < strings.length; i++)
            strings[i] = StringUtils.trim(strings[i]);
        return strings;
    }

    public static class EmailMessage
    {
        private String[] _from;
        private String[] _to;
        private String _subject;
        private String _body;

        public String[] getFrom()
        {
            return _from;
        }

        public void setFrom(String[] from)
        {
            _from = from;
        }

        public String[] getTo()
        {
            return _to;
        }

        public void setTo(String[] to)
        {
            _to = to;
        }

        public String getSubject()
        {
            return _subject;
        }

        public void setSubject(String subject)
        {
            _subject = subject;
        }

        public String getBody()
        {
            return _body;
        }

        public void setBody(String body)
        {
            _body = body;
        }
    }
}
