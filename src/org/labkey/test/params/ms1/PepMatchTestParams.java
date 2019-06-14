/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.params.ms1;

import org.labkey.test.params.ms1.AbstractInspectTestParams;
import org.labkey.test.pipeline.PipelineWebTestBase;

import java.io.File;

/**
 * PepMatchTestParams class
* <p/>
* Created: Aug 18, 2008
*
* @author bmaclean
*/
public class PepMatchTestParams extends AbstractInspectTestParams
{
    public PepMatchTestParams(PipelineWebTestBase test, String dataPath, String protocolName,
                             boolean cachesExist, String... sampleNames)
    {
        super(test, dataPath, "pepmatch", protocolName, ".pepmatch.tsv", cachesExist, sampleNames);
    }

    public File getCacheDir(File rootDir)
    {
        return new File(rootDir, getDataPath() + File.separatorChar + ".." + File.separatorChar + "..");
    }

    public void clickActionButton()
    {
        String[] names = getSampleNames();
        if (names.length != 0)
        {
            for (String name : names)
            {
                _test._fileBrowserHelper.checkFileBrowserFileCheckbox(name + ".pep.xml");
            }
        }
        _test.log("msInspect & pepmatch run");
        _test._fileBrowserHelper.selectImportDataAction("msInspect Find Features and Match Peptides");
    }
}