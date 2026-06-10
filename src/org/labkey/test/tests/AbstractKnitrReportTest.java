/*
 * Copyright (c) 2017-2026 LabKey Corporation
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

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.pages.admin.ExternalSourcesPage;
import org.labkey.test.pages.core.admin.logger.ManagerPage;
import org.labkey.test.pages.reports.ManageViewsPage;
import org.labkey.test.pages.reports.ScriptReportPage;
import org.labkey.test.pages.reports.ScriptReportPage.StandardReportOption;
import org.labkey.test.util.CodeMirrorHelper;
import org.labkey.test.util.CspLogUtil;
import org.labkey.test.util.Log4jUtils;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.core.admin.CspConfigHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Split helper methods and tests from KnitrReportTest so a subset of those test cases can be run in RSandboxTest
 */
public abstract class AbstractKnitrReportTest extends BaseWebDriverTest
{
    protected static final Path scriptpadReports = new File(TestFileUtils.getTestRoot(), "modules/scriptpad/resources/reports/schemas").toPath();
    protected static final Path rmdReport = scriptpadReports.resolve("script_rmd.rmd");
    protected static final Path rmdReport_no_scriptpad = TestFileUtils.getSampleData("reports/knitr_no_scriptpad.rmd").toPath();
    private static final Path rhtmlReport = scriptpadReports.resolve("script_rhtml.rhtml");
    private static final Path rhtmlReport_no_scriptpad = TestFileUtils.getSampleData("reports/knitr_no_scriptpad.rhtml").toPath();
    private static final Path rhtmlNonceCheck = TestFileUtils.getSampleData("reports/nonce_check.rhtml").toPath();
    private static final Locator.XPathLocator nonceCheckLoc = Locator.id("nonce-check-result");
    private static final Locator.XPathLocator nonceCheckSuccessLoc = nonceCheckLoc.withText("SUCCESS");

    protected final RReportHelper _rReportHelper = new RReportHelper(this);

    private static String readReport(final Path reportFile)
    {
        String reportSource;

        reportSource = TestFileUtils.getFileContents(reportFile);

        assertFalse("No data in report file [" + reportFile.getFileName() + "]", reportSource.isEmpty());

        return reportSource;
    }

    @BeforeClass
    public static void initProject() throws Exception
    {
        AbstractKnitrReportTest init = getCurrentTest();
        init.setupProject();
    }

    @LogMethod
    protected void setupProject()
    {
        Log4jUtils.setLogLevel("org.labkey.query.reports.ReportsController", ManagerPage.LoggingLevel.DEBUG);
        try
        {
            new CspConfigHelper(this).setAllowedHosts(Map.of(
                ExternalSourcesPage.Directive.Object, List.of("'self'"), // Issue 53226: reports-streamFile is blocked by object-src CSP directive
                ExternalSourcesPage.Directive.Style, List.of("https://cdn.datatables.net"),
                ExternalSourcesPage.Directive.Font, List.of("https://mathjax.rstudio.com")));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        _rReportHelper.ensureRConfig(isDocker());

        _containerHelper.createProject(getProjectName(), "Collaboration");
        if (!TestProperties.isWithoutTestModules())
        {
            _containerHelper.enableModule(getProjectName(), "scriptpad");
        }

        new PortalHelper(this).doInAdminMode(portalHelper -> {
            portalHelper.removeAllWebParts();
            portalHelper.addWebPart("Data Views");
        });
    }

    protected boolean isDocker()
    {
        return false;
    }

    @LogMethod
    protected String createKnitrReport(@LoggedParam Path reportSourcePath, @LoggedParam RReportHelper.ReportOption knitrOption)
    {
        String reportSource = readReport(reportSourcePath);

        goToProjectHome();
        ManageViewsPage manageViewsPage = goToManageViews();

        manageViewsPage.clickAddReport("R Report");
        _rReportHelper.selectOption(knitrOption);
        setCodeEditorValue("script-report-editor", reportSource);
        return reportSource;
    }

    protected WebElement createAndVerifyKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains, boolean useRmarkdownV2)
    {
        return createAndVerifyKnitrReport(reportSourcePath, knitrOption, reportContains, reportNotContains, useRmarkdownV2, reportSourcePath.getFileName() + " Report");
    }

