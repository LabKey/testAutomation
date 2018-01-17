/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: tgaluhn
 * Date: 9/20/2017
 *
 * Split helper methods and tests from KnitrReportTest so a subset of those test cases can be run in RSandboxTest
 */
public abstract class AbstractKnitrReportTest extends BaseWebDriverTest
{
    protected static final Path scriptpadReports = Paths.get(TestFileUtils.getLabKeyRoot(), "server/test/modules/scriptpad/resources/reports/schemas");
    protected static final Path rmdReport = scriptpadReports.resolve("script_rmd.rmd");
    private static final Path rhtmlReport = scriptpadReports.resolve("script_rhtml.rhtml");
    protected final RReportHelper _rReportHelper = new RReportHelper(this);

    private static String readReport(final Path reportFile)
    {
        String reportSource;

        reportSource = TestFileUtils.getFileContents(reportFile);

        assertTrue("No data in report file [" + reportFile.getFileName() + "]", reportSource.length() > 0);

        return reportSource;
    }

    @BeforeClass
    public static void initProject()
    {
        AbstractKnitrReportTest init = (AbstractKnitrReportTest)getCurrentTest();
        init.setupProject();
    }

    @LogMethod
    protected void setupProject()
    {
        _rReportHelper.ensureRConfig(isDocker());

        _containerHelper.createProject(getProjectName(), "Collaboration");
        _containerHelper.enableModule(getProjectName(), "scriptpad");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Data Views");
    }

    protected boolean isDocker()
    {
        return false;
    }

    protected String createKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption)
    {
        String reportSource = readReport(reportSourcePath);

        goToProjectHome();
        goToManageViews();

        BootstrapMenu.find(getDriver(),"Add Report").clickSubMenu(true,"R Report");
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
        assertTrue("Incorrect number of lines present in code editor.", _rReportHelper.isReportSourceLineCountMatch(expectedLineCount));

        return saveAndVerifyKnitrReport(reportName, reportContains, reportNotContains);
    }

    protected void setPandocEnabled(boolean useRmarkdownV2)
    {
        _rReportHelper.setPandocEnabled(useRmarkdownV2);
    }

    protected WebElement saveAndVerifyKnitrReport(String reportName, Locator[] reportContains, String[] reportNotContains)
    {
        _rReportHelper.saveReport(reportName);
        return _rReportHelper.assertKnitrReportContents(reportContains, reportNotContains);
    }

    protected void htmlFormat()
    {
        Locator[] reportContains = {Locator.tag("p").withText("This is a minimal example which shows knitr working with HTML pages in LabKey."),
                                    Locator.tag("img").withAttribute("title", "plot of chunk blood-pressure-scatter"),
                                    Locator.tag("pre").containing("## \"1\",249318596,\"2008-05-17\",86,36,129,76,64,17,0,\"false\",\"English\",\"urn:lsid:labkey.com:Study.Data-2156:5004.249318596.20080517.0000\""),
                                    Locator.tag("pre").withText("## knitr says hello to HTML!"),
                                    Locator.tag("pre").startsWith("## Error").containing(": non-numeric argument to binary operator"),
                                    Locator.tag("p").startsWith("Well, everything seems to be working. Let's ask R what is the value of \u03C0? Of course it is 3.141")};
        String[] reportNotContains = {"<html>",                          // Uninterpreted html
                                      "<!--",                            // ditto
                                      "A minimal knitr example in HTML", // report title element
                                      "begin.rcode",                     // knitr commands shouldn't be visible
                                      "opts_chunk"};                     // Un-echoed R code

        createAndVerifyKnitrReport(rhtmlReport, RReportHelper.ReportOption.knitrHtml, reportContains, reportNotContains, false);
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
                Locator.tag("sup").withText("write") //should not contain the hat markdown v2 closing tag
        };

        String[] reportNotContains = {"```",              // Markdown for R code chunks
                "## R code chunks", // Uninterpreted Markdown
                "{r",               // Markdown for R code chunks
                "data_means"};      // Non-echoed R code

        createAndVerifyKnitrReport(rmdReport, RReportHelper.ReportOption.knitrMarkdown, reportContains, reportNotContains, true, rmdReport.getFileName() + "MarkdownV2");
    }

    protected void moduleReportDependencies()
    {
        //
        // Checks that the dependencies can be loaded from the included kable report's metadata file.
        // If the dependencies did not load correctly then the test will fail with an
        // UnhandledAlertException when trying to view this report in the report designer
        //
        setPandocEnabled(true);
        clickProject(getProjectName());
        _ext4Helper.waitForMaskToDisappear();
        waitAndClickAndWait(Locator.linkWithText("kable"));
        _ext4Helper.waitForMaskToDisappear();
        waitForElement(Locator.id("mtcars_table_wrapper"));
    }
}
