/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.pipeline.AbstractPipelineTestParams;
import org.labkey.test.pipeline.PipelineWebTestBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

abstract public class AbstractInspectTestParams extends AbstractPipelineTestParams
{
    private static final String[] _cacheExtensions = new String[] { ".mzXML.inspect", ".mzXML.ms2.tsv" };

    public AbstractInspectTestParams(PipelineWebTestBase test, String dataPath, String protocolType, String protocolName,
                             String outputExt, boolean cachesExist, String... sampleNames)
    {
        super(test, dataPath, protocolType, protocolName, sampleNames);

        // msInspect pipeline does not support fraction roll-up.
        assert sampleNames.length > 0 : "Sample names are required for the msInspect pipeline.";

        ArrayList<String> listExts = new ArrayList<>(Arrays.asList(_cacheExtensions));
        if (cachesExist)
        {
            listExts.add(".mzXML");
            setInputExtensions(listExts.toArray(new String[listExts.size()]));
            setOutputExtensions(outputExt);
        }
        else
        {
            setInputExtensions(".mzXML");
//          BUG: Unfortunately LocalWorkDirectory does not catch output of cache files
//               directly to their final destination.
//            listExts.add(outputExt);
//            setOutputExtensions(listExts.toArray(new String[listExts.size()]));
            setOutputExtensions(outputExt);
        }
    }

    public String getExperimentRunTableName()
    {
        return "MSInspectFeatureRuns";
    }

    public File getCacheDir(File rootDir)
    {
        return new File(rootDir, getDataPath());
    }

    public void clean(File rootDir)
    {
        super.clean(rootDir);

        File cacheDir = getCacheDir(rootDir);

        for (String sampleName : getSampleNames())
        {
            for (String ext : _cacheExtensions)
                TestFileUtils.delete(new File(cacheDir, sampleName + ext));
        }
    }

    @Override
    protected void clickSubmitButton()
    {
        _test.click(Locator.id("submitButton"));
    }

    public void verifyClean(File rootDir)
    {
        super.verifyClean(rootDir);

        File cacheDir = getCacheDir(rootDir);

        for (String sampleName : getSampleNames())
        {
            for (String ext : _cacheExtensions)
            {
                File cacheFile = new File(cacheDir, sampleName + ext);
                if (cacheFile.exists())
                    fail("Pipeline files were not cleaned up; "+ cacheFile + " still exists");
            }
        }
    }
}
