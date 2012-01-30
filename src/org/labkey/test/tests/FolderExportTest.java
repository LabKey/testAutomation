/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 1/24/12
 * Time: 1:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class FolderExportTest extends BaseSeleniumWebTest
{

    String folderFromZip = "Folder 1" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    String[] webParts = {"Study Overview", "Data Pipeline", "Specimens", "Views", "Study Data Tools"};
    String dataDir = getSampledataPath() + "\\FolderExport";
    private String folderFromPipeplineZip = "Folder 2";



    @Override
    protected String getProjectName()
    {
        return "FolderExportTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject(getProjectName());
        
        verifyImportFromZip();
        verifyImportFromPipelineZip();
        //Issue 13881
//        verifyImportFromPipelineExpanded();
    }

    private void verifyImportFromPipelineZip()
    {
        verifyImportFromPipeline("Sample.folder.zip");
    }

    private void verifyImportFromPipelineExpanded()
    {
        verifyImportFromPipeline("Sample.folder/folder.xml");

    }

    private void verifyImportFromPipeline(String fileImport)
    {

        createSubfolder(getProjectName(), folderFromPipeplineZip, null);
        setPipelineRoot(dataDir);
        importFolderFromPipeline( "" + fileImport);


        clickLinkContainingText(folderFromPipeplineZip);
        verifyFolderImportAsExpected();
        deleteFolder(getProjectName(), folderFromPipeplineZip);}

    private void verifyImportFromZip()
    {
        createSubfolder(getProjectName(), folderFromZip, null);

        importFolderFromZip(dataDir + "\\Sample.folder.zip");
        waitForPipelineJobsToComplete(1, "foo", false);
        clickLinkWithText(folderFromZip);
        verifyFolderImportAsExpected();
    }

    private void verifyFolderImportAsExpected()
    {
        assertTextPresentInThisOrder(webParts);
        assertTextPresent("Test wikiTest wikiTest wiki");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
