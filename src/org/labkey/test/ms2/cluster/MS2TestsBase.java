/*
 * Copyright (c) 2005 LabKey Software, LLC
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
package org.labkey.test.ms2.cluster;

import org.labkey.test.BaseSeleniumWebTest;

import java.util.List;
import java.util.ArrayList;

/**
 * MS2TestsBase class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
public class MS2TestsBase
{
    protected BaseSeleniumWebTest test;
    protected List<MS2TestParams> listParams = new ArrayList<MS2TestParams>();

    public MS2TestsBase(BaseSeleniumWebTest test)
    {
        this.test = test;
    }

    public MS2TestParams[] getParams()
    {
        return listParams.toArray(new MS2TestParams[listParams.size()]);
    }

    public void removeParams(MS2TestParams params)
    {
        listParams.remove(params);
    }

    public void addTestsScoringOrganisms() {}
    public void addTestsISBMix() {}
    public void addTestsScoringMix() {}
    public void addTestsIPAS() {}
    public void addTestsQuant() {}
}
