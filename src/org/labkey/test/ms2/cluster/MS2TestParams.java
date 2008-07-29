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
package org.labkey.test.ms2.cluster;

import org.labkey.test.util.DataRegionTable;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.ms2.MS2ClusterTest;
import com.thoughtworks.selenium.SeleniumException;

/**
 * MS2TestParams class
* <p/>
* Created: Aug 15, 2007
*
* @author bmaclean
*/
public class MS2TestParams
{
    protected BaseSeleniumWebTest test;
    protected String dataPath;
    protected String sampleName;
    protected String protocol;
    protected boolean valid;

    public MS2TestParams(BaseSeleniumWebTest test, String dataPath, String protocol)
    {
        this(test, dataPath, null, protocol);
    }

    public MS2TestParams(BaseSeleniumWebTest test, String dataPath, String sampleName, String protocol)
    {
        this.test = test;
        this.dataPath = dataPath;
        this.sampleName = sampleName;
        this.protocol = protocol + MS2ClusterTest.PROTOCOL_MODIFIER;
        this.valid = true;
    }

    public String getDataPath()
    {
        return dataPath;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getSearchKey()
    {
        return dataPath + " (" + protocol + ")";
    }

    public String getExperimentLink()
    {
        String[] dirs = dataPath.split("/");
        StringBuffer link = new StringBuffer(dirs[dirs.length - 1]);
        if (sampleName != null)
            link.append('/').append(sampleName);
        link.append(" (").append(protocol).append(")");
        return link.toString();
    }

    public String getStatus(String nameText, DataRegionTable table)
    {
        if (nameText == null)
            return null;

        int colDescription = table.getColumn("Description");
        int colStatus = table.getColumn("Status");

        for (int i = 0; i < table.getDataRowCount(); i++)
        {
            try
            {
                if (!nameText.equals(table.getDataAsText(i, colDescription)))
                    continue;
            }
            catch (SeleniumException e)
            {
                test.log("ERROR: Getting description text for row " + i + ", column " + colDescription);
                test.log("       Row count " + table.getDataRowCount());
                return "UNKNOWN";
            }

            return table.getDataAsText(i, colStatus);
        }
        return null;
    }

    public void setGrouping(String grouping)
    {
        test.log("Set grouping to " + grouping);
        test.selectOptionByText("grouping", grouping);
        test.clickAndWait(Locator.id("viewTypeSubmitButton"));
    }

    public void validate()
    {
        // Default does fails to allow manual analysis of the run.
        // Override to do actual automated validation of the resulting
        // MS2 run data.

        validateTrue("No automated validation for " + getExperimentLink(), false);
    }

    public void validateTrue(String message, boolean condition)
    {
        if (!condition)
        {
            test.log("INVALID: " + message);
            valid = false;
        }
    }

    public boolean isValid()
    {
        return valid;
    }
}
