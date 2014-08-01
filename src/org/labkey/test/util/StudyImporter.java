/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;

import java.io.File;
import java.io.IOException;

public class StudyImporter
{
    BaseWebDriverTest _test;

    public StudyImporter(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void zipAndImportStudy(File studyFolder)
    {
        ZipUtil zipper = new ZipUtil(studyFolder, BaseWebDriverTest.getDownloadDir());
        File studyZip;
        try
        {
            studyZip = zipper.zipIt();
        }
        catch (IOException fail)
        {
            throw new RuntimeException(fail);
        }
        _test.importStudyFromZip(studyZip, true, true);
    }
}
