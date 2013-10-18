/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.junit.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelperWD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User: tchadick
 * Date: 5/30/13
 * Time: 12:34 PM
 */
@Category({DailyA.class, Reports.class})
public class KnitrReportTest extends ReportTest
{
    private static final Path scriptpadReports = Paths.get(getLabKeyRoot(), "server/test/modules/scriptpad/resources/reports/schemas");
    private static final Path rhtmlReport = scriptpadReports.resolve("script_rhtml.rhtml");
    private static final Path rmdReport = scriptpadReports.resolve("script_rmd.rmd");
    private final RReportHelperWD _rReportHelper = new RReportHelperWD(this);

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
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {
        RReportHelperWD rReportHelper = new RReportHelperWD(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), "Collaboration");
        enableModule(getProjectName(), "scriptpad");

        PortalHelper portalHelper = new PortalHelper(this);
        //portalHelper.addWebPart("Scriptpad");

        portalHelper.addReportWebPart("script_rmd");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyKnitrHTMLFormat()
    {
        Locator[] reportContains = {Locator.tag("p").withText("This is a minimal example which shows knitr working with HTML pages in LabKey."),
                                    Locator.tag("img").withAttribute("title", "plot of chunk blood-pressure-scatter"),
                                    Locator.tag("pre").containing("## \"1\",249318596,\"2008-05-17\",86,36,129,76,64,17,0,\"false\",\"English\",\"urn:lsid:labkey.com:Study.Data-2156:5004.249318596.20080517.0000\""),
//                                    Locator.css("span.functioncall").withText("message"),
                                    Locator.tag("pre").withText("## knitr says hello to HTML!"),
                                    Locator.tag("pre").withText("## Error: non-numeric argument to binary operator"),
                                    Locator.tag("p").withText("Well, everything seems to be working. Let's ask R what is the value of \u03C0? Of course it is 3.1416.")};
        String[] reportNotContains = {"<html>",                          // Uninterpreted html
                                      "<!--",                            // ditto
                                      "A minimal knitr example in HTML", // report title element
                                      "begin.rcode",                     // knitr commands shouldn't be visible
                                      "opts_chunk"};                     // Un-echoed R code

        createAndVerifyKnitrReport(rhtmlReport, RReportHelperWD.ReportOption.knitrHtml, reportContains, reportNotContains);
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

        createAndVerifyKnitrReport(rmdReport, RReportHelperWD.ReportOption.knitrMarkdown, reportContains, reportNotContains);
    }

    private void createAndVerifyKnitrReport(Path reportSourcePath, RReportHelperWD.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains)
    {
        final String reportSource = readReport(reportSourcePath);
        final String reportName = reportSourcePath.getFileName() + " Report";

        clickProject(getProjectName());
        goToManageViews();

        clickAddReport("R View");
        _rReportHelper.selectOption(knitrOption);
        setCodeEditorValue("script-report-editor", reportSource);

        // Regression test: Issue #18602
        _rReportHelper.clickViewTab();
        assertReportContents(reportContains, reportNotContains);

        _rReportHelper.clickSourceTab();
        Assert.assertEquals("Incorrect number of lines present in code editor.", reportSource.split("\n").length, getElementCount(Locator.css(".CodeMirror-gutter-text pre")));
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
            assertElementPresent(contains);
        }

        for (String text : reportNotContains)
        {
            Assert.assertFalse("Report contained undesired text : " + text, reportText.contains(text));
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
            Assert.fail("Failed to read report file [" + reportFile.getFileName() + "]: " + fail.getMessage());
        }

        Assert.assertTrue("No data in report file [" + reportFile.getFileName() + "]", reportSource.length() > 0);

        return reportSource;
    }

    private void openView(String viewName)
    {
        clickReportDetailsLink(viewName);
        clickAndWait(Locator.linkContainingText("View Report"));
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
