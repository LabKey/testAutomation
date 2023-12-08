<%
/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.data.DataRegion" %>
<%@ page import="org.labkey.api.util.HtmlString" %>
<%@ page import="org.labkey.api.util.MailHelper" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.dumbster.DumbsterController" %>
<%@ page import="org.labkey.dumbster.model.DumbsterManager" %>
<%@ page import="org.labkey.dumbster.view.MailPage" %>
<%@ page import="javax.mail.MessagingException" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="static org.labkey.api.util.DOM.Attribute.*" %>
<%@ page import="static org.labkey.api.util.DOM.*" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext4");
    }
%>
<%
    JspView<MailPage> me = (JspView<MailPage>) HttpView.currentView();
    MailPage pageInfo = me.getModelBean();
    Container c = getContainer();
    SmtpMessage[] messages = pageInfo.getMessages();
    if ("true".equals(request.getParameter("reverse"))) ArrayUtils.reverse(messages);
    boolean recorder = pageInfo.isEnableRecorder();

    DataRegion emailRegion = new DataRegion();
    emailRegion.setName("EmailRecord");

    String renderId = "emailRecordEmpty-" + getRequestScopedUID();

    %><p id="emailRecordError" class="labkey-error" style="display: none;">&nbsp;</p><%

%>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
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
                var t = document.getElementById(<%=q(emailRegion.getDomId())%>);
                var len = t.rows.length;
                for (var i = len - 2; i > 0; i--)
                    t.deleteRow(i);
                Ext4.get(<%=q(renderId)%>).setDisplayed("");
            }
        }
    };

    Ext4.Ajax.request({
        url : LABKEY.ActionURL.buildURL('dumbster', 'setRecordEmail') + '?record=' + checked,
        method : 'POST',
        success: onUpdateSuccess,
        failure: onUpdateFailure
    });
}
</script>
<!--Fake data region for ease of testing.-->
<table id=<%=q(emailRegion.getDomId())%> lk-region-name=<%=q(emailRegion.getName())%> class="labkey-data-region-legacy labkey-show-borders">
    <colgroup><col width="120"/><col width="120"/><col width="125"/><col width="400"></colgroup>
    <!-- hidden TRs where the header region and message box would normally be in a real data region -->
    <tr style="display:none"><td colspan="5">&nbsp;</td></tr>
    <tr>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>To</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>From</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Date/Time</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Message</div></td>
        <td class="labkey-column-header labkey-col-header-filter" align="left"><div>Headers</div></td>
        <td colspan="3" class="labkey-column-header labkey-col-header-filter" align="center"><div>View</div></td>
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


            StringBuilder headers = new StringBuilder();
            Iterator i = m.getHeaderNames();

            while (i.hasNext())
            {
                String header = (String)i.next();
                headers.append(h(header));
                headers.append(": ");
                headers.append(h(m.getHeaderValue(header)));
                headers.append("<br/>\n");
            }

            boolean hasHtml = false;
            boolean hasText = false;
            HtmlString body;
            try
            {
                MimeMessage mimeMessage = DumbsterManager.convertToMimeMessage(m);
                Map<String, String> map = MailHelper.getBodyParts(mimeMessage);

                hasHtml = map.get("text/html") != null;
                hasText = map.get("text/plain") != null;

                if (hasHtml)
                {
                    body = unsafe(map.get("text/html"));
                }
                else if (hasText)
                {
                    body = h(map.get("text/plain"));
                }
                else
                {
                    body = createHtml(DIV(cl("labkey-error"), "No message body"));
                }
            }
            catch (MessagingException e)
            {
                body = createHtml(DIV(cl("labkey-error"), "Error parsing email: " + e.getMessage()));
            }
%>
            <td><%=h(m.getHeaderValue("To"))%></td>
            <td><%=h(m.getHeaderValue("From"))%></td>
            <td><%=formatDateTime(m.getCreatedTimestamp())%></td>
            <%
                var idSubject = "subject_" + rowIndex;
                var idHeaders = "headers_" + rowIndex;
                addHandler(idSubject, "click", "toggleBody('email_body_" + rowIndex + "'); return false;");
                addHandler(idHeaders, "click", "toggleBody('email_headers_" + rowIndex + "'); return false;");
            %>
            <td><a id="<%=h(idSubject)%>"><%=h(m.getHeaderValue("Subject"))%></a>
                <div id="email_body_<%=rowIndex%>" style="display: none;"><hr><%=body%></div></td>
            <td><a id="<%=h(idHeaders)%>">View headers</a>
                <div id="email_headers_<%=rowIndex%>" style="display: none;"><hr><%=unsafe(headers.toString())%></div></td>
            <%=hasHtml ? createHtml(TD(A(at(href, DumbsterController.getViewMessageURL(c, rowIndex - 1, "html")).at(target, "_messageHtml"), "HTML"))) : createHtml(TD())%>
            <%=hasText ? createHtml(TD(A(at(href, DumbsterController.getViewMessageURL(c, rowIndex - 1, "text")).at(target, "_messageText"), "Text"))) : createHtml(TD())%>
            <%=createHtml(TD(A(at(href, DumbsterController.getViewMessageURL(c, rowIndex - 1, "raw")).at(target, "_messageText"), "Raw")))%>
        </tr>
<%
        }
    }
%>
    <tr id=<%=q(renderId)%> style="display: <%=unsafe(messages.length > 0 ? "none" : "")%>;"><td colspan="6">No email recorded.</td></tr>
</table>
<%
    if (getUser().hasRootAdminPermission())
    {
        addHandler("emailRecordOn", "click", "toggleRecorder(this);");
%>
        <input id="emailRecordOn" name="emailRecordOn" type="checkbox" <%=checked(recorder)%>> Record email messages sent
<%
    }
%>
