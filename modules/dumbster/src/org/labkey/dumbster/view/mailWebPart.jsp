<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
%>
<%@ page import="org.labkey.dumbster.view.MailPage" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="com.dumbster.smtp.SmtpMessage" %>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<MailPage> me = (JspView<MailPage>) HttpView.currentView();
    ViewContext context = me.getViewContext();

    MailPage pageInfo = me.getModelBean();
    SmtpMessage[] messages = pageInfo.getMessages();
    boolean recorder = pageInfo.isEnableRecorder();
    boolean devmode = AppProps.getInstance().isDevMode();

    if (!devmode)
    {
        %><p class="labkey-error">Server must be in devmode to use the email recorder.</p><%
    }
    else
    {
        %><p id="emailRecordError" class="labkey-error" style="display: none;">Failed to update email recorder status.</p><%
    }
%>
<script type="text/javascript">
function toggleBody(id)
{
    var el = Ext.get(id);
    if (el)
        el.setDisplayed(el.isDisplayed() ? "none" : "");
}

function toggleRecorder(checkbox)
{
    var checked = checkbox.checked;

    var showError = function(show)
    {
        Ext.get("emailRecordError").setDisplayed(show ? "" : "none");
    }

    var onUpdateSuccess = function() // (response)
    {
        showError(false);

        if (checked)
        {
            var t = document.getElementById("mockregion_EmailRecord");
            var len = t.rows.length;
            for (var i = len - 1; i > 1; i--)
                t.deleteRow(i);
            Ext.get("emailRecordEmpty").setDisplayed("");
        }
    }

    var onUpdateFailure = function() // (response)
    {
        showError(true);
        
        // Reset to its initial value.
        checkbox.checked = !checked;
    }

    Ext.Ajax.request({
        url : LABKEY.ActionURL.buildURL('dumbster', 'setRecordEmail') + '?record=' + checked,
        method : 'GET',
        success: onUpdateSuccess,
        failure: onUpdateFailure
    });
}
</script>
<table id="mockregion_EmailRecord" class="labkey-data-region labkey-show-borders">
    <colgroup><col width="120"/><col width="120"/><col width="400"></colgroup>
    <thead><tr>
        <th class="labkey-col-header-filter" align="left">To</th>
        <th class="labkey-col-header-filter" align="left">From</th>
        <th class="labkey-col-header-filter" align="left">Message</th>
    </tr></thead>
    <tr id="emailRecordEmpty" style="display: <%=messages.length > 0 ? "none" : ""%>;"><td colspan="3">No email recorded.</td></tr>
    <%
    if (messages.length > 0)
    {
        int rowIndex = 0;
        for (SmtpMessage m : messages)
        {
            if (rowIndex++ % 2 == 0)
            {    %><tr class="labkey-alternate-row"><% }
            else
            {    %><tr class="labkey-row"><% }

            String[] lines = m.getBody().split("\n");
            StringBuffer body = new StringBuffer();
            boolean sawHtml = false;
            boolean inHtml = false;
            for (String line : lines)
            {
                if (inHtml)
                    body.append(line).append('\n');
                else
                    body.append(h(line)).append("<br>\n");

                if (line.indexOf("Content-Type: text/html") == 0)
                    sawHtml = true;
                else if (sawHtml && "".equals(line))
                    inHtml = true;    
                else if (line.indexOf("------=") == 0)
                    sawHtml = inHtml = false;
            }
%>
            <td><%=h(m.getHeaderValue("To"))%></td><td><%=h(m.getHeaderValue("From"))%></td>
            <td><a href="javascript:toggleBody(email_body_<%=rowIndex%>)"><%=h(m.getHeaderValue("Subject"))%></a>
                <div id="email_body_<%=rowIndex%>" style="display: none;"><br><%=body%></div></td></tr>
<%
        }
    }
%>
</table>
<%
    if (devmode && context.getUser().isAdministrator())
    {
%>
        <input type="checkbox" onclick="toggleRecorder(this)" <%=recorder ? "checked" : ""%>> Record email messages sent
<%
    }
%>
