/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.tests;

import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebDriverWrapper;
import java.util.Arrays;
import java.util.List;

public class GlassComponentTests extends BaseWebDriverTest
{
    private final static String PROJECT_NAME = "Glass_Component_Tests";

    @Override
    protected WebDriverWrapper.BrowserType bestBrowser()
    {
        return WebDriverWrapper.BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("premium");
    }

    @Test
    public void testGrids()
    {
        // TODO: Test needs to be rewritten using the new components page.
    }

}
