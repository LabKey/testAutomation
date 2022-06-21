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
import org.labkey.test.pages.core.admin.ConfigureFileSystemAccessPage;
import org.labkey.test.pages.core.admin.LimitActiveUserPage;
import org.labkey.test.pages.core.login.DatabaseAuthConfigureDialog;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineToolsHelper;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.login.AuthenticationAPIUtils;
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

        Connection connection = createDefaultConnection();

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
            DatabaseAuthConfigureDialog.resetDbLoginConfig(connection);
        }
        catch (RuntimeException e)
        {
            TestLogger.error("Failed to reset DB login config after test", e);
        }

        try
        {
            // disable any/all secondary auth configurations
            AuthenticationAPIUtils.deleteConfigurations("TestSecondary", connection);
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
            resetPremiumPageElements();
        }
        catch (IOException | CommandException e)
        {
            TestLogger.error("Failed to reset header and footer settings after test", e);
        }

        try
        {
            disableFileUploadSetting();
        }
        catch (Exception e)
        {
            TestLogger.error("Failed to re-enable file Upload after test", e);
        }

        try
        {
            LimitActiveUserPage.resetUserLimits(connection);
        }
        catch (IOException | CommandException e)
        {
            TestLogger.error("Failed to reset active user limit after test", e);
        }

    }

    @LogMethod(quiet = true)
    private void disableLoginAttemptLimit() throws IOException, CommandException
    {
        PostCommand<CommandResponse> command = new PostCommand<>("compliance", "complianceSettings");
        Map<String, Object> params = new HashMap<>();
        params.put("tab", "login");
        params.put("attemptEnabled", "false");
        command.setParameters(params);

        executeIgnoring404(command);
    }

    @LogMethod(quiet = true)
    private void resetPremiumPageElements() throws IOException, CommandException
    {
        PostCommand<CommandResponse> command = new PostCommand<>("premium", "adminConsoleConfigurePageElements");
        Map<String, Object> params = new HashMap<>();
        params.put("headerModule", "none");
        params.put("bannerModule", "none");
        params.put("footerModule", "Core");
        command.setParameters(params);

        executeIgnoring404(command);
    }

    private void executeIgnoring404(PostCommand<CommandResponse> command) throws IOException, CommandException
    {
        Connection connection = getRemoteApiConnection();
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

    private void disableFileUploadSetting() throws IOException, CommandException
    {
        if (TestProperties.isTestRunningOnTeamCity())
        {
            PostCommand<CommandResponse> command = new PostCommand<>("admin", "filesSiteSettings");
            // POST'ing without any parameters will enable upload without touching site level file root
            command.execute(createDefaultConnection(), "/");
        }
        else
        {
            // Go through UI locally to avoid messing up user folders (experimental feature)
            ConfigureFileSystemAccessPage fsaPage = ConfigureFileSystemAccessPage.beginAt(this);
            Checkbox disableCheckBox = new Checkbox(Locator.input("fileUploadDisabled").findElementOrNull(getDriver()));
            if (disableCheckBox.getComponentElement() != null && disableCheckBox.isChecked())
            {
                disableCheckBox.uncheck();
                fsaPage.save();
            }
        }
    }
}
