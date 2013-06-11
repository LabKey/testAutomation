package org.labkey.test.tests;

import org.junit.Assert;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelperWD;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User: tchadick
 * Date: 5/30/13
 * Time: 12:34 PM
 */
public class KnitrReportTest extends BaseWebDriverTest
{
    private static final Path scriptpadReports = Paths.get(getLabKeyRoot(), "server/test/modules/scriptpad/resources/reports/schemas");
    private static final Path rhtmlReport = scriptpadReports.resolve("script_rhtml.rhtml");
    private static final Path rmdReport = scriptpadReports.resolve("script_rmd.rmd");

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "KnitrReportProject";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupProject();
        verifyKnitrHTMLFormat();
        verifyKnitrMarkupFormat();
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    private void setupProject()
    {
        RReportHelperWD rReportHelper = new RReportHelperWD(this);
        String rVersion = rReportHelper.ensureRConfig();

//        if (!rVersion.startsWith("3"))
//        {
//            Assert.fail("Knitr reports require R v3. Found: " + rVersion);
//        }

        _containerHelper.createProject(getProjectName(), "Collaboration");
        enableModule(getProjectName(), "scriptpad");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Scriptpad");

        portalHelper.addReportWebPart("script_rmd");
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyKnitrHTMLFormat()
    {
        Locator[] reportContains = {Locator.tag("p").withText("This is a minimal example which shows knitr working with HTML pages in LabKey."),
                                    Locator.tag("img").withAttribute("title", "plot of chunk blood-pressure-scatter"),
                                    Locator.tag("pre").containing("## \"1\",249318596,\"2008-05-17\",86,36,129,76,64,17,0,\"false\",\"English\",\"urn:lsid:labkey.com:Study.Data-2156:5004.249318596.20080517.0000\""),
                                    Locator.css("span.functioncall").withText("message"),
                                    Locator.tag("pre").withText("## knitr says hello to HTML!"),
                                    Locator.tag("pre").withText("## Error: non-numeric argument to binary operator"),
                                    Locator.tag("p").withText("Well, everything seems to be working. Let's ask R what is the value of \u03C0? Of course it is 3.1416.")};
        String[] reportNotContains = {"<html>",                          // Uninterpreted html
                                      "<!--",                            // ditto
                                      "A minimal knitr example in HTML", // report title element
                                      "begin.rcode",                     // knitr commands shouldn't be visible
                                      "opts_chunk"};                     // Un-echoed R code

        createAndVerifyKnitrReport(rhtmlReport, KnitrFormat.Html, reportContains, reportNotContains);
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    private void verifyKnitrMarkupFormat()
    {
        Locator[] reportContains = {Locator.css("h1").withText("A Minimal Example for Markdown"),
                                    Locator.css("h2").withText("R code chunks"),
                                    Locator.css("code.r").containing("set.seed(123)"),       // Echoed R code
                                    Locator.xpath("//img").withAttribute("alt", "plot of chunk graphics"),
                                    Locator.css("p").withText("Inline R code is also supported, e.g. the value of x is 2, and 2 × \u03C0 = 6.2832."),
                                    Locator.css(".MathJax")};
        String[] reportNotContains = {"```",              // Markdown for R code chunks
                                      "## R code chunks", // Uninterpreted Markdown
                                      "{r",               // Markdown for R code chunks
                                      "data_means"};      // Non-echoed R code

        createAndVerifyKnitrReport(rmdReport, KnitrFormat.Markdown, reportContains, reportNotContains);
    }

    private enum KnitrFormat
    {
        Markdown,
        Html,
        None
    }

    private void createAndVerifyKnitrReport(Path reportSourcePath, KnitrFormat format, Locator[] reportContains, String[] reportNotContains)
    {
        final String reportSource = readReport(reportSourcePath);
        final String reportName = reportSourcePath.getFileName() + " Report";
        Locator reportDiv = Locator.css("#viewDiv > div.labkey-wiki");

        clickProject(getProjectName());
        goToManageViews();

        _extHelper.clickMenuButton("Create", "R View");
        checkRadioButton(Locator.radioButtonByNameAndValue("knitrFormat", format.toString()));
        setCodeEditorValue("script-report-editor", reportSource);
        clickButton("Save", 0);
        _extHelper.waitForExtDialog("Save View");
        _extHelper.setExtFormElement(reportName);
        _extHelper.clickExtButton("Save View", "Save");

        openView(reportName);

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
        Actions builder = new Actions(_driver);
        builder.contextClick(Locator.tag("div").withText(viewName).waitForElmement(getDriver(), WAIT_FOR_JAVASCRIPT)).build().perform();
        waitAndClickAndWait(Locator.linkWithText("View"));
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
