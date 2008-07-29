/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
package org.labkey.test.ms2;

import org.apache.commons.io.FileUtils;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.ms2.cluster.MS2TestParams;
import org.labkey.test.ms2.cluster.MS2TestsBase;
import org.labkey.test.ms2.cluster.MS2TestsBaseline;
import org.labkey.test.ms2.cluster.MS2Tests_20070701__3_4_1;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author brendanx
 */
public class MS2ClusterTest extends BaseSeleniumWebTest
{
    public static final String PROTOCOL_MODIFIER = "";
    public static final String PROTOCOL_MODIFIER_SEARCH = "";

    private static boolean CLEAN_DATA = true;
    private static boolean NEW_DATA = true;
    private static boolean NEW_SEARCH = true;
    private static boolean REMOVE_DATA = true;
    private static boolean USE_GLOBUS = true;

    protected static final String PROJECT_NAME = "MS2ClusterProject";
    protected static final String FOLDER_NAME = "Pipeline";
    protected static final String PIPELINE_PATH = "T:/edi/pipeline/Test/regression";
    protected static final String FASTA_PATH = "T:/data/databases";
    // These files are not checked in, since that would be a security issue.
    // Ask Brendan, Josh or Brian, if you need them.
    protected static final String USER_CERT = "/sampledata/pipeline/globus/usercert.pem";
    protected static final String USER_KEY = "/sampledata/pipeline/globus/userkey.pem";
    protected static final String USER_KEY_PASSWORD = "ChiKung1";
    protected static final int MAX_WAIT_SECONDS = 60*60*5;

    protected MS2TestsBase testSet;

    public MS2ClusterTest()
    {
        testSet = new MS2Tests_20070701__3_4_1(this);
//        testSet.addTestsScoringMix();
        testSet.addTestsQuant();
//        testSet.addTestsScoringOrganisms();
//        testSet.addTestsISBMix();
//        testSet.addTestsIPAS();
    }

    // Return the directory of the module whose functionality this class tests, or "none" if multiple/all modules are tested
    public String getAssociatedModuleDirectory()
    {
        return "ms2";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return USE_GLOBUS;
    }

    protected void doCleanup() throws IOException
    {
        if (CLEAN_DATA)
        {
            cleanPipe(PIPELINE_PATH);
            try {deleteFolder(PROJECT_NAME, FOLDER_NAME); } catch (Throwable t) {}
            try {deleteProject(PROJECT_NAME); } catch (Throwable t) {} //*/
        }
    }

    protected void doTestSteps()
    {
        if (CLEAN_DATA)
        {
            log("Verifying that pipeline files were cleaned up properly");
            verifyPipeClean(PIPELINE_PATH);

            doSetup();
        }
        else
        {
            beginAt("/labkey/Project/MS2ClusterProject/Pipeline/begin.view");
        }
        
        if (NEW_DATA)
        {
            doAnalysis();
        }

        int seconds = 0;
        List<MS2TestParams> listValidated = new ArrayList<MS2TestParams>();
        while (seconds++ < MAX_WAIT_SECONDS)
        {
            DataRegionTable tableExp = new DataRegionTable("MS2SearchRuns", this);
            int rows = tableExp.getDataRowCount();
            int nameCol = tableExp.getColumn("Name");

            for (MS2TestParams tp : testSet.getParams())
            {
                for (int i = 0; i < rows; i++)
                {
                    // See if the experiment is present.
                    String name = tp.getExperimentLink();
                    String cellText = tableExp.getDataAsText(i, nameCol);
                    if (cellText.indexOf(name) == -1)
                        continue;
                    
                    // Make sure the status is COMPLETE.
                    DataRegionTable tableStatus = new DataRegionTable("StatusFiles", this, false);
                    String status = tp.getStatus(name, tableStatus);
                    if (status != null)
                        continue;

                    log("***** " + name + " *****");

                    // Save the experiment run id for deletion.
                    log("Click peptide view link");
                    pushLocation();
                    tableExp.clickLink(i, 2);
                    URL url = getURL();
                    popLocation();

                    // Click the link for the MS2 peptides view.
                    log("Click peptide view link");
                    clickLinkWithText(cellText);

                    // If experiment is present, and not still running, validate
                    tp.validate();

                    clickLinkWithText("MS2 Dashboard");
                    if (tp.isValid() && REMOVE_DATA)
                    {
                        String id = url.getQuery();
                        id = id.substring(id.indexOf('=') + 1);
                        checkCheckbox(".select", id, false);
                        clickNavButton("Delete");
                        clickNavButton("Confirm Delete");

                        // Number of rows has changed, so recount them.
                        rows = tableExp.getDataRowCount();
                    }
                    testSet.removeParams(tp);
                    listValidated.add(tp);
                    break;
                }
            }

            if (testSet.getParams().length == 0)
                break;
            
            log("Waiting to validate completed searches");
            sleep(60*1000);
            refresh();
        }

        // Count uses case sensitive match.
        assertLinkPresentWithTextCount("ERROR", 0);

        for (MS2TestParams tp : listValidated)
        {
            assertTrue("Failed " + tp.getExperimentLink(), tp.isValid());
        }
    }

