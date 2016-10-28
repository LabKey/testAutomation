/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
package org.labkey.test.pipeline;

import org.labkey.test.util.ExperimentRunTable;
import org.labkey.test.util.PipelineStatusTable;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertFalse;

/**
 * MS2TestsBase class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
public class PipelineTestsBase
{
    protected PipelineWebTestBase _test;
    protected PipelineFolder _folder;
    protected List<PipelineTestParams> _listParams = new ArrayList<>();

    public PipelineTestsBase(PipelineWebTestBase test)
    {
        _test = test;
    }

    public PipelineFolder getFolder()
    {
        return _folder;
    }

    public void setFolder(PipelineFolder folder)
    {
        _folder = folder;
    }

    public boolean hasParams()
    {
        return _listParams.size() > 0;
    }

    public void addParams(PipelineTestParams params)
    {
        _listParams.add(params);
    }

    public void removeParams(PipelineTestParams params)
    {
        _listParams.remove(params);
    }

    public PipelineTestParams[] getParams()
    {
        return _listParams.toArray(new PipelineTestParams[_listParams.size()]);
    }

    public PipelineTestParams[] getCompleteParams()
    {
        PageCache pc = new PageCache();
        List<PipelineTestParams> listCompleteParams = new ArrayList<>();
        for (PipelineTestParams tp : _listParams)
        {
            if (pc.isComplete(tp))
                listCompleteParams.add(tp);
        }
        return listCompleteParams.toArray(new PipelineTestParams[listCompleteParams.size()]);
    }

    public void setup()
    {
        _folder.setup();
    }

    public void beginAt()
    {
        _test.beginAt("/labkey/Project/" + _test.getProjectName() + "/" + _folder.getFolderName() + "/begin.view");        
    }
    
    public void clean()
    {
        if (_folder == null)
            return;

        _folder.clean();

        File rootDir = new File(_folder.getPipelinePath());
        _test.log("Cleaning pipeline files in " + rootDir);

        for (PipelineTestParams tp : getParams())
        {
            tp.clean(rootDir);
        }
    }

    public void verifyClean()
    {
        if (_folder == null)
            return;

        File rootDir = new File(_folder.getPipelinePath());
        _test.log("Verifying that pipeline files were cleaned up properly in " + rootDir);

        for (PipelineTestParams tp : getParams())
        {
            tp.verifyClean(rootDir);
        }
    }

    public void runAll()
    {
        HashSet<String> runs = new HashSet<>();

        for (PipelineTestParams tp : getParams())
        {
            String searchKey = tp.getRunKey();
            if (runs.contains(searchKey))
                continue;
            runs.add(searchKey);

            beginAt();
            tp.startProcessing();
        }
    }

    public class PageCache
    {
        private PipelineStatusTable _tableStatus;
        private Map<String, ExperimentRunTable> _mapTableExp = new HashMap<>();

        /**
         * Determines whether a test has completed its pipeline processing.  Note
         * that this function also has the side-effect of replacing the expected
         * experiment names with the complete text for their experiment link, for
         * easier link clicking.
         *
         * @param tp The test to check for completeness
         * @return True if the experiment run is in the expected table, and status is complete
         */
        public boolean isComplete(PipelineTestParams tp)
        {
            String tableName = tp.getExperimentRunTableName();
            String[] names = tp.getExperimentLinks();
            String[] completeNames = new String[names.length];
            for (int i = 0; i < names.length; i++)
            {
                if (tp.isExpectError())
                {
                    if (!"ERROR".equals(getStatusTable().getJobStatus(names[i])))
                        return false;
                }
                else
                {
                    assertFalse("Unexpected error in job: " + names[i], "ERROR".equals(getStatusTable().getJobStatus(names[i])));

                    String completeName = getExperimentTable(tableName).getRunName(names[i]);
                    if (completeName == null || getStatusTable().hasJob(names[i]))
                        return false;
                    completeNames[i] = completeName;
                }
            }
            if (!tp.isExpectError())
                tp.setExperimentLinks(completeNames);
            return true;
        }

        private ExperimentRunTable getExperimentTable(String tableName)
        {
            if (!_mapTableExp.containsKey(tableName))
                _mapTableExp.put(tableName, new ExperimentRunTable(tableName, _test, true));
            return _mapTableExp.get(tableName);
        }

        private PipelineStatusTable getStatusTable()
        {
            if (_tableStatus == null)
                _tableStatus = new PipelineStatusTable(_test);
            return _tableStatus;
        }
    }
}
