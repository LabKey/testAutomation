/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.test.tests;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;
import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, Reports.class})
public class KnitrReportTest extends ReportTest
{
    private static final Path scriptpadReports = Paths.get(TestFileUtils.getLabKeyRoot(), "server/test/modules/scriptpad/resources/reports/schemas");
    private static final Path rhtmlReport = scriptpadReports.resolve("script_rhtml.rhtml");
    private static final Path rmdReport = scriptpadReports.resolve("script_rmd.rmd");
    private static final Path rmdDependenciesReport = scriptpadReports.resolve("kable.rmd");
    private final RReportHelper _rReportHelper = new RReportHelper(this);

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "KnitrReportProject";
    }

    @Override
    protected void doCreateSteps()
    {
        setupProject();
    }

    @Override
    protected void doVerifySteps()
    {
        verifyKnitrHTMLFormat();
        verifyKnitrMarkupFormat();
        verifyModuleReportDependencies();
        verifyAdhocReportDependenciesString();
        verifyAdhocReportDependenciesLib();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), "Collaboration");
        _containerHelper.enableModule(getProjectName(), "scriptpad");

        PortalHelper portalHelper = new PortalHelper(this);

        portalHelper.addReportWebPart("script_rmd");
        portalHelper.addWebPart("Data Views");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyKnitrHTMLFormat()
    {
        Locator[] reportContains = {Locator.tag("p").withText("This is a minimal example which shows knitr working with HTML pages in LabKey."),
                                    Locator.tag("img").withAttribute("title", "plot of chunk blood-pressure-scatter"),
                                    Locator.tag("pre").containing("## \"1\",249318596,\"2008-05-17\",86,36,129,76,64,17,0,\"false\",\"English\",\"urn:lsid:labkey.com:Study.Data-2156:5004.249318596.20080517.0000\""),
//                                    Locator.css("span.functioncall").withText("message"),
                                    Locator.tag("pre").withText("## knitr says hello to HTML!"),
                                    Locator.tag("pre").startsWith("## Error").containing(": non-numeric argument to binary operator"),
                                    Locator.tag("p").startsWith("Well, everything seems to be working. Let's ask R what is the value of \u03C0? Of course it is 3.141")};
        String[] reportNotContains = {"<html>",                          // Uninterpreted html
                                      "<!--",                            // ditto
                                      "A minimal knitr example in HTML", // report title element
                                      "begin.rcode",                     // knitr commands shouldn't be visible
                                      "opts_chunk"};                     // Un-echoed R code

        createAndVerifyKnitrReport(rhtmlReport, RReportHelper.ReportOption.knitrHtml, reportContains, reportNotContains);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyKnitrMarkupFormat()
    {
        Locator[] reportContains = {Locator.css("h1").withText("A Minimal Example for Markdown"),
                                    Locator.css("h2").withText("R code chunks"),
                                    Locator.css("code.r").containing("set.seed(123)"),       // Echoed R code
                                    Locator.xpath("//img").withAttribute("alt", "plot of chunk graphics"),
                                    //Locator.css("p").withText("Inline R code is also supported, e.g. the value of x is 2, and 2 \u00D7 \u03C0 = 6.2832."),
                                    Locator.css(".MathJax")};
        String[] reportNotContains = {"```",              // Markdown for R code chunks
                                      "## R code chunks", // Uninterpreted Markdown
                                      "{r",               // Markdown for R code chunks
                                      "data_means"};      // Non-echoed R code

        createAndVerifyKnitrReport(rmdReport, RReportHelper.ReportOption.knitrMarkdown, reportContains, reportNotContains);
    }

    private void verifyModuleReportDependencies()
    {
        //
        // Checks that the dependencies can be loaded from the included kable report's metadata file.
        // If the dependencies did not load correctly then the test will fail with an
        // UnhandledAlertException when trying to view this report in the report designer
        //
        clickProject(getProjectName());
        Locator link = getReportGridLink("kable", false);
        waitForElement(link);
        scrollIntoView(link);
        clickAndWait(link);
        _ext4Helper.waitForMaskToDisappear();
    }

    private void verifyAdhocReportDependenciesString()
    {
        verifyAdhocReportDependencies("Strings",
                "http://ajax.aspnetcdn.com/ajax/jquery/jquery-1.9.0.min.js;" +
                "http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/jquery.dataTables.min.js;\r\n" +
                "http://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/css/jquery.dataTables.css"
        );
    }

    private void verifyAdhocReportDependenciesLib()
    {
        //
        // copy over a lib to the webapp path that includes the required dependencies and ensure
        // the report loads by referencing those dependencies
        //
        Path source = Paths.get(TestFileUtils.getSampledataPath(), "knitr/knitr.lib.xml");
        Path destDir = Paths.get(TestFileUtils.getDefaultWebAppRoot());

        try
        {
            Files.copy(source, destDir.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            fail("Failed to copy knitr.lib.xml to [" + destDir.toString() + "]: " + e.getMessage());
        }

        verifyAdhocReportDependencies("ClientLib", "knitr");
    }
    private void verifyAdhocReportDependencies(String viewName, String dependencies)
    {
        // just do a sanity check of the report's contents.  If the dependencies aren't loaded then we'll throw an alert
        Locator[] reportContains = {Locator.css("h1").withText("jQuery DataTables")};
        String[] reportNotContains = {"```", "{r",};
        String expectedError = "ReferenceError: $ is not defined";

        createKnitrReport(rmdDependenciesReport, RReportHelper.ReportOption.knitrMarkdown);

        // Without dependencies, we expect a javascript error.  Behavior is browser dependent
        if (getBrowserType() == BrowserType.FIREFOX)
        {
            // no exception thrown, so look into the JS errors collection
            _rReportHelper.clickViewTab();
            verifyJsErrors(expectedError, true);
        }
        else
        {
            // exception thrown, so knock down the alert
            try
            {
                _rReportHelper.clickViewTab();
                _rReportHelper.clickSourceTab();
            }
            catch(UnhandledAlertException e)
            {
                acceptAllAlerts();
            }
        }

        // now set the dependencies
        _rReportHelper.clickSourceTab();
        _rReportHelper.ensureFieldSetExpanded("knitr");
        setFormElement(Locator.name("scriptDependencies"), dependencies);

        _rReportHelper.clickViewTab();
        if (getBrowserType() == BrowserType.FIREFOX)
        {
            // verify no errors now on FF.  Chrome would have thrown again and we would fail
            // if there were errors on the page
            verifyJsErrors(expectedError, false);
        }

        assertReportContents(reportContains, reportNotContains);
        _rReportHelper.clickSourceTab();
        saveAndVerifyKnitrReport(rmdDependenciesReport.getFileName() + " " + viewName, reportContains, reportNotContains);
    }

    private void verifyJsErrors(String expectedError, boolean shouldBePresent)
    {
        if (TestProperties.isScriptCheckEnabled())
        {
            try
            {
                boolean foundError = false;
                List<JavaScriptError> jsErrors = JavaScriptError.readErrors(getDriver());
                for (JavaScriptError j : jsErrors)
                {
                    String msg = j.getErrorMessage();
                    if (null != msg)
                    {
                        log("found JS error: " + msg);
                        if (msg.contains(expectedError))
                        {
                            foundError = true;
                            break;
                        }
                    }
                }

                if (shouldBePresent)
                    assertTrue("expected JS jquery reference error not found!", foundError);
                else
                    assertFalse("unexpected JS jquery reference error found!", foundError);
            }
            catch(WebDriverException ex)
            {
                fail("error checker not enabled!");
            }
        }
    }

    private String createKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption)
    {
        String reportSource = readReport(reportSourcePath);

        clickProject(getProjectName());
        goToManageViews();

        clickAddReport("R View");
        _rReportHelper.selectOption(knitrOption);
        setCodeEditorValue("script-report-editor", reportSource);
        return reportSource;
    }


    private void createAndVerifyKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains)
    {
        final String reportName = reportSourcePath.getFileName() + " Report";
        String reportSource = createKnitrReport(reportSourcePath, knitrOption);

        // Regression test: Issue #18602
        _rReportHelper.clickViewTab();
        assertReportContents(reportContains, reportNotContains);

        _rReportHelper.clickSourceTab();

        List<WebElement> lines;
        WebElement prevLastLine, lastLine = null;
        long startTime = System.currentTimeMillis();
        do
        {
            prevLastLine = lastLine;
            lines = Locator.css(".CodeMirror-linenumber").findElements(getDriver());
            lastLine = lines.get(lines.size() - 1);
            scrollIntoView(lastLine);
        }while(!lastLine.equals(prevLastLine) && System.currentTimeMillis() - startTime < 1000);

        assertEquals("Incorrect number of lines present in code editor.", reportSource.split("\n").length, Integer.parseInt(lastLine.getText()));

        saveAndVerifyKnitrReport(reportName, reportContains, reportNotContains);
    }

    private void saveAndVerifyKnitrReport(String reportName, Locator[] reportContains, String[] reportNotContains)
    {
        _rReportHelper.saveReport(reportName);
        openView(reportName);
        assertReportContents(reportContains, reportNotContains);
    }

    private void assertReportContents(Locator[] reportContains, String[] reportNotContains)
    {
        Locator reportDiv = Locator.css("div.reportView > div.labkey-knitr");
        waitForElement(reportDiv);
        String reportText = getText(reportDiv);

        for (Locator contains : reportContains)
        {
            waitForElement(contains);
        }

        for (String text : reportNotContains)
        {
            assertFalse("Report contained undesired text : " + text, reportText.contains(text));
        }
    }

    private static String readReport(final Path reportFile)
    {
        String reportSource = null;

        try
        {
            reportSource = new String(Files.readAllBytes(reportFile));
        }
        catch (IOException fail)
        {
            fail("Failed to read report file [" + reportFile.getFileName() + "]: " + fail.getMessage());
        }

        assertTrue("No data in report file [" + reportFile.getFileName() + "]", reportSource.length() > 0);

        return reportSource;
    }

    private void openView(String viewName)
    {
        clickReportDetailsLink(viewName);
        clickAndWait(Locator.linkContainingText("View Report"));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
