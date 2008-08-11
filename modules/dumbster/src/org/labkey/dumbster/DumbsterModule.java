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

package org.labkey.dumbster;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.security.User;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.api.settings.AppProps;
import org.labkey.dumbster.model.DumbsterManager;
import org.labkey.dumbster.view.MailWebPart;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;

public class DumbsterModule extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(DumbsterModule.class);
    public static final String NAME = "Dumbster";

    public DumbsterModule()
    {
        super(NAME, 1.00, null, false,
            new WebPartFactory("Mail Record") {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws IllegalAccessException, InvocationTargetException
                {
                    return new MailWebPart();
                }
            });

        addController("dumbster", DumbsterController.class);
        
        DumbsterManager.setInstance(new DumbsterManager());
    }

    public void containerCreated(Container c)
    {
    }

    public void containerDeleted(Container c, User user)
    {
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void startup(ModuleContext moduleContext)
    {
        super.startup(moduleContext);
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(this);

        if (AppProps.getInstance().isMailRecorderEnabled())
            DumbsterManager.get().start();
    }
}