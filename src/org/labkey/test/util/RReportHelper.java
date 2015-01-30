/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
import org.labkey.api.util.FileUtil;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.pages.ConfigureReportsAndScriptsHelper;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class RReportHelper extends AbstractHelper
{
    public enum ReportOption {
        shareReport("Make this view available to all users", null, true),
        showSourceTab("Show source tab to all users", null, true),
        runInPipeline("Run this view in the background as a pipeline job", null, true),
        knitrNone("None", "Knitr Options", false),
        knitrHtml("Html", "Knitr Options", false),
        knitrMarkdown("Markdown", "Knitr Options", false);

        public String _label;
        public boolean _isCheckbox;
        public String _fieldSet;

        ReportOption(String label, String fieldSet, boolean isCheckbox)
        {
            _label = label;
            _fieldSet = fieldSet;
            _isCheckbox = isCheckbox;
        }
    }

    public RReportHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    private static File rExecutable = null;
    private static File rScriptExecutable = null;

    private static final String INSTALL_RLABKEY = "install.packages(\"Rlabkey\", repos=\"http://cran.r-project.org\")";
    private static final String INSTALL_LOCAL_RLABKEY = "install.packages(\"%s\", repos=NULL)";

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
    @LogMethod
    public boolean executeScript(String script, String expectedLines, boolean failOnError)
    {
        _test.log("execute script");

        // running a saved script
        _test._ext4Helper.clickTabContainingText("Source");

        _test.setCodeEditorValue("script-report-editor", script);
        _test._ext4Helper.clickTabContainingText("View");
        _test._ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 5);
        _test.waitForElement(Locator.xpath("//table[@class='labkey-output']"), _test.getDefaultWaitForPage());

        Locator l = Locator.xpath("//div[@class='reportView']//pre");
        _test.waitForElement(l);
        String html = _test.getText(l).replaceAll(" +", " ");

        return checkScriptOutput(html, expectedLines, failOnError);
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
                    _test.log("Error: could not find expected text: " + part + ".\nfrom value:\n" + scriptOutput);
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
            _test.log("Error: the script failed with an error:\n" + scriptOutput);
            return true;
        }

        return false;
    }

    /**
     * Installs the latest version of the Rlabkey package that is either built or checked into the ../remoteapi/r/latest
     * directory. In order to install any dependent packages: (RCurl, rjson), an installation from the CRAN repository
     * is performed first, then if the local flag has been set, the local package is installed over the top.
     */
    @LogMethod
    public void installRlabkey(boolean local)
    {
        if (executeScript(INSTALL_RLABKEY, null, true))
        {
            if (local)
            {
                File rPackage = new File(TestFileUtils.getLabKeyRoot(), "/sampledata/rlabkey/Rlabkey.zip");

                if (!rPackage.exists())
                    fail("Unable to locate the local Rlabkey package: " + rPackage.getName());

                String cmd = String.format(INSTALL_LOCAL_RLABKEY, rPackage.getAbsolutePath());
                cmd = cmd.replaceAll("\\\\", "/");
                if (!executeScript(cmd, null, true))
                    fail("Unable to install the local Rlabkey package.");
            }
        }
        else
            fail("Unable to install the base Rlabkey package and dependencies.");
    }

    @LogMethod
    public String ensureRConfig()
    {
        _test.ensureAdminMode();

        _test.goToAdminConsole();
        _test.clickAndWait(Locator.linkWithText("views and scripting"));

        _test.log("Check if R already is configured");

        ConfigureReportsAndScriptsHelper scripts = new ConfigureReportsAndScriptsHelper(_test);

        String defaultScriptName = "R Scripting Engine";
        if (scripts.isEnginePresent("R"))
        {
            if (!TestProperties.isTestRunningOnTeamCity())
            {
                scripts.editEngine(defaultScriptName);
                rExecutable = new File(_test.getFormElement(Locator.id("editEngine_exePath")));
                return getRVersion(rExecutable);
            }
            else // Reset R scripting engine on TeamCity
                scripts.deleteEngine(defaultScriptName);
        }

        _test.log("Try configuring R");
        String rVersion = getRVersion(getRExecutable());

        ConfigureReportsAndScriptsHelper.EngineConfig config = new ConfigureReportsAndScriptsHelper.EngineConfig(getRExecutable());
        config.setVersion(rVersion);
        scripts.addEngine(ConfigureReportsAndScriptsHelper.EngineType.R, config);

        return rVersion;
    }

    private File getRExecutable()
    {
        if (rExecutable != null)
            return rExecutable;

        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            _test.log("R_HOME is set to: " + rHome + " searching for the R application");
            File rHomeDir = new File(rHome);
            FileFilter rFilenameFilter = new FileFilter()
            {
                public boolean accept(File file)
                {
                    return ("r.exe".equalsIgnoreCase(file.getName()) || "r".equalsIgnoreCase(file.getName())) && file.canExecute();
                }
            };
            File[] files = rHomeDir.listFiles(rFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(rHome, "bin").listFiles(rFilenameFilter);
            }

            if (files != null)
            {
                if (files.length == 1)
                {
                    rExecutable = files[0];
                    return rExecutable;
                }
                else if (files.length > 1)
                {
                    _test.log("Found too many R executables:");
                    for (File file : files)
                    {
                        _test.log("\t" + file.getAbsolutePath());
                    }
                }
            }
        }

        _test.log("Environment info: " + System.getenv());

        if (null == rHome)
        {
            _test.log("");   // Blank line helps make the following message more readable
            _test.log("R_HOME environment variable is not set.  Set R_HOME to your R bin directory to enable automatic configuration.");
        }
        fail("R is not configured on this system. Failed R tests.");

        return null; // Unreachable
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
            versionOutput = TestFileUtils.getProcessOutput(r, "--version");

            Pattern versionPattern = Pattern.compile("R version ([1-9]\\.\\d+\\.\\d)");
            Matcher matcher = versionPattern.matcher(versionOutput);
            matcher.find();
            String versionNumber = matcher.group(1);

            _test.log("R --version > " + versionNumber);

            return versionNumber;
        }
        catch(IOException ex)
        {
            if (versionOutput.length() > 0) _test.log("R --version > " + versionOutput);
            fail("Unable to determine R version: " + r.getAbsolutePath() + " due to " + ex.getMessage());
            return null; // Unreachable
        }
    }

    public String getRScriptOutput(String scriptContents) throws IOException
    {
        return TestFileUtils.getProcessOutput(getRScriptExecutable(), "-e", scriptContents);
    }

    public String getRScriptOutput(File script) throws IOException
    {
        return TestFileUtils.getProcessOutput(getRScriptExecutable(), FileUtil.getAbsoluteCaseSensitiveFile(script).getAbsolutePath());
    }

    public void saveReport(String name)
    {
        _test.clickButton("Save", 0);

        if (null != name)
        {
            Locator locator = Ext4Helper.Locators.window("Save View").append(Locator.xpath("//input[contains(@class, 'x4-form-field')]"));
            if (_test.isElementPresent(locator))
            {
                _test.setFormElement(locator, name);
                _test._ext4Helper.clickWindowButton("Save View", "OK", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, 0);
            }
        }
    }

    public void selectOption(ReportOption option)
    {
        ensureFieldSetExpanded(option._fieldSet);
        if (option._isCheckbox)
        {
            Locator checkbox = Ext4Helper.Locators.checkbox(_test, option._label);
            _test.waitForElement(checkbox);
            _test._ext4Helper.checkCheckbox(option._label);
        }
        else
        {
            Locator checkbox = Ext4Helper.Locators.radiobutton(_test, option._label);
            _test.waitForElement(checkbox);
            _test._ext4Helper.selectRadioButton(option._label);
        }
    }

    public void clickViewTab()
    {
        _test._ext4Helper.clickTabContainingText("View");
        _test.waitForElement(Locator.tagWithClass("div", "reportView").notHidden().withPredicate("not(ancestor-or-self::*[contains(@class,'mask')])"), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickSourceTab()
    {
        _test._ext4Helper.clickTabContainingText("Source");
        _test.waitForElement(Locator.tagWithClass("div", "reportSource").notHidden(), BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void ensureFieldSetExpanded(String name)
    {
        if (name != null)
        {
            Locator fieldSet = Locator.xpath("//fieldset").withClass("x4-fieldset-collapsed").withDescendant(Locator.xpath("//div").withClass("x4-fieldset-header-text").containing(name)).append("//div/img");

            if (_test.isElementPresent(fieldSet))
            {
                _test.click(fieldSet);
            }
        }
    }
}
