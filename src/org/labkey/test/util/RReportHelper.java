/*
 * Copyright (c) 2010-2012 LabKey Corporation
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
import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.io.FilenameFilter;

/**
 * User: klum
 * Date: Mar 9, 2010
 * Time: 2:54:08 PM
 */
public class RReportHelper
{
    private static final String INSTALL_RLABKEY = "install.packages(\"Rlabkey\", repos=\"http://cran.r-project.org\")";
    private static final String INSTALL_LOCAL_RLABKEY = "install.packages(\"%s\", repos=NULL)";

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    public static boolean executeScript(BaseSeleniumWebTest test, String script, String verify)
    {
        return executeScript(test, script, verify, false);
    }

    /**
     * Execute an R script and verify the specified text is present.
     * @return - true if the test result was present
     */
    public static boolean executeScript(BaseSeleniumWebTest test, String script, String verify, boolean failOnError)
    {
        test.log("execute script");

        // running a saved script
        if (!test.isLinkPresentWithText("Download input data") && test.isLinkPresentWithText("Source"))
        {
            ExtHelper.clickExtTab(test, "Source");
        }

        test.setQueryEditorValue("script", script);
        ExtHelper.clickExtTab(test, "View");
        ExtHelper.waitForLoadingMaskToDisappear(test, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT * 5);
        test.waitForElement(Locator.xpath("//table[@class='labkey-output']"), test.getDefaultWaitForPage());

        Locator l = Locator.xpath("//div[@id='viewDiv']//pre");
        String html = test.getText(l);

        if (failOnError)
        {
            if (html.contains("javax.script.ScriptException"))
            {
                test.log("Error: the script failed with an error:\n" + html);
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
                    test.log("Error: could not find expected text: " + part + ".\nfrom value:\n" + html);
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
    public static void installRlabkey(BaseSeleniumWebTest test, boolean local)
    {
        if (executeScript(test, INSTALL_RLABKEY, null, true))
        {
            if (local)
            {
                File rPackage = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/rlabkey/Rlabkey.zip");

                if (!rPackage.exists())
                    Assert.fail("Unable to locate the local Rlabkey package: " + rPackage.getName());

                String cmd = String.format(INSTALL_LOCAL_RLABKEY, rPackage.getAbsolutePath());
                cmd = cmd.replaceAll("\\\\", "/");
                if (!executeScript(test, cmd, null, true))
                    Assert.fail("Unable to install the local Rlabkey package.");
            }
        }
        else
            Assert.fail("Unable to install the base Rlabkey package and dependencies.");
    }

    public static boolean ensureRConfig(final BaseSeleniumWebTest test)
    {
        test.ensureAdminMode();
        // user need to be added to the site develpers group
        // createSiteDeveloper(PasswordUtil.getUsername());

        test.goToAdminConsole();
        test.clickLinkWithText("views and scripting");
        test.log("Check if R already is configured");

        try
        {
            if (test.isREngineConfigured())
                if(System.getProperty("teamcity.buildType.id") == null)
                    return true;
                else // Reset R scripting engine on TeamCity
                    deleteEngine(test);        
        }
        catch (SeleniumException e)
        {
            test.log("Ignoring Selenium Error");
            test.log(e.getMessage());
        }

        test.log("Try configuring R");
        String rHome = System.getenv("R_HOME");
        if (rHome != null)
        {
            test.log("R_HOME is set to: " + rHome + " searching for the R application");
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
                    if (file.isFile() )
                    {
                        // add a new r engine configuration
                        String id = ExtHelper.getExtElementId(test, "btn_addEngine");
                        test.click(Locator.id(id));
    
                        id = ExtHelper.getExtElementId(test, "add_rEngine");
                        test.click(Locator.id(id));

                        id = ExtHelper.getExtElementId(test, "btn_submit");
                        test.waitForElement(Locator.id(id), 10000);

                        id = ExtHelper.getExtElementId(test, "editEngine_exePath");
                        test.setFormElement(Locator.id(id), file.getAbsolutePath());

                        id = ExtHelper.getExtElementId(test, "btn_submit");
                        test.click(Locator.id(id));

                        // wait until the dialog has been dismissed
                        int cnt = 3;
                        while (test.isElementPresent(Locator.id(id)) && cnt > 0)
                        {
                            test.sleep(1000);
                            cnt--;
                        }

                        test.waitFor(new BaseSeleniumWebTest.Checker()
                        {
                            public boolean check()
                            {
                                return test.isREngineConfigured();
                            }
                        }, "unable to setup the R script engine", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

                        return true;
                    }
                }
            }
        }

        test.log("Environment info: " + System.getenv());

        if (null == rHome)
        {
            test.log("");   // Blank line helps make the following message more readable
            test.log("R_HOME environment variable is not set.  Set R_HOME to your R bin directory to enable automatic configuration.");
        }
        Assert.fail("R is not configured on this system. Failed R tests.");
        return false;
    }

    public static void deleteEngine(BaseSeleniumWebTest test)
    {
        if (test.isREngineConfigured())
        {
            Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='R,r']");
            test.mouseDown(engine);

            String id = ExtHelper.getExtElementId(test, "btn_deleteEngine");
            test.click(Locator.id(id));

            ExtHelper.waitForExtDialog(test, "Delete Engine Configuration", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

            test.clickNavButton("Yes", 0);
            test.waitForElementToDisappear(engine, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        }
    }

    public static void saveReport(BaseSeleniumWebTest test, String name)
    {
        test.clickLinkWithText("Source", false);
        test.clickNavButton("Save", 0);
        ExtHelper.setExtFormElement(test, name);
        ExtHelper.clickExtButton(test, "Save");
    }
}
