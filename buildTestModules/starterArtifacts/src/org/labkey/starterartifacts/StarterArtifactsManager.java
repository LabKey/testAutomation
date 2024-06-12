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

package org.labkey.starterartifacts;

import org.labkey.api.assay.AssayRunDomainKind;
import org.labkey.api.admin.HealthCheck;
import org.labkey.api.data.Container;
import org.labkey.api.issues.IssuesUrls;
import org.labkey.api.security.User;
import org.labkey.api.security.roles.TrustedAnalystRole;
import org.labkey.api.view.ActionURL;
import org.labkey.api.di.DataIntegrationService;
import org.labkey.api.inventory.InventoryService;

public class StarterArtifactsManager implements HealthCheck, IssuesUrls
{
    private static final StarterArtifactsManager _instance = new StarterArtifactsManager();

    private StarterArtifactsManager()
    {
        // prevent external construction with a private default constructor
    }

    public static StarterArtifactsManager get()
    {
        return _instance;
    }

    public AssayRunDomainKind getAssayRunDomainKind()
    {
        return new AssayRunDomainKind();
    }

    public HealthCheck.Result checkHealth() {
        return new HealthCheck.Result();
    }

    @Override
    public ActionURL getDetailsURL(Container container)
    {
        return null;
    }

    @Override
    public ActionURL getInsertURL(Container container, String s)
    {
        return null;
    }

    @Override
    public ActionURL getListURL(Container container, String s)
    {
        return null;
    }

    public void dataIntegrationServiceCheck()
    {
        DataIntegrationService service = DataIntegrationService.get();
    }

    public void premiumRolesCheck()
    {
        TrustedAnalystRole role = new TrustedAnalystRole();
    }

    public void inventoryServiceCheck()
    {
        InventoryService service = InventoryService.get();
    }
}
