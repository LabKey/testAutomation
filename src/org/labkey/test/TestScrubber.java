/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test;

import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.ConfigureDbLoginPage;
import org.labkey.test.pages.core.admin.ConfigureFileSystemAccessPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineToolsHelper;

import java.io.File;

public class TestScrubber extends ExtraSiteWrapper
{
    public TestScrubber(BrowserType browserType, File downloadDir)
    {
        super(browserType, downloadDir);
    }

    @LogMethod
    public void cleanSiteSettings()
    {
        simpleSignIn();

        try
        {
            // Get DB back in a good state after failed pipeline tools test.
            PipelineToolsHelper pipelineToolsHelper = new PipelineToolsHelper(this);
            pipelineToolsHelper.resetPipelineToolsDirectory();
        }
        catch (RuntimeException e)
        {
            // Assure that this failure is noticed
            // Regression check: https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=10732
            log("**************************ERROR*******************************");
            log("** SERIOUS ERROR: Failed to reset pipeline tools directory. **");
            log("** Server may be in a bad state.                            **");
            log("** Set tools directory manually or bootstrap to fix.        **");
            log("**************************ERROR*******************************");
        }

        try
        {
            deleteSiteWideTermsOfUsePage();
        }
        catch (RuntimeException e)
        {
            log("Failed to remove site-wide terms of use. This will likely cause other tests to fail.");
        }

        try
        {
            ConfigureDbLoginPage.resetDbLoginConfig(this);
        }
        catch (RuntimeException e)
        {
            log("Failed to reset DB login config after test");
        }

        try
        {
            disableSecondaryAuthentication();
        }
        catch (RuntimeException e)
        {
            log("Failed to reset Secondary Authentication after test");
        }
        try
        {
            disableLoginAttemptLimit();
        }
        catch (RuntimeException e)
        {
            log("Failed to disable login attempt limit after test");
        }

        try
        {
            disableFileUploadSetting();
        }
        catch (RuntimeException e)
        {
            log("Failed to re-enable file Upload after test");
        }
    }

    private void disableFileUploadSetting()
    {
        ConfigureFileSystemAccessPage fsaPage = ConfigureFileSystemAccessPage.beginAt(this);
        Checkbox disableCheckBox = new Checkbox(Locator.input("fileUploadDisabled").findElementOrNull(getDriver()));
        if (disableCheckBox.getComponentElement() != null && disableCheckBox.isChecked())
        {
            disableCheckBox.uncheck();
            fsaPage.save();
        }
    }
}
