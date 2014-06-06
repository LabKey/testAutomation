/*
 * Copyright (c) 2009-2014 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.ConfigureReportsAndScriptsHelper;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public abstract class AbstractQCAssayTest extends AbstractAssayTest
{
    private final String engineLanguage = "java";
    private final String engineName = "Java";

    @LogMethod
    public void prepareProgrammaticQC()
    {
        createEngine();
        createNetrcFile();
    }

    @LogMethod
    private void createEngine()
    {
        ensureAdminMode();

        goToAdminConsole();
        clickAndWait(Locator.linkWithText("views and scripting"));
        log("setup a java engine");

        ConfigureReportsAndScriptsHelper scripts = new ConfigureReportsAndScriptsHelper(this);

        if (!scripts.isEnginePresent(engineLanguage))
        {
            String javaHome = System.getProperty("java.home");
            File javaExe = new File(javaHome + "/bin/java.exe");
            if (!javaExe.exists())
            {
                javaExe = new File(javaHome + "/bin/java");
                if (!javaExe.exists())
                    fail("unable to setup the java engine");
            }

            ConfigureReportsAndScriptsHelper.EngineConfig config = new ConfigureReportsAndScriptsHelper.EngineConfig(javaExe);
            config.setName(engineName);
            config.setLanguage(engineLanguage);
            config.setExtensions("jar");
            config.setCommand("-jar \"${scriptFile}\" \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            // add -Xdebug and -Xrunjdwp parameters to the engine command in order to attach a debugger to you transform script
            //config.setCommand("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 -jar ${scriptFile} \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            scripts.addEngine(ConfigureReportsAndScriptsHelper.EngineType.EXTERNAL, config);
        }
    }

    @LogMethod
    private void createNetrcFile()
    {
        // ensure the .netrc file exists
        try {
            File netrcFile = new File(System.getProperty("user.home") + "/" + "_netrc");

            if (!netrcFile.exists())
                netrcFile = new File(System.getProperty("user.home") + "/" + ".netrc");

            if (!netrcFile.exists())
            {
                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(netrcFile))))
                {
                    pw.append("machine localhost:8080");
                    pw.append('\n');
                    pw.append("login ");
                    pw.append(PasswordUtil.getUsername());
                    pw.append('\n');
                    pw.append("password ");
                    pw.append(PasswordUtil.getPassword());
                    pw.append('\n');
                }
            }
        }
        catch (IOException ioe)
        {
            log("failed trying to create a .netrc file " + ioe.getMessage());
        }
    }

    public void deleteEngine()
    {
        ensureAdminMode();

        goToAdminConsole();
        clickAndWait(Locator.linkWithText("views and scripting"));

        ConfigureReportsAndScriptsHelper scripts = new ConfigureReportsAndScriptsHelper(this);

        if (scripts.isEnginePresent(engineLanguage))
        {
            scripts.deleteEngine(engineName);
        }
    }

    public void addTransformScript(File transformScript, int index)
    {
        assertTrue("unable to locate the Transform script", transformScript.exists());

        waitForElement(Locator.lkButton("Add Script"));
        clickButton("Add Script", 0);
        setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
    }

    protected void startCreateNabAssay(String name)
    {
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb)"));
        clickButton("Next");

        Locator assayName = Locator.xpath("//input[@id='AssayDesignerName']");
        waitForElement(assayName, WAIT_FOR_JAVASCRIPT);
        setFormElement(assayName, name);

        log("Setting up NAb assay");
    }

    /**
     * Import a new run into this assay
     */
    protected void importData(AssayImportOptions options)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator("Import Data");
        Locator linkLocator = Locator.linkContainingText("Import Data");
        if (buttonLocator != null && isElementPresent(buttonLocator))
            clickAndWait(buttonLocator);
        else if (isElementPresent(linkLocator))
            clickAndWait(linkLocator);
        else
            fail("No Import Data button available");

        if (!options.isUseDefaultResolver())
        {
            if (options.getVisitResolver() == AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit)
            {
                checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.SpecimenID.name()));
                Locator checkBox = Locator.checkboxByName("includeParticipantAndVisit");
                waitForElement(checkBox);
                checkCheckbox(checkBox);
            }
            else
                checkCheckbox(Locator.radioButtonByNameAndValue("participantVisitResolver", options.getVisitResolver().name()));
        }
        else
        {
            switch (options.getVisitResolver())
            {
                case LookupList:
                    assertChecked(Locator.radioButtonByNameAndValue("ThawListType", "List"));
                    break;
                case LookupText:
                    assertChecked(Locator.radioButtonByNameAndValue("ThawListType", "Text"));
                    break;
                case SpecimenIDParticipantVisit:
                    assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", AssayImportOptions.VisitResolverType.SpecimenID.name()));
                    break;
                default:
                    assertChecked(Locator.radioButtonByNameAndValue("participantVisitResolver", options.getVisitResolver().name()));
            }
        }

        clickButton("Next");

        if (options.getAssayId() != null)
            setFormElement(Locator.name("name"), options.getAssayId());

        setFormElement(Locator.name("cutoff1"), options.getCutoff1());
        setFormElement(Locator.name("cutoff2"), options.getCutoff2());
        if (options.getCutoff3() != null)
            setFormElement(Locator.name("cutoff3"), options.getCutoff3());

        setFormElement(Locator.name("virusName"), options.getVirusName());
        setFormElement(Locator.name("virusID"), options.getVirusId());
        selectOptionByText(Locator.name("curveFitMethod"), options.getCurveFitMethod());

        // populate the sample well group information
        for (int i=0; i < options.getPtids().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_ParticipantID"), options.getPtids()[i]);
        }

        for (int i=0; i < options.getVisits().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_VisitID"), options.getVisits()[i]);
        }

        for (int i=0; i < options.getInitialDilutions().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_InitialDilution"), options.getInitialDilutions()[i]);
        }

        for (int i=0; i < options.getDilutionFactors().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_Factor"), options.getDilutionFactors()[i]);
        }

        for (int i=0; i < options.getMethods().length; i++)
        {
            selectOptionByText(Locator.name("specimen" + (i + 1) + "_Method"), options.getMethods()[i]);
        }

        for (int i=0; i < options.getDates().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_Date"), options.getDates()[i]);
        }

        for (int i=0; i < options.getSampleIds().length; i++)
        {
            setFormElement(Locator.name("specimen" + (i + 1) + "_SpecimenID"), options.getSampleIds()[i]);
        }

        File file1 = new File(options.getFilePath());
        setFormElement(Locator.name("__primaryFile__"), file1);
        clickButton("Save and Finish", 60000);
    }
}
