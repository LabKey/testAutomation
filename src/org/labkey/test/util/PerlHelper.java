package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 4/23/12
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PerlHelper
{

    public static boolean ensurePerlConfig(BaseSeleniumWebTest test)
    {

        test.gotoAdminConsole();
        test.clickLinkWithText("views and scripting");
        test.log("Check if Perl already is configured");


        if (test.isPerlEngineConfigured())
            return true;


        test.log("Try configuring Perl");
        String perlHome = System.getenv("PERL_HOME");
        if (perlHome != null)
        {
            test.log("PERL_HOME is set to: " + perlHome + " searching for the Perl application");
            File perlHomeDir = new File(perlHome);
            FilenameFilter perlFilenameFilter = new FilenameFilter()
            {
                public boolean accept(File dir, String name)
                {
                    return "perl.exe".equalsIgnoreCase(name) || "perl".equalsIgnoreCase(name);
                }
            };
            File[] files = perlHomeDir.listFiles(perlFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(perlHome, "bin").listFiles(perlFilenameFilter);
            }

            if (files != null)
            {
                for (File file : files)
                {
                    // add a new r engine configuration
                    String id = ExtHelper.getExtElementId(test, "btn_addEngine");
                    test.click(Locator.id(id));

                    id = ExtHelper.getExtElementId(test, "add_perlEngine");
                    test.click(Locator.id(id));

                    id = ExtHelper.getExtElementId(test, "btn_submit");
                    test.waitForElement(Locator.id(id), 10000);

                    id = ExtHelper.getExtElementId(test, "editEngine_exePath");
                    test.setFormElement(Locator.id(id), file.getAbsolutePath());

                    id = ExtHelper.getExtElementId(test, "btn_submit");
                    test.click(Locator.id(id));

                    // wait until the dialog has been dismissed
                    int cnt = 3;
                    while (test.isElementPresent(Locator.id(id)) && cnt > 0)
                    {
                        test.sleep(1000);
                        cnt--;
                    }

                    if (test.isPerlEngineConfigured())
                        return true;

                    test.refresh();
                }
            }
        }
        return false;
    }
}