    protected WebElement createAndVerifyKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains, boolean useRmarkdownV2, String reportName)
    {
        setPandocEnabled(useRmarkdownV2);

        String reportSource = createKnitrReport(reportSourcePath, knitrOption);

        // Regression test: Issue #18602
        _rReportHelper.clickReportTab();
        _rReportHelper.assertKnitrReportContents(reportContains, reportNotContains);

        _rReportHelper.clickSourceTab();

        int expectedLineCount = reportSource.split("\n").length;
        assertEquals("Incorrect number of lines present in code editor.", expectedLineCount, new CodeMirrorHelper(this, "script-report-editor").getLineCount());

        return saveAndVerifyKnitrReport(reportName, reportContains, reportNotContains);
    }

    protected void setPandocEnabled(boolean enable)
    {
        _rReportHelper.setPandocEnabled(RReportHelper.LOCAL_R_ENGINE, enable);
    }

    protected WebElement saveAndVerifyKnitrReport(String reportName, Locator[] reportContains, String[] reportNotContains)
    {
        _rReportHelper.saveReport(reportName);
        return _rReportHelper.assertKnitrReportContents(reportContains, reportNotContains);
    }

    protected void htmlFormat()
    {
        Locator[] reportContains = {Locator.tag("p").withText("This is a minimal example which shows knitr working with HTML pages in LabKey."),
                                    Locator.XPathLocator.union(
                                            // Image output varies slightly between versions (not sure if it's pandoc or some R package(s))
                                            Locator.tag("img").withAttribute("title", "plot of chunk blood-pressure-scatter"), // old
                                            Locator.tag("img").withAttribute("alt", "plot of chunk blood-pressure-scatter")), // new
                                    Locator.tag("pre").containing("## \"1\",249318596,\"2008-05-17\",86,36,129,76,64"),
                                    Locator.tag("pre").withText("## knitr says hello to HTML!"),
                                    Locator.tag("pre").startsWith("## Error").containing("non-numeric argument to binary operator"),
                                    Locator.tag("p").startsWith("Well, everything seems to be working. Let's ask R what is the value of \u03C0? Of course it is 3.141"),
                                    nonceCheckSuccessLoc // Inline script should run
        };
        String[] reportNotContains = {"<html>",                          // Uninterpreted html
                                      "<!--",                            // ditto
                                      "A minimal knitr example in HTML", // report title element
                                      "begin.rcode",                     // knitr commands shouldn't be visible
                                      "opts_chunk"};                     // Un-echoed R code

        Path reportSourcePath = TestProperties.isWithoutTestModules() ? rhtmlReport_no_scriptpad : rhtmlReport;
        createAndVerifyKnitrReport(reportSourcePath, RReportHelper.ReportOption.knitrHtml, reportContains, reportNotContains, false);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("reports");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    protected void markdownV2()
    {
        Locator[] reportContains = {Locator.css("h1").withText("A Minimal Example for Markdown"),
                Locator.tag("h2").withText("R code chunks"),
                Locator.tag("code").containing("set.seed(123)"),       // Echoed R code
                Locator.css("p").containing("2 x pi = 6.283"),
                Locator.tag("sup").withText("write"), //should not contain the hat markdown v2 closing tag
                nonceCheckSuccessLoc // Inline script should run
        };

        String[] reportNotContains = {"```",              // Markdown for R code chunks
                "## R code chunks", // Uninterpreted Markdown
                "{r",               // Markdown for R code chunks
                "data_means"};      // Non-echoed R code

        Path reportSourcePath = TestProperties.isWithoutTestModules() ? rmdReport_no_scriptpad : rmdReport;
        createAndVerifyKnitrReport(reportSourcePath, RReportHelper.ReportOption.knitrMarkdown, reportContains,
            reportNotContains, true, reportSourcePath.getFileName() + "MarkdownV2");
    }

    protected void moduleReportDependencies()
    {
        Assume.assumeFalse("Test modules not installed (module report dependencies are provided by scriptpad).",
            TestProperties.isWithoutTestModules());
        //
        // Checks that the dependencies can be loaded from the included kable report's metadata file.
        // If the dependencies did not load correctly then the test will fail with an
        // UnhandledAlertException when trying to view this report in the report designer
        //
        setPandocEnabled(true);
        clickProject(getProjectName());
        _ext4Helper.waitForMaskToDisappear();
        waitAndClickAndWait(Locator.linkWithText("kable"));
        _ext4Helper.waitForMaskToDisappear(60_000);
        waitForElement(Locator.id("mtcars_table"));
    }

    /**
     * Issue 53211: CSP reports when an R/Plotly graph is displayed in Reports web part, same thing wrapped in a wiki works fine with strict csp
     */
    @Test
    public void testEmbeddedReportNonce()
    {
        CspConfigHelper.debugCspWarnings();

        String name = "rhtml nonce check";
        Locator[] reportContains = {nonceCheckSuccessLoc};

        createAndVerifyKnitrReport(rhtmlNonceCheck, RReportHelper.ReportOption.knitrHtml, reportContains,
            null, true, name);
        assertNonceSuccess();

        log("Create wiki with embedded report");
        new WikiHelper(this).createNewWikiPage()
            .setName(name)
            .setBody("""
                ${labkey.webPart(partName='Report',
                    reportName='%s',
                    showFrame='false'
                )}
            """.formatted(name))
            .saveAndClose();
        clickAndWait(Locator.linkWithText(name));
        assertNonceSuccess();

        log("Add report webpart");
        new PortalHelper(this).doInAdminMode(ph -> {
            ph.addTab(name + " tab"); // Use a separate tab to ensure report isn't run accidentally
            ph.addReportWebPart(name);
            assertNonceSuccess();
        });

        log("Re-verify with report run as pipeline job");
        goToManageViews();
        waitAndClickAndWait(Locator.linkWithText(name));
        ScriptReportPage reportPage = _rReportHelper.getReportPage();
        reportPage.selectOption(StandardReportOption.runInPipeline);
        String reportId = reportPage.saveReport(null, false, WAIT_FOR_PAGE);
        goBack();
        reportPage.startPipelineJobAndWait();

        ScriptReportPage.beginAtReport(this, getCurrentContainerPath(), reportId);
        assertNonceSuccess();

        clickTab(name + " tab");
        assertNonceSuccess();

        goToModule("Wiki");
        clickAndWait(Locator.linkWithText(name));
        assertNonceSuccess();
    }

    private void assertNonceSuccess()
    {
        assertEquals("Nonce check result", "SUCCESS", getText(nonceCheckLoc));
        CspLogUtil.checkNewCspWarnings(getArtifactCollector());
    }
}
