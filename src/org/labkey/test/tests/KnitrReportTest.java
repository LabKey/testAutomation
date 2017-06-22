/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyA.class, Reports.class})
public class KnitrReportTest extends BaseWebDriverTest
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
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        if (afterTest)
            revertLibXml();
        else
            deleteLibXml();
    }

    @BeforeClass
    public static void initProject()
    {
        KnitrReportTest init = (KnitrReportTest)getCurrentTest();
        init.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), "Collaboration");
        _containerHelper.enableModule(getProjectName(), "scriptpad");

        PortalHelper portalHelper = new PortalHelper(this);

//        portalHelper.addReportWebPart("script_rmd");
        portalHelper.addWebPart("Data Views");
    }

    @Test
    public void testKnitrHTMLFormat()
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

    @Test
    public void testKnitrMarkupFormat() throws Exception
    {
        Locator.CssLocator plotLocator = Locator.css("img[alt='plot of chunk graphics']");
        Locator[] reportContains = {Locator.tag("h1").withText("A Minimal Example for Markdown"),
                                    Locator.tag("h2").withText("R code chunks"),
                                    Locator.tagWithClass("code", "r").containing("set.seed(123)"),       // Echoed R code
                                    plotLocator,
                                    //Locator.css("p").withText("Inline R code is also supported, e.g. the value of x is 2, and 2 \u00D7 \u03C0 = 6.2832."),
                                    //Locator.css(".MathJax, .mathjax")
                                    Locator.tag("sup").withText("write^") //should contain the hat markdown v2 closing tag
        };
        String[] reportNotContains = {"```",              // Markdown for R code chunks
                                      "## R code chunks", // Uninterpreted Markdown
                                      "{r",               // Markdown for R code chunks
                                      //"propto",           // MathJax source
                                      "data_means"};      // Non-echoed R code

        createAndVerifyKnitrReport(rmdReport, RReportHelper.ReportOption.knitrMarkdown, reportContains, reportNotContains);
        assertEquals("Knitr report failed to display plot", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(plotLocator.findElement(getDriver()).getAttribute("src")).getResponseCode());
    }

    @Test
    public void testModuleReportDependencies()
    {
        //
        // Checks that the dependencies can be loaded from the included kable report's metadata file.
        // If the dependencies did not load correctly then the test will fail with an
        // UnhandledAlertException when trying to view this report in the report designer
        //
        clickProject(getProjectName());
        _ext4Helper.waitForMaskToDisappear();
        waitAndClickAndWait(Locator.linkWithText("kable"));
        _ext4Helper.waitForMaskToDisappear();
        waitForElement(Locator.id("mtcars_table_wrapper"));
    }

    @Test
    public void testAdhocReportDependenciesString()
    {
        deleteLibXml();
        verifyAdhocReportDependencies("Strings",
                "https://ajax.aspnetcdn.com/ajax/jquery/jquery-1.9.0.min.js;" +
                "https://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/jquery.dataTables.min.js;\r\n" +
                "https://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/css/jquery.dataTables.css"
        );
    }

    @Test
    public void testAdhocReportDependenciesLib()
    {
        copyLibXml();

        verifyAdhocReportDependencies("ClientLib", "knitr");
    }

    @Test
    public void testRmarkdownV2Support() throws Exception
    {
        Locator[] reportContains = {Locator.css("h1").withText("A Minimal Example for Markdown"),
                Locator.tag("h2").withText("R code chunks"),
                Locator.tag("code").containing("set.seed(123)"),       // Echoed R code
                Locator.tag("sup").withText("write") //should not contain the hat markdown v2 closing tag
        };

        String[] reportNotContains = {"```",              // Markdown for R code chunks
                "## R code chunks", // Uninterpreted Markdown
                "{r",               // Markdown for R code chunks
                "data_means"};      // Non-echoed R code

        createAndVerifyKnitrReport(rmdReport, RReportHelper.ReportOption.knitrMarkdown, reportContains, reportNotContains, true, rmdReport.getFileName() + "MarkdownV2");
    }

    final Path libXmlSource = Paths.get(TestFileUtils.getSampleData("knitr/knitr.lib.xml").toURI());
    final Path libXmlDest = Paths.get(TestFileUtils.getDefaultWebAppRoot()).resolve(libXmlSource.getFileName());
    final Path libXmlBackup = libXmlDest.resolveSibling(libXmlDest.getFileName() + ".bak");

    private void copyLibXml()
    {
        //
        // copy over a lib to the webapp path that includes the required dependencies and ensure
        // the report loads by referencing those dependencies
        //

        try
        {
            backupLibXml();

            Files.copy(libXmlSource, libXmlDest, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void deleteLibXml()
    {
        try
        {
            backupLibXml();

            Files.deleteIfExists(libXmlDest);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void backupLibXml() throws IOException
    {
        if (libXmlDest.toFile().exists() && !libXmlBackup.toFile().exists())
            Files.move(libXmlDest, libXmlBackup, StandardCopyOption.ATOMIC_MOVE);
    }

    private void revertLibXml()
    {
        try
        {
            if (libXmlBackup.toFile().exists())
                Files.move(libXmlBackup, libXmlDest, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void verifyAdhocReportDependencies(String viewName, String dependencies)
    {
        // just do a sanity check of the report's contents.  If the dependencies aren't loaded then we'll throw an alert
        Locator[] reportContains = {Locator.css("h1").withText("jQuery DataTables")};
        String[] reportNotContains = {"```", "{r",};
        String expectedError = "$ is not";

        createKnitrReport(rmdDependenciesReport, RReportHelper.ReportOption.knitrMarkdown);

        pauseJsErrorChecker(); // Don't fail due to "$ is not a function"
        {
            click(Ext4Helper.Locators.tab("Report"));
            waitForElement(Locator.id("mtcars_table"));
            assertElementNotPresent(Locator.id("mtcars_table_wrapper")); // Created by jQuery
        }
        resumeJsErrorChecker();

        _rReportHelper.clickSourceTab();

        // now set the dependencies
        _rReportHelper.clickSourceTab();
        _rReportHelper.ensureFieldSetExpanded("knitr");
        setFormElement(Locator.name("scriptDependencies"), dependencies);

        _rReportHelper.clickReportTab();
        waitForElement(Locator.id("mtcars_table_wrapper"));

        assertReportContents(reportContains, reportNotContains);
        _rReportHelper.clickSourceTab();
        saveAndVerifyKnitrReport(rmdDependenciesReport.getFileName() + " " + viewName, reportContains, reportNotContains);
    }

    private String createKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption)
    {
        String reportSource = readReport(reportSourcePath);

        clickProject(getProjectName());
        goToManageViews();

        _extHelper.clickExtMenuButton(true, Locator.linkContainingText("Add Report"), "R Report");
        _rReportHelper.selectOption(knitrOption);
        setCodeEditorValue("script-report-editor", reportSource);
        return reportSource;
    }

    private WebElement createAndVerifyKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains)
    {
        return createAndVerifyKnitrReport(reportSourcePath, knitrOption, reportContains, reportNotContains, false, reportSourcePath.getFileName() + " Report");
    }

    private WebElement createAndVerifyKnitrReport(Path reportSourcePath, RReportHelper.ReportOption knitrOption, Locator[] reportContains, String[] reportNotContains, boolean useRmarkdownV2, String reportName)
    {
        _rReportHelper.setPandocEnabled(useRmarkdownV2);

        String reportSource = createKnitrReport(reportSourcePath, knitrOption);

        // Regression test: Issue #18602
        _rReportHelper.clickReportTab();
        assertReportContents(reportContains, reportNotContains);

        _rReportHelper.clickSourceTab();

        int expectedLineCount = reportSource.split("\n").length;
        Locator lastLineLoc = Locator.css(".CodeMirror-code > div:last-of-type .CodeMirror-linenumber");
        WebElement lastLine = lastLineLoc.findElement(getDriver());
        int lineCount = Integer.parseInt(lastLine.getText());

        if (lineCount < expectedLineCount)
        {
            WebElement codeEditorDiv = Locator.css(".CodeMirror-scroll").findElement(getDriver());
            executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", codeEditorDiv);
            shortWait().until(ExpectedConditions.stalenessOf(lastLine));
            lastLine = lastLineLoc.findElement(getDriver());
            lineCount = Integer.parseInt(lastLine.getText());
        }

        assertEquals("Incorrect number of lines present in code editor.", expectedLineCount, lineCount);

        return saveAndVerifyKnitrReport(reportName, reportContains, reportNotContains);
    }

    private WebElement saveAndVerifyKnitrReport(String reportName, Locator[] reportContains, String[] reportNotContains)
    {
        _rReportHelper.saveReport(reportName);
        waitAndClickAndWait(Locator.linkContainingText(reportName));
        return assertReportContents(reportContains, reportNotContains);
    }

    private WebElement assertReportContents(Locator[] reportContains, String[] reportNotContains)
    {
        WebElement reportDiv = waitForElement(Locator.css("div.reportView > div.labkey-knitr"));

        for (Locator contains : reportContains)
        {
            contains.waitForElement(reportDiv, BaseWebDriverTest.WAIT_FOR_PAGE);
        }

        String reportText = reportDiv.getText();

        for (String text : reportNotContains)
        {
            assertFalse("Report contained undesired text : " + text, reportText.contains(text));
        }

        return reportDiv;
    }

    private static String readReport(final Path reportFile)
    {
        String reportSource;

        reportSource = TestFileUtils.getFileContents(reportFile);

        assertTrue("No data in report file [" + reportFile.getFileName() + "]", reportSource.length() > 0);

        return reportSource;
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
}
