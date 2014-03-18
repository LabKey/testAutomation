/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class RReportHelper extends AbstractHelperWD
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
    public boolean executeScript(String script, String verify, boolean failOnError)
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

        if (failOnError)
        {
            if (html.contains("javax.script.ScriptException"))
            {
                _test.log("Error: the script failed with an error:\n" + html);
                return false;
            }
        }

        if (!StringUtils.isEmpty(verify))
        {
            // split string on newlines to make the comparison more reliable
            String [] parts = verify.split("\n");

            for (String part : parts)
            {
                if (!html.contains(part.trim()))
                {
                    _test.log("Error: could not find expected text: " + part + ".\nfrom value:\n" + html);
                    return false;
                }
            }
        }

        return true;
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
                File rPackage = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/rlabkey/Rlabkey.zip");

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

        try
        {
            if (_test.isREngineConfigured())
                if(TestProperties.isTestRunningOnTeamCity())
                {
                    _test.doubleClick(Locator.css("div.x-grid3-col-0").containing("R Scripting Engine"));
                    return getRVersion(new File(_test.getFormElement(Locator.id("editEngine_exePath"))));
                }
                else // Reset R scripting engine on TeamCity
                    deleteEngine();
        }
        catch (SeleniumException e)
        {
            _test.log("Ignoring Selenium Error");
            _test.log(e.getMessage());
        }

        _test.log("Try configuring R");
        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            _test.log("R_HOME is set to: " + rHome + " searching for the R application");
            File rHomeDir = new File(rHome);
            FilenameFilter rFilenameFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return "r.exe".equalsIgnoreCase(name) || "r".equalsIgnoreCase(name);
                }
            };
            File[] files = rHomeDir.listFiles(rFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(rHome, "bin").listFiles(rFilenameFilter);
            }

            if (files != null)
            {
                for (File file : files)
                {
                    if (file.canExecute() )
                    {
                        // add a new r engine configuration
                        String id = _test._extHelper.getExtElementId("btn_addEngine");
                        _test.click(Locator.id(id));
    
                        id = _test._extHelper.getExtElementId("add_rEngine");
                        _test.click(Locator.id(id));

                        id = _test._extHelper.getExtElementId("btn_submit");
                        _test.waitForElement(Locator.id(id), 10000);

                        id = _test._extHelper.getExtElementId("editEngine_exePath");
                        _test.setFormElement(Locator.id(id), file.getAbsolutePath());

                        String rVersion = getRVersion(file);
                        _test.setFormElement(Locator.id("editEngine_languageVersion"), rVersion);

                        id = _test._extHelper.getExtElementId("btn_submit");
                        _test.click(Locator.id(id));

                        // wait until the dialog has been dismissed
                        int cnt = 3;
                        while (_test.isElementPresent(Locator.id(id)) && cnt > 0)
                        {
                            _test.sleep(1000);
                            cnt--;
                        }

                        _test.waitFor(new BaseWebDriverTest.Checker()
                        {
                            public boolean check()
                            {
                                return _test.isREngineConfigured();
                            }
                        }, "unable to setup the R script engine", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

                        return rVersion;
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
        return null; // unreachable
    }

    private String getRVersion(File r)
    {
        String versionOutput = "";
        try
        {
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(new String[]{r.getCanonicalPath(), "--version"});

            // Different platforms output version info differently; just combine all std/err output
            versionOutput = BaseWebDriverTest.getStreamContentsAsString(p.getInputStream());
            versionOutput += BaseWebDriverTest.getStreamContentsAsString(p.getErrorStream());

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

    @LogMethod
    public void deleteEngine()
    {
        if (_test.isREngineConfigured())
        {
            Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='R,r']");
            _test.click(engine);

            String id = _test._extHelper.getExtElementId("btn_deleteEngine");
            _test.click(Locator.id(id));

            _test._extHelper.waitForExtDialog("Delete Engine Configuration", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

            _test.clickButton("Yes", 0);
            _test.waitForElementToDisappear(engine, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void saveReport(String name)
    {
        _test.clickButton("Save", 0);

        if (null != name)
        {
            Locator locator = Ext4HelperWD.Locators.window("Save View").append(Locator.xpath("//input[contains(@class, 'x4-form-field')]"));
            if (_test.isElementPresent(locator))
            {
                _test.setFormElement(locator, name);
                _test._ext4Helper.clickWindowButton("Save View", "OK", _test.WAIT_FOR_JAVASCRIPT, 0);
            }
        }
    }

    public void selectOption(ReportOption option)
    {
        ensureFieldSetExpanded(option._fieldSet);
        if (option._isCheckbox)
        {
            Locator checkbox = Ext4HelperWD.Locators.checkbox(_test, option._label);
            _test.waitForElement(checkbox);
            _test._ext4Helper.checkCheckbox(option._label);
        }
        else
        {
            Locator checkbox = Ext4HelperWD.Locators.radiobutton(_test, option._label);
            _test.waitForElement(checkbox);
            _test._ext4Helper.selectRadioButton(option._label);
        }
    }

    public void clickViewTab()
    {
        clickDesignerTab("View");
    }

    public void clickSourceTab()
    {
        clickDesignerTab("Source");
    }

    public void clickDesignerTab(String name)
    {
        _test._ext4Helper.clickTabContainingText(name);
        _test.sleep(2000); // TODO
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
