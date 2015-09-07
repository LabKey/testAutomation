<%
/*
 * Copyright (c) 2008-2015 LabKey Corporation
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
<%@ page import="com.dumbster.smtp.SmtpMessage" %>
<%@ page import="org.apache.commons.lang3.ArrayUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="org.labkey.dumbster.DumbsterController" %>
<%@ page import="org.labkey.dumbster.view.MailPage" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="org.labkey.api.util.MailHelper" %>
<%@ page import="org.labkey.dumbster.model.DumbsterManager" %>
<%@ page import="java.util.Set" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromPath("Ext4"));
        return resources;
    }
%>
<%
    JspView<MailPage> me = (JspView<MailPage>) HttpView.currentView();
    MailPage pageInfo = me.getModelBean();
    Container c = getContainer();
    SmtpMessage[] messages = pageInfo.getMessages();
    if ("true".equals(request.getParameter("reverse"))) ArrayUtils.reverse(messages);
    boolean recorder = pageInfo.isEnableRecorder();

    %><p id="emailRecordError" class="labkey-error" style="display: none;">&nbsp;</p><%

%>
<script type="text/javascript">
function toggleBody(id)
{
    var el = Ext4.get(id);
    if (el)
        el.setDisplayed(el.isDisplayed() ? "none" : "");
}

function toggleRecorder(checkbox)
{
    var checked = checkbox.checked;

    var showError = function(show, message)
    {
        var el = Ext4.get("emailRecordError");
        if (el)
        {
            if (message)
                el.update(message);
            el.setDisplayed(show ? "" : "none");
        }
    };

    var onUpdateFailure = function(response, error)
    {
        if (!error)
            error = "Failed to update email recorder status.";
        showError(true, error);

        // Reset to its initial value.
        checkbox.checked = !checked;
    };

    var onUpdateSuccess = function(response)
    {
        var json;
        var contentType = response.getResponseHeader('Content-Type');
        if (contentType && contentType.indexOf('application/json') >= 0)
            json = Ext4.decode(response.responseText);
        if (json && json.error)
            onUpdateFailure(response, json.error);
        else
        {
            showError(false);

            if (checked)
            {
                var t = document.getElementById("dataregion_EmailRecord");
                var len = t.rows.length;
                for (var i = len - 2; i > 0; i--)
                    t.deleteRow(i);
                Ext4.get("emailRecordEmpty").setDisplayed("");
            }
        }
    };

    Ext4.Ajax.request({
        url : LABKEY.ActionURL.buildURL('dumbster', 'setRecordEmail') + '?record=' + checked,
        method : 'GET',
        success: onUpdateSuccess,
        failure: onUpdateFailure
    });
}
</script>
<!--Fake data region for ease of testing.-->
<table id="dataregion_EmailRecord" class="labkey-data-region labkey-show-borders">
    <colgroup><col width="120"/><col width="120"/><col width="125"/><col width="400"></colgroup>
    <!-- hidden TRs where the header region and message box would normally be in a real data region -->
    <tr style="display:none"><td colspan="5">&nbsp;</td></tr>
    <tr>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>To</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>From</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Date/Time</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Message</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Headers</div></td>
        <td colspan="2" class="labkey-column-header labkey-col-header-filter" align="center"><div>View</div></td>
    </tr>
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
            StringBuilder body = new StringBuilder();
            boolean sawHtml = false;
            boolean inHtml = false;
            boolean inMessageBody = false;

            StringBuilder headers = new StringBuilder();
            Iterator i = m.getHeaderNames();

            while (i.hasNext())
            {
                String header = (String)i.next();
                headers.append(h(header));
                headers.append(": ");
                headers.append(h(m.getHeaderValue(header)));
                headers.append("<br/>\n");
                if (header.equals("Content-Type") && m.getHeaderValue(header).indexOf("text/html") == 0)
                    sawHtml = true;
            }

            for (String line : lines)
            {
                if (line.indexOf("Content-Type: text/html") == 0)
                    sawHtml = true;
                else if (sawHtml && "".equals(line))
                    inHtml = true;    
                else if (line.indexOf("------=") == 0)
                    sawHtml = inHtml = false;
                else if (inHtml && line.trim().equals("</td></tr>"))
                    inMessageBody = false;

                if (inHtml && !inMessageBody)
                    body.append(line).append('\n');
                else
                    body.append(h(line)).append("<br>\n");

                if (inHtml && line.indexOf("id=\"message-body\"") > 0)
                    inMessageBody = true;
            }

            Set<String> contentTypes = MailHelper.getBodyPartContentTypes(DumbsterManager.convertToMimeMessage(m));

%>
            <td><%=h(m.getHeaderValue("To"))%></td>
            <td><%=h(m.getHeaderValue("From"))%></td>
            <td><%=formatDateTime(m.getCreatedTimestamp())%></td>
            <td><a onclick="toggleBody('email_body_<%=rowIndex%>'); return false;"><%=h(m.getHeaderValue("Subject"))%></a>
                <div id="email_body_<%=rowIndex%>" style="display: none;"><br><%=body%></div></td>
            <td><a onclick="toggleBody('email_headers_<%=rowIndex%>'); return false;">View headers</a>
                <div id="email_headers_<%=rowIndex%>" style="display: none;"><br><%=headers%></div></td>
            <td>
                <%=text(contentTypes.contains("text/html") ? "<a href=\"" + h(DumbsterController.getViewMessageURL(c, rowIndex - 1, "html")) + "\" target=\"_messageHtml\">HTML</a>" : "&nbsp;")%>
            </td>
            <td>
                <%=text(contentTypes.contains("text/plain") ? "<a href=\"" + h(DumbsterController.getViewMessageURL(c, rowIndex - 1, "text")) + "\" target=\"_messageText\">Text</a>" : "&nbsp;")%>
            </td>
        </tr>
<%
        }
    }
%>
    <tr id="emailRecordEmpty" style="display: <%=text(messages.length > 0 ? "none" : "")%>;"><td colspan="3">No email recorded.</td></tr>
</table>
<%
    if (getUser().isSiteAdmin())
    {
%>
        <input name="emailRecordOn" type="checkbox" onclick="toggleRecorder(this);"<%=checked(recorder)%>> Record email messages sent
<%
    }
%>
