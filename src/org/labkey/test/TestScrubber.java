/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.apache.http.HttpStatus;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.ConfigureDbLoginPage;
import org.labkey.test.pages.core.admin.ConfigureFileSystemAccessPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineToolsHelper;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.labkey.test.WebTestHelper.getRemoteApiConnection;

public class TestScrubber extends ExtraSiteWrapper
{
    public TestScrubber(BrowserType browserType, File downloadDir)
    {
        super(browserType, downloadDir);
    }

    @LogMethod
    public void cleanSiteSettings()
    {
        try
        {
            simpleSignIn();
        }
        catch (WebDriverException wde)
        {
            // Sometimes, we are unable to create a second FirefoxDriver instance
            TestLogger.error("Unable to clean site settings", wde);
            return;
        }

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
            TestLogger.error("**************************ERROR*******************************");
            TestLogger.error("** SERIOUS ERROR: Failed to reset pipeline tools directory. **");
            TestLogger.error("** Server may be in a bad state.                            **");
            TestLogger.error("** Set tools directory manually or bootstrap to fix.        **");
            TestLogger.error("**************************ERROR*******************************");
            TestLogger.error("", e);
        }

        try
        {
            deleteSiteWideTermsOfUsePage();
        }
        catch (RuntimeException e)
        {
            TestLogger.error("Failed to remove site-wide terms of use. This will likely cause other tests to fail.", e);
        }

        try
        {
            ConfigureDbLoginPage.resetDbLoginConfig(this);
        }
        catch (RuntimeException e)
        {
            TestLogger.error("Failed to reset DB login config after test", e);
        }

        try
        {
            disableSecondaryAuthentication();
        }
        catch (RuntimeException e)
        {
            TestLogger.error("Failed to reset Secondary Authentication after test", e);
        }
        try
        {
            disableLoginAttemptLimit();
        }
        catch (IOException | CommandException e)
        {
            TestLogger.error("Failed to disable login attempt limit after test", e);
        }

        try
        {
            disableFileUploadSetting();
        }
        catch (RuntimeException e)
        {
            TestLogger.error("Failed to re-enable file Upload after test", e);
        }
    }

    @LogMethod(quiet = true)
    private void disableLoginAttemptLimit() throws IOException, CommandException
    {
        Connection connection = getRemoteApiConnection();
        PostCommand<CommandResponse> command = new PostCommand<>("compliance", "complianceSettings");
        Map<String, Object> params = new HashMap<>();
        params.put("tab", "login");
        params.put("attemptEnabled", "false");
        command.setParameters(params);

        try
        {
            command.execute(connection, "/");
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND)
            {
                throw e;
            }
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
