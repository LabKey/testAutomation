/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.dumbster;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.Table;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class EmailRecordTable extends Table
{
    private static final String RECORDER_CHECKBOX_NAME = "emailRecordOn";
    private static final String _regionName = "EmailRecord";
    private static final int _headerRows = 2;
    private static final int _footerRows = 1;

    public EmailRecordTable(WebDriver driver)
    {
        super(driver, new RefindingWebElement(emailLocator(), driver).withTimeout(WAIT_FOR_JAVASCRIPT));
        ((RefindingWebElement) getComponentElement()).withRefindListener(el -> clearElementCache());
    }

    public EmailRecordTable(BaseWebDriverTest test)
    {
        this(test.getDriver());
    }

    @Override
    protected Elements elementCache()
    {
        getComponentElement().isDisplayed();
        return super.elementCache();
    }

    public int getEmailCount()
    {
        //3 rows in the table do not contain email messages
        return getRowCount() - (_headerRows + _footerRows);
    }

    public void startRecording()
    {
        getWrapper().checkCheckbox(Locator.checkboxByName(RECORDER_CHECKBOX_NAME));
        clearElementCache();
    }

    public void stopRecording()
    {
        getWrapper().uncheckCheckbox(Locator.checkboxByName(RECORDER_CHECKBOX_NAME));
    }

    /**
     * Call this before getting the message if you want to capture the body of the message as well
     * @param subject the text of the subject that will appear in the message table
     */
    public void clickSubject(String subject)
    {
        getWrapper().click(Locator.linkWithText(subject));
    }

    public void clickSubjectAtIndex(String subject, int index)
    {
        getWrapper().click(Locator.linkWithText(subject).index(index));
    }

    public void clickSubjectTo(String subject, List<String> recipient)
    {
        int index;
        int rows = getRowCount();

        if (rows > 0)
        {
            for (int i = 0; i < rows; i++)
            {
                int colTo = getColumnIndex("To");
                int colMsg = getColumnIndex("Message");
                String to = getDataAsText(i, colTo);
                if(recipient.contains(to))
                {
                    if(getDataAsText(i, colMsg).contains(subject)){clickSubjectAtIndex(subject, i); return;}
                }
            }
            getWrapper().log("unable to find message with subject " + subject + "addressed to recipient " + recipient);
            return;
        }
        getWrapper().log("no rows in mail record table");
    }

    public void clickMessage(EmailMessage message)
    {
        clickSubject(message.getSubject());
    }

    public EmailMessage getMessage(String subjectPart)
    {
        return getMessageRegEx(".*" + Pattern.quote(subjectPart) + ".*");
    }

    public EmailMessage getMessageRegEx(String regExp)
    {
        int rows = getRowCount() - _footerRows;

        if (rows > 0)
        {
            int colTo      = getColumnIndex("To", _headerRows);
            int colFrom    = getColumnIndex("From", _headerRows);
            int colMessage = getColumnIndex("Message", _headerRows);
            for (int i = _headerRows + 1; i <= rows; i++)
            {
                String message = getDataAsText(i, colMessage);
                String[] lines = trimAll(StringUtils.split(message, "\n"));
                String subjectLine = lines[0];
                if (subjectLine.matches(regExp))
                {
                    EmailMessage em = new EmailMessage();
                    em.setFrom(trimAll(StringUtils.split(getDataAsText(i, colFrom), ',')));
                    String[] to = trimAll(StringUtils.split(getDataAsText(i, colTo), ','));
                    for (int j = 0; j < to.length; j++)
                    {
                        // Extract email from : "Display <display@labkey.test>"
                        Pattern pattern = Pattern.compile(".*<(.+)>");
                        Matcher matcher = pattern.matcher(to[j]);
                        if (matcher.find())
                            to[j] = matcher.group(1);
                    }
                    em.setTo(to);
                    em.setSubject(subjectLine);
                    em.setBody(StringUtils.join(lines, "\n", 1, lines.length));
                    return em;
                }
            }
        }

        return null;
    }

    public List<EmailMessage> getMessagesByHeaderAndText(String header, String text)
    {
        List<Integer> colsWithText = getTableIndexesWhereTextAppears(header, text);
        List<EmailMessage> messages = new ArrayList<>();
        for(Integer i : colsWithText)
        {
            messages.add(getEmailAtTableIndex(i));
        }
        return messages;
    }

    public List<Integer> getTableIndexesWhereTextAppears(String header, String text)
    {
        List<String> columnText = getColumnAsText(header, _headerRows);
        List<Integer> colsWithString = new ArrayList<>();
        for(int i = 1; i <= columnText.size(); i++)
        {
            int arrayIndex = i-1;
            if(columnText.get(arrayIndex).equals(text)){colsWithString.add(i + _headerRows);}
        }
        return colsWithString;
    }

    public EmailMessage getEmailAtTableIndex(int Index)
    {
        int colTo      = getColumnIndex("To", _headerRows);
        int colFrom    = getColumnIndex("From", _headerRows);
        int colMessage = getColumnIndex("Message", _headerRows);
        String message = getDataAsText(Index, colMessage);
        String[] lines = trimAll(StringUtils.split(message, "\n"));
        String subjectLine = lines[0];
        EmailMessage em = new EmailMessage();
        em.setFrom(trimAll(StringUtils.split(getDataAsText(Index, colFrom), ',')));
        String[] to = trimAll(StringUtils.split(getDataAsText(Index, colTo), ','));
        for (int j = 0; j < to.length; j++)
        {
            // Extract email from : "Display <display@labkey.test>"
            Pattern pattern = Pattern.compile(".*<(.+)>");
            Matcher matcher = pattern.matcher(to[j]);
            if (matcher.find())
                to[j] = matcher.group(1);
        }
        em.setTo(to);
        em.setSubject(subjectLine);
        em.setBody(StringUtils.join(lines, "\n", 1, lines.length));
        return em;
    }

    public List<String> getColumnDataAsText(String column)
    {
        return getColumnAsText(column , _headerRows);
    }

    public int getHeaderRowCount()
    {
        return _headerRows;
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

    private static Locator emailLocator()
    {
        return Locator.xpath("//table[@lk-region-name='"+ _regionName +"']");
    }
}
