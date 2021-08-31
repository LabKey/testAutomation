/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Reports;
import org.labkey.test.util.RReportHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;

@Category({Daily.class, Reports.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class KnitrReportTest extends AbstractKnitrReportTest
{
    private static final Path rmdDependenciesReport = scriptpadReports.resolve("kable.rmd");

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

        if (!TestProperties.isServerRemote())
        {
            if (afterTest)
                revertLibXml();
            else
                deleteLibXml();
        }
    }

    @Test
    public void testKnitrHTMLFormat()
    {
        htmlFormat();
    }

    @Test
    public void testKnitrMarkupFormat() throws Exception
    {
        Locator.XPathLocator plotLocator = Locator.xpath("//div[@class='labkey-knitr']//img");
        Locator[] reportContains = {Locator.tag("h1").withText("A Minimal Example for Markdown"),
                                    Locator.tag("h2").withText("R code chunks"),
                                    Locator.tagWithClass("code", "hljs").containing("set.seed(123)"),       // Echoed R code
                                    Locator.css("p").containing("2 x pi = 6.283"),
                                    Locator.tag("sup").withText("write") //should contain the hat markdown v2 closing tag
        };
        String[] reportNotContains = {"```",              // Markdown for R code chunks
                                      "## R code chunks", // Uninterpreted Markdown
                                      "{r",               // Markdown for R code chunks
                                      //"propto",           // MathJax source
                                      "data_means"};      // Non-echoed R code

        Path reportSourcePath = TestProperties.isWithoutTestModules() ? rmdReport_no_scriptpad : rmdReport;
        createAndVerifyKnitrReport(reportSourcePath, RReportHelper.ReportOption.knitrMarkdown, reportContains, reportNotContains, true);
        assertEquals("Knitr report failed to display plot", HttpStatus.SC_OK, WebTestHelper.getHttpResponse(plotLocator.findElement(getDriver()).getAttribute("src")).getResponseCode());
    }

    @Test
    public void testModuleReportDependencies()
    {
        moduleReportDependencies();
    }

    @Test
    public void testAdhocReportDependenciesString()
    {
        if (TestProperties.isServerRemote())
        {
            Assume.assumeTrue("Unable to modify remote webapp. Skipping test.", TestProperties.isWithoutTestModules());
            // Scriptpad module supplies lib.xml. No need to modify webapp if it isn't present.
        }
        else
        {
            deleteLibXml();
        }
        verifyAdhocReportDependencies("Strings",
                "https://ajax.aspnetcdn.com/ajax/jquery/jquery-1.9.0.min.js;" +
                "https://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/jquery.dataTables.min.js;\r\n" +
                "https://ajax.aspnetcdn.com/ajax/jquery.dataTables/1.9.4/css/jquery.dataTables.css"
        );
    }

    @Test
    public void testAdhocReportDependenciesLib()
    {
        Assume.assumeFalse("Unable to add dependencies to remote webapp root.", TestProperties.isServerRemote());

        copyLibXml();

        verifyAdhocReportDependencies("ClientLib", "knitr");
    }

    @Test
    public void testRmarkdownV2Support() throws Exception
    {
        markdownV2();
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
        setPandocEnabled(true);

        // just do a sanity check of the report's contents.  If the dependencies aren't loaded then we'll throw an alert
        Locator[] reportContains = {Locator.id("mtcars_table_wrapper"), Locator.css("h1").withText("jQuery DataTables")};
        String[] reportNotContains = {"```", "{r",};

        createKnitrReport(rmdDependenciesReport, RReportHelper.ReportOption.knitrMarkdown);

        pauseJsErrorChecker(); // Don't fail due to "$ is not a function"
        {
            _rReportHelper.clickReportTab();
            waitForElement(Locator.id("mtcars_table"));
            assertElementNotPresent(Locator.id("mtcars_table_wrapper")); // Created by jQuery
        }
        resumeJsErrorChecker();

        // now set the dependencies
        _rReportHelper.clickSourceTab();
        _rReportHelper.ensureFieldSetExpanded("knitr");
        setFormElement(Locator.name("scriptDependencies"), dependencies);
        saveAndVerifyKnitrReport(rmdDependenciesReport.getFileName() + " " + viewName, reportContains, reportNotContains);
    }
}
