/*
 * Copyright (c) 2008-2018 LabKey Corporation
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

package org.labkey.dumbster;

import com.dumbster.smtp.SmtpMessage;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ExportAction;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.WriteableAppProps;
import org.labkey.api.util.MailHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.api.view.template.PageConfig;
import org.labkey.dumbster.model.DumbsterManager;
import org.labkey.dumbster.view.MailWebPart;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * View action is like MultiActionController, but each action is a class not a method
 */
public class DumbsterController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(DumbsterController.class);

    public DumbsterController()
    {
        setActionResolver(_actionResolver);
    }

    public PageConfig defaultPageConfig()
    {
        return new PageConfig();
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors)
        {
            if (getUser().hasRootAdminPermission())
                return new MailWebPart();
            else
                return new HtmlView("You must be a site or application administrator to view the email record.");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Mail Record");
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class SetRecordEmailAction extends MutatingApiAction<RecordEmailForm>
    {
        public ApiResponse execute(RecordEmailForm form, BindException errors)
        {
            if (!getUser().hasRootAdminPermission())
                throw new UnauthorizedException();

            if (form.isRecord())
            {
                if (!DumbsterManager.get().start())
                    return new ApiSimpleResponse("error", "Error starting mail recorder. Check log for more information.");
            }
            else
            {
                DumbsterManager.get().stop();
            }

            WriteableAppProps props = AppProps.getWriteableInstance();
            props.setMailRecorderEnabled(form.isRecord());
            props.save(getUser());
            return null;
        }
    }

    public static class RecordEmailForm
    {
        private boolean _record;

        public boolean isRecord()
        {
            return _record;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setRecord(boolean record)
        {
            _record = record;
        }
    }

    public static class MessageForm
    {
        private int _message;
        private String _type;

        public int getMessage()
        {
            return _message;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setMessage(int message)
        {
            _message = message;
        }

        public String getType()
        {
            return _type;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setType(String type)
        {
            _type = type;
        }
    }

    public static ActionURL getViewMessageURL(Container c, int index, String type)
    {
        ActionURL url = new ActionURL(ViewMessage.class, c);
        url.addParameter("message", index);
        url.addParameter("type", type);
        return url;
    }

    @RequiresPermission(AdminPermission.class)
    public class ViewMessage extends ExportAction<MessageForm>
    {
        @Override
        public void export(MessageForm form, HttpServletResponse response, BindException errors) throws Exception
        {
            if (!getUser().hasRootAdminPermission())
                throw new UnauthorizedException();

            SmtpMessage message = DumbsterManager.get().getMessages()[form.getMessage()];
            Map<String, String> map = MailHelper.getBodyParts(DumbsterManager.convertToMimeMessage(message));

            String output;
            String desiredContentType = "text/plain";

            if ("html".equals(form.getType()))
            {
                String html = map.get("text/html");

                if (null != html)
                {
                    desiredContentType = "text/html";
                    output = html;
                }
                else
                {
                    output = "No HTML found";
                }
            }
            else
            {
                String contents = map.get("text/plain");
                output = null != contents ? contents : "No text found";
            }

            getPageConfig().setTemplate(PageConfig.Template.None);

            // Just blast the contents... no debug comments, view divs, etc.
            response.setContentType(desiredContentType);
            response.getWriter().print(output);
            response.flushBuffer();
        }
    }

}