    protected void doSetup()
    {
        log("Set cluster checkbox");
        clickLinkWithText("Admin Console");
        clickLinkWithText("site settings");
        checkCheckbox("perlPipelineEnabled");
        clickNavButton("Save");

        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "MS2", new String[] { });

        log("Setup pipeline.");
        clickNavButton("Setup");

        log("Set pipeline root.");
        setFormElement("path", PIPELINE_PATH);

        if (USE_GLOBUS)
        {
            assertTrue("Globus test requires file upload.", isFileUploadAvailable());
            setFormElement("keyFile", new File(getLabKeyRoot() + USER_KEY));
            setFormElement("keyPassword", USER_KEY_PASSWORD);
            setFormElement("certFile", new File(getLabKeyRoot() + USER_CERT));
        }
        else
        {
            checkCheckbox("perlPipeline");
        }
        submit();

        log("Set FASTA root");
        clickLinkWithText("Set FASTA root");

        setFormElement("localPathRoot", FASTA_PATH);
        submit();

        clickNavButton("View Status");
    }

    protected void doAnalysis()
    {
        HashSet<String> searches = new HashSet<String>();

        for (MS2TestParams tp : testSet.getParams())
        {
            String searchKey = tp.getSearchKey();
            if (searches.contains(searchKey))
                continue;
            searches.add(searchKey);

            log("Start analysis of " + tp.getDataPath());
            clickNavButton("Process and Import Data");
            clickDirLinks(tp.getDataPath());

            log("X! Tandem Search");
            clickNavButton("X%21Tandem Peptide Search");

            log("Choose existing protocol " + tp.getProtocol());
            waitForElement(Locator.xpath("//select[@name='protocol']/option[.='" + tp.getProtocol() + "']" ), WAIT_FOR_GWT * 12);
            selectOptionByText("protocol", tp.getProtocol());
            sleep(WAIT_FOR_GWT);

            log("Start the search");
            submit();
            sleep(WAIT_FOR_GWT);

            if (!NEW_SEARCH)
            {
                File dirRoot = new File(PIPELINE_PATH);
                String analysisPath = tp.getDataPath() + File.separator +
                        "xtandem" + File.separator + tp.getProtocol();
                File dirDest = new File(dirRoot,  analysisPath);

                // Strip modifier from the name.
                analysisPath = analysisPath.substring(0,
                        analysisPath.length() - PROTOCOL_MODIFIER.length());
                File dirSrc = new File(dirRoot,  analysisPath + PROTOCOL_MODIFIER_SEARCH);

                File[] tandemFiles = dirSrc.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".xtan.xml");
                    }
                });

                for (File fileTandem : tandemFiles)
                {
                    try
                    {
                        FileUtils.copyFileToDirectory(fileTandem, dirDest);
                    }
                    catch (IOException e)
                    {
                        assertTrue("Failed to copy search results '" + fileTandem + "'.", false);
                    }
                }
            }
        }
    }

    private void clickDirLinks(String path)
    {
        clickLinkWithText("root");
        String[] dirs = path.split("/");
        for (String dir : dirs)
            clickLinkWithText(dir);
    }

    private void verifyPipeClean(String path)
    {
        if (path == null)
            return;

        File rootDir = new File(path);
        for (MS2TestParams tp : testSet.getParams())
        {
            File analysisDir = new File(rootDir, tp.getDataPath() + File.separator + "xtandem");
            if (analysisDir.exists())
                fail("Pipeline files were not cleaned up; "+ analysisDir.toString() + " directory still exists");
        }
    }

    private void cleanPipe(String path) throws IOException
    {
        if (path == null)
            return;

        File rootDir = new File(path);
        for (MS2TestParams tp : testSet.getParams())
        {
            delete(new File(rootDir, tp.getDataPath() + "/xtandem"));
        }
    }

    private void delete(File file) throws IOException
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        System.out.println("Deleting " + file.getPath() + "\n");
        file.delete();
    }

}
