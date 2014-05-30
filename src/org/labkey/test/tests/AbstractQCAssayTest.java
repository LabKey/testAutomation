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
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.PasswordUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.*;

public abstract class AbstractQCAssayTest extends AbstractAssayTest
{
    public void prepareProgrammaticQC()
    {
        ensureAdminMode();

        goToAdminConsole();
        clickAndWait(Locator.linkWithText("views and scripting"));
        log("setup a java engine");

        if (!isEngineConfigured())
        {
            // add a new r engine configuration
            String id = _extHelper.getExtElementId("btn_addEngine");
            click(Locator.id(id));

            id = _extHelper.getExtElementId("add_externalEngine");
            click(Locator.id(id));

            id = _extHelper.getExtElementId("btn_submit");
            waitForElement(Locator.id(id), 10000);

            id = _extHelper.getExtElementId("editEngine_exePath");

            String javaHome = System.getProperty("java.home");
            File javaExe = new File(javaHome + "/bin/java.exe");
            if (!javaExe.exists())
            {
                javaExe = new File(javaHome + "/bin/java");
                if (!javaExe.exists())
                    fail("unable to setup the java engine");
            }
            setFormElement(Locator.id(id), javaExe.getAbsolutePath());

            id = _extHelper.getExtElementId("editEngine_name");
            setFormElement(Locator.id(id), "Java");

            id = _extHelper.getExtElementId("editEngine_languageName");
            setFormElement(Locator.id(id), "java");

            id = _extHelper.getExtElementId("editEngine_extensions");
            setFormElement(Locator.id(id), "jar");

            id = _extHelper.getExtElementId("editEngine_exeCommand");
            setFormElement(Locator.id(id), "-jar \"${scriptFile}\" \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            // add -Xdebug and -Xrunjdwp parameters to the engine command in order to attach a debugger to you transform script
            //setFormElement(Locator.id(id), "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 -jar ${scriptFile} \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            id = _extHelper.getExtElementId("btn_submit");
            click(Locator.id(id));

            // wait until the dialog has been dismissed
            waitForElementToDisappear(Locator.id(id), WAIT_FOR_JAVASCRIPT);

            waitFor(new Checker()
            {
                public boolean check()
                {
                    return isEngineConfigured();
                }
            }, "unable to setup the java engine", WAIT_FOR_JAVASCRIPT);
        }

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

    public boolean isEngineConfigured()
    {
        // need to allow time for the server to return the engine list and the ext grid to render
        Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='jar']");

        waitForElement(Locator.xpath("//div[@id='enginesGrid']//td//div[.='js']"), WAIT_FOR_JAVASCRIPT); //JS engine always present
        return isElementPresent(engine);
    }

    public void deleteEngine()
    {
        ensureAdminMode();

        goToAdminConsole();
        clickAndWait(Locator.linkWithText("views and scripting"));

        if (isEngineConfigured())
        {
            click(Locator.xpath("//div[@id='enginesGrid']//td//div[.='jar']"));

            String id = _extHelper.getExtElementId("btn_deleteEngine");
            click(Locator.id(id));

            _extHelper.waitForExtDialog("Delete Engine Configuration", WAIT_FOR_JAVASCRIPT);

            _extHelper.clickExtButton("Delete Engine Configuration", "Yes");
        }
    }

    public void addTransformScript(File transformScript, int index)
    {
        assertTrue("unable to locate the Transform script", transformScript.exists());

        waitForElement(Locator.navButton("Add Script"));
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
