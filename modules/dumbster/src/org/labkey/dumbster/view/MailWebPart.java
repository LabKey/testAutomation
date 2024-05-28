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

package org.labkey.dumbster.view;

import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.dumbster.DumbsterController;
import org.labkey.dumbster.model.DumbsterManager;

import jakarta.servlet.ServletException;

public class MailWebPart extends JspView<MailPage>
{
    public MailWebPart()
    {
        super("/org/labkey/dumbster/view/mailWebPart.jsp", new MailPage());
        setTitle("Mail Record");
        setTitleHref(new ActionURL(DumbsterController.BeginAction.class, getViewContext().getContainer()));
    }


    @Override
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