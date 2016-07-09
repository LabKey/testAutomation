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

package org.labkey.dumbster;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.module.CodeOnlyModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.settings.AppProps;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.dumbster.model.DumbsterManager;
import org.labkey.dumbster.view.MailWebPart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class DumbsterModule extends CodeOnlyModule
{
    public String getName()
    {
        return "Dumbster";
    }

    protected void init()
    {
        addController("dumbster", DumbsterController.class);
        DumbsterManager.setInstance(new DumbsterManager());
    }

    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return new ArrayList<>(Arrays.asList(new BaseWebPartFactory("Mail Record")
        {
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                return new MailWebPart();
            }
        }));
    }

    public void doStartup(ModuleContext moduleContext)
    {
        if (AppProps.getInstance().isMailRecorderEnabled())
            DumbsterManager.get().start();
    }

    @Override
    public boolean isAutoUninstall()
    {
        return true;
    }
}