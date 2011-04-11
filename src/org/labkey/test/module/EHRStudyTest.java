/*
 * Copyright (c) 2011 LabKey Corporation
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

package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 21, 2011
 * Time: 1:59:12 PM
 */
public class EHRStudyTest extends BaseSeleniumWebTest
{
    // Project/folder names are hard-coded into some links in the module.
    private static final String PROJECT_NAME = "WNPRC";
    private static final String FOLDER_NAME = "EHR";

    private static final String STUDY_ZIP = "/sampledata/study/EHR Study Anon.zip";

    @Override
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL");
    }
       
    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/ehr";
    }

    @Override
    public void doCleanup()
    {
        long startTime = System.currentTimeMillis();
        try {deleteProject(PROJECT_NAME);} catch (Throwable t) { /*ignore*/ }
        if(isTextPresent(PROJECT_NAME))
        {
            log("Wait extra long for folder to finish deleting.");
            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
            {
                sleep(5000);
                refresh();
            }
            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else fail("Test Project not finished deleting after 5 minutes");
        }
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "Study", new String[]{"EHR"});
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());

        log("Remove all webparts");
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        clickWebpartMenuItem("Views", false, "Layout", "Remove From Page");
        sleep(100);
        clickWebpartMenuItem("Specimens", false, "Layout", "Remove From Page");
        
        log("Add EHR webparts");
        addWebPart("Electronic Health Record");
        addWebPart("EHR Navigation");
        addWebPart("EHR Datasets");
        addWebPart("Animal History");
        addWebPart("Last EHR Sync");
        addWebPart("Quick Search");
        addWebPart("Project Sponsors");
    }
}
