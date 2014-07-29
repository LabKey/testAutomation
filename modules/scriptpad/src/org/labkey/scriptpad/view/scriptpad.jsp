<%
/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependency"%>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!

    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("Ext4"));
        return resources;
    }
%>
<div id='divForm'>
</div>
<p></p>
<div id='divOutput'>
</div>
<script type="text/javascript">
    var _sessionId;

    function preparePage()
    {
        document.getElementById('divOutput').innerHTML = "";
        maskGraph.show();
    }

    function renderFinal(htmlOut)
    {
        document.getElementById('divOutput').innerHTML = htmlOut;
    }

    function renderImage(type, title, image)
    {
        var htmlOut = "<table class=\"labkey-output\"> ";
        htmlOut += renderTitle(type, title);
        htmlOut += "<tr style=\"display:none\"><td>";
        htmlOut += "<img id=\"resultImage\" src=\"";
        htmlOut += image;
        htmlOut += "\"></td></tr></table>";
        return htmlOut;
    }

    function renderText(type, title, text)
    {
        var htmlOut = "<table class=\"labkey-output\"> ";
        htmlOut += renderTitle(type, title);
        htmlOut += "<tr style=\"display:none\"><td><pre>";
        htmlOut += text;
        htmlOut += "<pre></td></tr></table>";
        return htmlOut;
    }

    function renderDownload(type, title, text)
    {
        var htmlOut = "<table class=\"labkey-output\"> ";
        htmlOut += renderTitle(type, title);
        htmlOut += "<tr style=\"display:none\"><td>";
        if (text)
        {
            htmlOut += "<a href=\"" + text + "\">";
            htmlOut += "output file (click to download)</a>";
        }
        htmlOut += "</td></tr></table>";
        return htmlOut;
    }

    function renderTitle(type, title)
    {
        var htmlOut = "<tr class=\"labkey-wp-header\"><th colspan=2 align=left>";
        htmlOut +="   <a href=\"#\" onclick=\"return LABKEY.Utils.toggleLink(this, false);\">";
        htmlOut +="   <img src=\"";
        htmlOut += LABKEY.contextPath + "/_images/";
        htmlOut += "plus.gif \"></a>&nbsp;";
        if (type)
            htmlOut += type + ": ";
        htmlOut += title;
        htmlOut += "</th></tr>";
        return htmlOut;
    }

    // Mask for the plot
    var maskGraph = new Ext4.LoadMask('divForm', {
        msg: "Generating the graphics, please, wait..."
    });

    // mask for session management
    var maskCreateSession = new Ext4.LoadMask('divForm', {
        msg: "Creating the session, please wait ..."
    });

    var maskDeleteSession = new Ext4.LoadMask('divForm', {
        msg: "Deleting the session, please wait ..."
    });

    function onFailure(errorInfo, responseObj, options){
        maskGraph.hide();
        var htmlOut = renderText(null, "error", errorInfo.exceptionClass + ': ' + errorInfo.exception);
        renderFinal(htmlOut);
    };

    function onCreateSessionSuccess(responseObj) {
        maskCreateSession.hide();
        _sessionId = responseObj.reportSessionId;
    }

    function onDeleteSessionSuccess(responseObj) {
        maskDeleteSession.hide();
        _sessionId = null;
    }

    function onSuccess(responseObj){
        maskGraph.hide();
        if (!responseObj)
            return;

        var consoles = responseObj.console;
        var errors = responseObj.errors;
        var outputParams = responseObj.outputParams;
        var htmlOut = "";
        var i;
        var param;

        for (i = 0; i < consoles.length; i++)
            htmlOut += renderText(null, "console", consoles[i]);

        for (i = 0; i < errors.length; i++)
            htmlOut += renderText(null, "error", errors[i]);

        for (i = 0; i < outputParams.length; i++)
        {
            param = outputParams[i];
            if (param.type == 'image')
            {
                htmlOut += renderImage(param.type, param.name, param.value);
            }
            else if (param.type == 'text' ||
                    param.type == 'html' ||
                    param.type == 'tsv')
            {
                htmlOut += renderText(param.type, param.name, param.value);
            }
            else if (param.type == 'json')
            {
                htmlOut += renderText(param.type, param.name, JSON.stringify(param.value, null, '\t'));
            }
            else if (param.type == 'file' ||
                    param.type == 'pdf' ||
                    param.type == 'postscript')
            {
                htmlOut += renderDownload(param.type, param.name, param.value);
            }
            else
            {
                htmlOut += renderText("BUG", "bug", "unknown output parameter type!");
            }
        }

        renderFinal(htmlOut);
    };

    Ext4.onReady(function(){
        //
        // setup to execute the report as a script
        //
        var scriptParams = {
            param1 : 'This is my parameter 1 value',
            param2 : 42
        };

        var scriptConfig = {
            success: onSuccess,
            failure: onFailure,
            inputParams: scriptParams
        };

        //
        // setup to execute the report as a webpart
        //
        var webpartReportConfig = {
            title: 'webpart result'

        };
        webpartReportConfig.param1 = scriptParams.param1;
        webpartReportConfig.param2 = scriptParams.param2;

        var webpartReport = new LABKEY.WebPart({
            failure: onFailure,
            success: onSuccess,
            frame: 'none',
            partConfig: webpartReportConfig,
            partName: 'Report',
            renderTo: 'divOutput'
        });

        //
        // setup our input form
        //
        var textField = Ext4.create('Ext.form.field.Text', {
               name: "script",
               fieldLabel: "script name",
               allowBlank: false
           });

        var paramField = Ext4.create('Ext.form.field.Text', {
            name: "param1",
            fieldLabel: "parameter",
            allowBlank: false
        });


        Ext4.create('Ext.form.Panel', {
               renderTo: 'divForm',
               width: 800,
               //layout: 'anchor',
               title:  'Scriptpad',
               defaults: {
                 margin: '5 0 5 5'
               },
               items: [ textField, paramField ],
               buttons: [{
                   text: 'Execute as Script',
                   handler: function() {
                       preparePage();
                       scriptConfig.reportId = 'module:/scriptpad/' + textField.value;
                       scriptConfig.reportSessionId = _sessionId;
                       LABKEY.Report.execute(scriptConfig);
                   }},
                   {
                   text: 'Execute as Webpart',
                   handler: function() {
                       preparePage();
                       webpartReportConfig.reportId = 'module:/scriptpad/' + textField.value;
                       webpartReportConfig.param1 = paramField.value;
                       webpartReport.render();
                    }},
                   {
                   text: 'Create Report Session',
                   handler: function() {
                       maskCreateSession.show();
                       LABKEY.Report.createSession({
                           success : onCreateSessionSuccess
                      });
                   }},
                   {
                   text: 'Delete Report Session',
                   handler: function() {
                       maskDeleteSession.show();
                       LABKEY.Report.deleteSession({
                           success : onDeleteSessionSuccess,
                           reportSessionId : _sessionId
                       });
                   }}
               ]
           });
    });
</script>
