/*
 * Copyright (c) 2010-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineConfig;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineType;
import org.labkey.test.pages.admin.RConfigurationPage;
import org.labkey.test.pages.reports.ScriptReportPage;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;
import static org.labkey.test.util.DataRegionTable.DataRegion;

public class RReportHelper
{
    public static final String RDOCKER = "RDocker";

    public enum ReportOption implements ScriptReportPage.ReportOption
    {
        @Deprecated (since = "22.7")
        shareReport(ScriptReportPage.StandardReportOption.shareReport),
        knitrNone("None" + Locator.NBSP, "Knitr Options", false),
        knitrHtml("Html" + Locator.NBSP, "Knitr Options", false),
        knitrMarkdown("Markdown" + Locator.NBSP, "Knitr Options", false);

        public final String _label;
        public final boolean _isCheckbox;
        public final String _fieldSet;

        ReportOption(String label, String section, boolean isCheckbox)
        {
            _label = label;
            _fieldSet = section;
            _isCheckbox = isCheckbox;
        }

        ReportOption(ScriptReportPage.StandardReportOption option)
        {
            this(option.getLabel(), option.getSection(), option.isCheckbox());
        }

        @Override
        public String getLabel()
        {
            return _label;
        }

        @Override
        public boolean isCheckbox()
        {
            return _isCheckbox;
        }

        @Override
        public String getSection()
        {
            return _fieldSet;
        }
    }

    protected final BaseWebDriverTest _test;

    public RReportHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    private static File rExecutable = null;
    private static File rScriptExecutable = null;
    private static String defaultEngineVersion = null;

    private static final String localEngineName = "R Scripting Engine";
    public static final String R_DOCKER_SCRIPTING_ENGINE = "R Docker Scripting Engine";
    private static final String REMOTE_R_SERVE ="Remote R Scripting Engine";

    private static final String INSTALL_RLABKEY = "install.packages(\"Rlabkey\", repos=\"http://cran.r-project.org\")";

    public static File getRLibraryPath()
    {
        String libPath = System.getenv("R_LIBS_USER");
        if (libPath != null)
            return new File(libPath);

        // default to sampledata/rlabkey path
        return new File(TestFileUtils.getLabKeyRoot(), "/sampledata/rlabkey");
    }

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    public boolean executeScript(String script, String verify)
    {
        return executeScript(script, verify, false);
    }

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    @LogMethod (quiet = true)
    public boolean executeScript(String script, String expectedLines, boolean failOnError)
    {
        // running a saved script
        clickSourceTab();

        _test.setCodeEditorValue("script-report-editor", script);
        clickReportTab();

        String html = getReportText();

        return checkScriptOutput(html, expectedLines, failOnError);
    }

    @NotNull
    public String getReportText()
    {
        Locator l = Locator.xpath("//div[@class='reportView']//pre");
        _test.waitForElement(l);
        return _test.getText(l).replaceAll(" +", " ");
    }

    public boolean executeScriptDirectly(String script, String expectedLines) throws IOException
    {
        return executeScriptDirectly(script, expectedLines, false);
    }

    public boolean executeScriptDirectly(String script, String expectedLines, boolean failOnError) throws IOException
    {
        String scriptOutput = getRScriptOutput(script);

        return checkScriptOutput(scriptOutput, expectedLines, failOnError);
    }

    private boolean checkScriptOutput(String scriptOutput, String expectedLines, boolean failOnError)
    {
        if (failOnError && doesScriptProduceError(scriptOutput))
            return false;

        return doesScriptProduceOutput(expectedLines, scriptOutput);
    }

    private boolean doesScriptProduceOutput(String expectedLines, String scriptOutput)
    {
        if (!StringUtils.isEmpty(expectedLines))
        {
            // split string on newlines to make the comparison more reliable
            String[] parts = expectedLines.split("\n");

            for (String part : parts)
            {
                if (!scriptOutput.contains(part.trim()))
                {
                    TestLogger.error("Error: could not find expected text: " + part + ".\nfrom value:\n" + scriptOutput);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean doesScriptProduceError(String scriptOutput)
    {
        if (scriptOutput.contains("javax.script.ScriptException"))
        {
            TestLogger.error("Error: the script failed with an error:\n" + scriptOutput);
            return true;
        }

        return false;
    }

    public String ensureRConfig()
    {
        return ensureRConfig(false);
    }

    /**
     * Ensure that an R script engine is configured
     * @param useDocker Specify whether the Dockerized R engine should be used
     * @return R version (e.g. 3.4.2); or "RDocker" for dockerized R engine
     */
    @LogMethod
    public String ensureRConfig(boolean useDocker)
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);

        if (TestProperties.isServerRemote() || TestProperties.isPrimaryUserAppAdmin())
        {
            String reason = "Unable to configure script engines " + (TestProperties.isServerRemote()
                ? "on remote server."
                : "as app admin.");
            Assert.assertTrue("R engine not present. " + reason,
                scripts.isEnginePresentForLanguage("R"));
            TestLogger.log(reason + " Using existing engine.");
            if (defaultEngineVersion == null)
            {
                ConfigureReportsAndScriptsPage.EditEngineWindow editEngineWindow =
                    scripts.editDefaultEngine("R");
                defaultEngineVersion = editEngineWindow.getLanguageVersion();
                _test.getArtifactCollector().dumpPageSnapshot("R_engine_config", null);
            }
            TestLogger.log("R engine version: " + defaultEngineVersion);
            return defaultEngineVersion;
        }

        _test.log("Check if R already is configured");

        String rVersion = null;
        if (useDocker)
        {
            rVersion = RDOCKER;
            if (!scripts.isEnginePresent(R_DOCKER_SCRIPTING_ENGINE))
                scripts.addEngineWithDefaults(EngineType.R_DOCKER);

            scripts.setSiteDefault(R_DOCKER_SCRIPTING_ENGINE);
        }
        else
        {
            _test.refresh(); // Avoid menu alignment issue on TeamCity

            if (scripts.isEnginePresent(localEngineName))
            {
                if (!TestProperties.isTestRunningOnTeamCity())
                {
                    scripts.editEngine(localEngineName);
                    rExecutable = new File(_test.getFormElement(Locator.id("editEngine_exePath-inputEl")));
                    TestLogger.log(localEngineName + " is already configured using: " + rExecutable.getAbsolutePath());
                    rVersion = getRVersion(rExecutable);
                    _test.clickButton("Cancel", 0);
                    scripts.setSiteDefault(localEngineName);
                    return rVersion;
                }
                else // Reset R scripting engine on TeamCity
                {
                    scripts.deleteAllREngines();
                    _test.refresh(); // Avoid menu alignment issue on TeamCity
                }
            }

            rVersion = getRVersion(getRExecutable());

            EngineConfig config = new EngineConfig(getRExecutable());
            config.setVersion(rVersion);
            scripts.addEngine(EngineType.R, config);
            scripts.setSiteDefault(localEngineName);
        }
        return rVersion;
    }

    public void configureRemoteRserve(String reports_temp,String data)
    {
        String username = "rserve";
        String password = "rserve";
        ConfigureReportsAndScriptsPage.RServeEngineConfig config = new ConfigureReportsAndScriptsPage.RServeEngineConfig(username,password,reports_temp,data);
        config.setMachine("127.0.0.1");
        config.setPortNumber("6311");

        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);
        if(!scripts.isEnginePresent(REMOTE_R_SERVE))
            scripts.addEngine(EngineType.REMOTE_R, config);

        scripts.setSiteDefault(REMOTE_R_SERVE);
    }

    public void ensureFolderREngine(String engineName)
    {
        ensureFolderREngine(engineName, engineName);
    }

    @LogMethod
    public void ensureFolderREngine(@LoggedParam String reportEngineName, @LoggedParam String pipelineEngineName)
    {
        RConfigurationPage rConfigurationPage = _test.goToFolderManagement().goToRConfigTab();
        rConfigurationPage.setEngineOverrides(reportEngineName, pipelineEngineName);
        if (rConfigurationPage.isSaveEnabled())
        {
            rConfigurationPage.save();
        }
    }

    public void resetFolderREngine()
    {
        RConfigurationPage rConfigurationPage = _test.goToFolderManagement().goToRConfigTab();

        if (rConfigurationPage.isConfigInherited())
        {
            return;
        }

        _test.log("Remove folder's engine override.");
        rConfigurationPage.setInheritConfiguration();
        rConfigurationPage.save();
    }

    public void setPandocEnabled(Boolean enabled)
    {
        if (TestProperties.isPrimaryUserAppAdmin())
        {
            TestLogger.warn("Test is running as app admin. Unable to modify R engine.");
            return;
        }
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);

        String defaultScriptName = "R Scripting Engine";
        assertTrue("R Engine not setup", scripts.isEnginePresentForLanguage("R"));

        scripts.editEngine(defaultScriptName);

        Checkbox enabledCheckbox = Checkbox.Ext4Checkbox().withLabel("Use pandoc & rmarkdown:").find(_test.getDriver());
        if(enabled)
            enabledCheckbox.check();
        else
            enabledCheckbox.uncheck();

        _test.clickButton("Submit", 0);
        _test.waitForElementToDisappear(ConfigureReportsAndScriptsPage.Locators.editEngineWindow);
    }


    public File getRExecutable()
    {
        if (rExecutable != null)
            return rExecutable;

        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            _test.log("R_HOME is set to: " + rHome + " searching for the R application");
            File rHomeDir = new File(rHome);
            FileFilter rFilenameFilter =
                    file -> ("r.exe".equalsIgnoreCase(file.getName()) || "r".equalsIgnoreCase(file.getName()))
                            && file.canExecute();
            File[] files = rHomeDir.listFiles(rFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(rHome, "bin").listFiles(rFilenameFilter);
            }

            if (files != null && files.length > 0)
            {
                if (files.length > 1)
                {
                    TestLogger.log("WARNING: Found multiple R executables:");
                    for (File file : files)
                    {
                        _test.log("\t" + file.getAbsolutePath());
                    }
                }
                rExecutable = files[0];
                return rExecutable;
            }
        }

        TestLogger.error("Environment info: " + System.getenv());

        if (null == rHome)
        {
            TestLogger.error("");   // Blank line helps make the following message more readable
            TestLogger.error("R_HOME environment variable is not set.  Set R_HOME to your R bin directory to enable automatic configuration.");
        }
        throw new IllegalStateException("R is not configured on this system. Failed R tests.");
    }

    private File getRScriptExecutable()
    {
        if (rScriptExecutable == null)
            rScriptExecutable = new File(getRExecutable().getParentFile(), "rscript");
        return rScriptExecutable;
    }

    private String getRVersion(File r)
    {
        String versionOutput = "";
        try
        {
            versionOutput = new ProcessHelper(r, "--version").getProcessOutput().trim();

            Pattern versionPattern = Pattern.compile("R version ([1-9]\\.\\d+\\.\\d)");
            Matcher matcher = versionPattern.matcher(versionOutput);
            matcher.find();
            String versionNumber = matcher.group(1);

            _test.log("R --version > " + versionNumber);

            return versionNumber;
        }
        catch(IOException ex)
        {
            if (versionOutput.length() > 0)
                _test.log("R --version > " + versionOutput);
            throw new RuntimeException("Unable to determine R version: " + r.getAbsolutePath(), ex);
        }
    }

    public String getRScriptOutput(String scriptContents) throws IOException
    {
        return new ProcessHelper(getRScriptExecutable(), "-e", scriptContents).getProcessOutput().trim();
    }

    public ScriptReportPage getReportPage()
    {
        return new ScriptReportPage(_test.getDriver());
    }

    public void saveReport(String name, boolean isSaveAs, int wait)
    {
        getReportPage().saveReport(name, isSaveAs, wait);
    }

    /**
     * Precondition: on save popup window
     */
    public void saveReportWithName(String name, boolean isSaveAs)
    {
        getReportPage().saveReportWithName(name, isSaveAs);
    }

    public void saveReportWithName(String name, boolean isSaveAs, boolean isExternal)
    {
        getReportPage().saveReportWithName(name, isSaveAs, isExternal);
    }

    public void saveReport(String name)
    {
        getReportPage().saveReport(name);
    }

    public void saveAsReport(String name)
    {
        getReportPage().saveAsReport(name);
    }

    public void selectOption(ScriptReportPage.ReportOption option)
    {
        getReportPage().selectOption(option);
    }

    public void clearOption(ScriptReportPage.ReportOption option)
    {
        getReportPage().clearOption(option);
    }

    public void clickReportTab()
    {
        getReportPage().clickReportTab();
    }

    public void clickSourceTab()
    {
        getReportPage().clickSourceTab();
    }

    public void ensureFieldSetExpanded(String name)
    {
        getReportPage().ensureFieldSetExpanded(name);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     */
    public void createRReport(String name)
    {
        createRReport(name, false);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     * @param shareView should this view be available to all users
     */
    public void createRReport(String name, boolean shareView)
    {
        _test.waitForText(("Reports"));
        DataRegion(_test.getDriver()).find().goToReport("Create R Report");

        if (shareView)
            selectOption(ScriptReportPage.StandardReportOption.shareReport);

        saveReport(name);
    }

    /**
     * pre-condition: at folder that the report is to be created from
     * @param reportName
     * @param reportSource
     * @param shareView
     * @return labkey-output content of report
     */
    public String createAndRunRReport(String reportName, String reportSource, boolean shareView)
    {
        _test.goToManageViews();

        BootstrapMenu.find(_test.getDriver(), "Add Report")
                .clickSubMenu(true, "R Report");
        RReportHelper rReportHelper = new RReportHelper(_test);

        _test.setCodeEditorValue("script-report-editor", reportSource);

        if (shareView)
            selectOption(ScriptReportPage.StandardReportOption.shareReport);

        rReportHelper.saveReport(reportName);
        _test.waitForText(reportName);
        _test.log("Report created: " + reportName);

        Locator reportOutput = Locator.tagWithClass("table", "labkey-output");
        _test.waitForElement(reportOutput);
        return _test.getText(reportOutput);
    }


    /**
     * pre-conditions: at report's Report tab
     */
    public WebElement assertKnitrReportContents(Locator[] reportContains, String[] reportNotContains)
    {
        WebElement reportDiv = _test.waitForElement(Locator.css("div.reportView > div.labkey-knitr"), WAIT_FOR_PAGE);

        for (Locator contains : reportContains)
        {
            contains.waitForElement(reportDiv, WAIT_FOR_PAGE);
        }

        if (reportNotContains != null)
        {
            String reportText = reportDiv.getText();

            for (String text : reportNotContains)
            {
                assertFalse("Report contained undesired text : " + text, reportText.contains(text));
            }
        }


        return reportDiv;
    }
}
