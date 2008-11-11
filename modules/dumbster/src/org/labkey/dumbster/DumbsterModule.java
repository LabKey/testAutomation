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

package org.labkey.dumbster;

import org.apache.log4j.Logger;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.*;
import org.labkey.dumbster.model.DumbsterManager;
import org.labkey.dumbster.view.MailWebPart;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

public class DumbsterModule extends DefaultModule
{
    private static final Logger _log = Logger.getLogger(DumbsterModule.class);
    public static final String NAME = "Dumbster";

    public String getName()
    {
        return "Dumbster";
    }

    public double getVersion()
    {
        return 1.00;
    }

    protected void init()
    {
        addController("dumbster", DumbsterController.class);
        DumbsterManager.setInstance(new DumbsterManager());
    }

    protected Collection<? extends WebPartFactory> createWebPartFactories()
    {
        return Arrays.asList(new BaseWebPartFactory("Mail Record") {
                public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart) throws IllegalAccessException, InvocationTargetException
                {
                    return new MailWebPart();
                }
            });
    }

    public boolean hasScripts()
    {
        return false;
    }

    public void startup(ModuleContext moduleContext)
    {
        if (AppProps.getInstance().isMailRecorderEnabled())
            DumbsterManager.get().start();
    }
}