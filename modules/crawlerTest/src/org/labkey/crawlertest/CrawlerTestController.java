/*
 * Copyright (c) 2020 LabKey Corporation
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

package org.labkey.crawlertest;

import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

public class CrawlerTestController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(CrawlerTestController.class);
    public static final String NAME = "crawlertest";

    public CrawlerTestController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class InjectJspAction extends SimpleViewAction<InjectForm>
    {
        @Override
        public void validate(InjectForm form, BindException errors)
        {
            if (!AppProps.getInstance().isDevMode())
            {
                errors.reject(ERROR_MSG, "This action requires dev mode. Enable with caution. Doing so will expose a script injection vulnerability.");
            }
            if (!getViewContext().getUser().getEmail().equals("injectiontester@labkey.injection.test"))
            {
                errors.reject(ERROR_MSG, "This action only responds to a specific test user.");
            }
        }

        @Override
        public ModelAndView getView(InjectForm form, BindException errors)
        {
            if (errors.hasErrors())
            {
                return new SimpleErrorView(errors);
            }
            getPageConfig().setTitle("Injection test page");
            return new JspView<>("/org/labkey/crawlertest/view/injectJsp.jsp", form.getInject());
        }

        @Override
        public void addNavTrail(NavTree root)
        { }
    }

    @RequiresPermission(ReadPermission.class)
    public class ExternalLinkAction extends SimpleViewAction<Object>
    {
        @Override
        public ModelAndView getView(Object form, BindException errors)
        {
            getPageConfig().setTitle("External link test page");
            return new JspView<>("/org/labkey/crawlertest/view/externalLink.jsp");
        }

        @Override
        public void addNavTrail(NavTree root)
        { }
    }

    public static class InjectForm
    {
        private String _inject;

        public String getInject()
        {
            return _inject;
        }

        public InjectForm setInject(String inject)
        {
            _inject = inject;
            return this;
        }
    }
}
