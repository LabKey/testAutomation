/*
 * Copyright (c) 2016-2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
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
