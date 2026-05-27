/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.util.compliance;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.pages.compliance.ActivityDialog;
import org.labkey.test.pages.compliance.TermsOfUse;
import org.labkey.test.util.UIContainerHelper;

/**
 * Created by RyanS on 2/12/2016.
 */
public class ComplianceActivityContainerHelper extends UIContainerHelper
{
    public ComplianceActivityContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    protected void doDeleteProject(String project, boolean failIfNotFound, int wait)
    {
        _test.goToProjectHome(project);
        if (!failIfNotFound && _test.getResponseCode() == 404)
            return;
        ActivityDialog dialog = new ActivityDialog(_test.getDriver());
        if(dialog.isDialogPresent())
        {
            dialog.setActivityDialogOptions();
        }
        TermsOfUse terms = new TermsOfUse(_test.getDriver());
        if(terms.isTermsDialogPresent())
        {
            terms.agreeToTermsAndOk();
        }
        super.doDeleteProject(project, failIfNotFound, wait);
    }
}
