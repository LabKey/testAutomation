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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.Table;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class EmailRecordTable extends Table
{
    private static final String RECORDER_CHECKBOX_NAME = "emailRecordOn";
    private static final String _regionName = "EmailRecord";
    private static final Locator gridLocator = Locator.xpath("//table[@lk-region-name='"+ _regionName +"']");
    private static final int _headerRows = 2;
    private static final int _footerRows = 1;

    public EmailRecordTable(WebDriver driver)
    {
        super(driver, new RefindingWebElement(gridLocator, driver).withTimeout(WAIT_FOR_JAVASCRIPT));
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

    @Override
    public int getColumnIndex(String headerLabel)
    {
        try
        {
            return EmailColumn.valueOf(headerLabel).getIndex();
        }
        catch (IllegalArgumentException fallback)
        {
            return super.getColumnIndex(headerLabel, _headerRows);
        }
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

    public void clickMessage(EmailMessage message)
    {
        Locator.tag("a").findElement(getDataAsElement(message.getRowIndex(), EmailColumn.Message.getIndex())).click();
        parseMessageCell(message); // Get body from expanded row
    }

    public EmailMessage getMessageWithSubjectContaining(String subjectPart)
    {
        return getMessageRegEx(".*" + Pattern.quote(subjectPart) + ".*");
    }

    public EmailMessage getMessage(String subject)
    {
        return getMessage(actualSubject -> actualSubject.equals(subject));
    }

    public EmailMessage getMessageRegEx(String regExp)
    {
        return getMessage(subject -> subject.matches(regExp));
    }

    private EmailMessage getMessage(Predicate<String> subjectFilter)
    {
        int rows = getRowCount() - _footerRows;

        if (rows > 0)
        {
            int colMessage = getColumnIndex("Message");
            for (int i = _headerRows + 1; i <= rows; i++)
            {
                String message = getDataAsText(i, colMessage);
                String[] lines = trimAll(StringUtils.split(message, "\n"));
                String subjectLine = lines.length > 0 ? lines[0] : "";
                if (subjectFilter.test(subjectLine))
                {
                    return getEmailAtTableIndex(i);
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

    public EmailMessage getEmailAtTableIndex(int index)
    {
        int colTo      = getColumnIndex("To");
        int colFrom    = getColumnIndex("From");
        EmailMessage em = new EmailMessage(index);
        em.setFrom(getDataAsText(index, colFrom));
        String[] to = trimAll(StringUtils.split(getDataAsText(index, colTo), ','));
        for (int j = 0; j < to.length; j++)
        {
            // Extract email from : "Display <display@labkey.test>"
            Pattern pattern = Pattern.compile(".*<(.+)>");
            Matcher matcher = pattern.matcher(to[j]);
            if (matcher.find())
                to[j] = matcher.group(1);
        }
        em.setTo(to);
        parseMessageCell(em);
        parseViewCell(em);
        return em;
    }

    private void parseMessageCell(EmailMessage emailMessage)
    {
        int colMessage = getColumnIndex("Message");
        String message = getDataAsText(emailMessage.getRowIndex(), colMessage);
        String[] lines = trimAll(StringUtils.split(message, "\n"));
        String subjectLine = lines.length > 0 ? lines[0] : "";
        emailMessage.setSubject(subjectLine);
        emailMessage.setBody(StringUtils.join(lines, "\n", 1, lines.length));
    }

    private void parseViewCell(EmailMessage emailMessage)
    {
        String html = getDataAsText(emailMessage.getRowIndex(), EmailColumn.View_HTML.getIndex()).trim();
        String text = getDataAsText(emailMessage.getRowIndex(), EmailColumn.View_Text.getIndex()).trim();
        List<String> views = new ArrayList<>();
        if (!html.isEmpty())
            views.add(html);
        if (!text.isEmpty())
            views.add(text);
        emailMessage.setViews(views);
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
        private final int rowIndex;
        private String _from;
        private String[] _to;
        private String _subject;
        private String _body;
        private List<String> views;

        private EmailMessage(int rowIndex)
        {
            this.rowIndex = rowIndex;
        }

        private int getRowIndex()
        {
            return rowIndex;
        }

        public String getFrom()
        {
            return _from;
        }

        private void setFrom(String from)
        {
            _from = from;
        }

        public String[] getTo()
        {
            return _to;
        }

        private void setTo(String[] to)
        {
            _to = to;
        }

        public String getSubject()
        {
            return _subject;
        }

        private void setSubject(String subject)
        {
            _subject = subject;
        }

        public String getBody()
        {
            return _body;
        }

        private void setBody(String body)
        {
            _body = body;
        }

        public List<String> getViews()
        {
            return views;
        }

        private void setViews(List<String> views)
        {
            this.views = ImmutableList.copyOf(views);
        }
    }

    private enum EmailColumn
    {
        To(1),
        From(2),
        Time(3),
        Date(3),
        DateTime(3),
        Message(4),
        Headers(5),
        View_HTML(6),
        View_Text(7);

        private final int index;

        EmailColumn(int index)
        {
            this.index = index;
        }

        public int getIndex()
        {
            return index;
        }
    }
}
