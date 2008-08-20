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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ActionURL;
import org.labkey.api.settings.AppProps;
import org.labkey.dumbster.model.DumbsterManager;

import javax.servlet.ServletException;

public class MailWebPart extends JspView<MailPage>
{
    static Logger _log = Logger.getLogger(MailWebPart.class);

    public MailWebPart()
    {
        this(HttpView.currentContext().getContainer());
    }


    public MailWebPart(Container c)
    {
        super("/org/labkey/dumbster/view/mailWebPart.jsp", new MailPage());
        setTitle("Mail Record");
        setTitleHref(new ActionURL("demo", "begin", HttpView.currentContext().getContainer()));
    }


    protected void prepareWebPart(MailPage model) throws ServletException
    {
        super.prepareWebPart(model);

        if (model.getMessages() == null)
        {
            DumbsterManager manager = DumbsterManager.get();
            model.setEnableRecorder(manager.isRecording());
            model.setMessages(manager.getMessages());
        }
    }
}