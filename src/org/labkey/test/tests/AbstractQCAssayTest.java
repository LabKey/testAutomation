/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Oct 27, 2009
 */
public abstract class AbstractQCAssayTest extends AbstractAssayTest
{
    public void prepareProgrammaticQC()
    {
        ensureAdminMode();

        gotoAdminConsole();
        clickLinkWithText("views and scripting");
        log("setup a java engine");

        if (!isEngineConfigured())
        {
            // add a new r engine configuration
            String id = ExtHelper.getExtElementId(this, "btn_addEngine");
            click(Locator.id(id));

            id = ExtHelper.getExtElementId(this, "add_externalEngine");
            click(Locator.id(id));

            id = ExtHelper.getExtElementId(this, "btn_submit");
            waitForElement(Locator.id(id), 10000);

            id = ExtHelper.getExtElementId(this, "editEngine_exePath");

            String javaHome = System.getProperty("java.home");
            File javaExe = new File(javaHome + "/bin/java.exe");
            if (!javaExe.exists())
            {
                javaExe = new File(javaHome + "/bin/java");
                if (!javaExe.exists())
                    fail("unable to setup the java engine");
            }
            setFormElement(Locator.id(id), javaExe.getAbsolutePath());

            id = ExtHelper.getExtElementId(this, "editEngine_name");
            setFormElement(Locator.id(id), "Java");

            id = ExtHelper.getExtElementId(this, "editEngine_languageName");
            setFormElement(Locator.id(id), "java");

            id = ExtHelper.getExtElementId(this, "editEngine_extensions");
            setFormElement(Locator.id(id), "jar");

            id = ExtHelper.getExtElementId(this, "editEngine_exeCommand");
            setFormElement(Locator.id(id), "-jar \"${scriptFile}\" \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"", true);

            // add -Xdebug and -Xrunjdwp parameters to the engine command in order to attach a debugger to you transform script
            //setFormElement(Locator.id(id), "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 -jar ${scriptFile} \"${runInfo}\" \"" + PasswordUtil.getUsername() + "\" \"" + PasswordUtil.getPassword() + "\" \"" + WebTestHelper.getBaseURL() + "\"");

            id = ExtHelper.getExtElementId(this, "btn_submit");
            click(Locator.id(id));

            // wait until the dialog has been dismissed
            waitForElementToDisappear(Locator.id(id), WAIT_FOR_JAVASCRIPT);

            if (!isEngineConfigured())
                fail("unable to setup the java engine");
        }

        // ensure the .netrc file exists
        try {
            File netrcFile = new File(System.getProperty("user.home") + "/" + "_netrc");

            if (!netrcFile.exists())
                netrcFile = new File(System.getProperty("user.home") + "/" + ".netrc");

            if (!netrcFile.exists())
            {
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(netrcFile)));
                try {
                    pw.append("machine localhost:8080");
                    pw.append('\n');
                    pw.append("login " + PasswordUtil.getUsername());
                    pw.append('\n');
                    pw.append("password " + PasswordUtil.getPassword());
                    pw.append('\n');
                }
                finally
                {
                    pw.close();
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
        int time = 0;
        while (!isElementPresent(engine) && time < WAIT_FOR_JAVASCRIPT)
        {
            sleep(100);
            time += 100;
        }
        return isElementPresent(engine);
    }

    public void deleteEngine()
    {
        ensureAdminMode();

        gotoAdminConsole();
        clickLinkWithText("views and scripting");

        if (isEngineConfigured())
        {
            Locator engine = Locator.xpath("//div[@id='enginesGrid']//td//div[.='jar']");
            selenium.mouseDown(engine.toString());

            String id = ExtHelper.getExtElementId(this, "btn_deleteEngine");
            click(Locator.id(id));

            ExtHelper.waitForExtDialog(this, "Delete Engine Configuration", WAIT_FOR_JAVASCRIPT);

            String btnId = selenium.getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().buttons[1].getId();");
            click(Locator.id(btnId));
        }
    }

    public void addTransformScript(File transformScript, int index)
    {
        if (transformScript.exists())
        {
            waitForElement(Locator.navButton("Add Script"));
            clickNavButton("Add Script", 0);
            selenium.type("//input[@id='AssayDesignerTransformScript" + index + "']", transformScript.getAbsolutePath());
        }
        else
            fail("unable to locate the Transform script");
    }
}
