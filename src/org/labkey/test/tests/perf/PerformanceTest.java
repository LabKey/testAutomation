/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
package org.labkey.test.tests.perf;

import org.labkey.api.writer.PrintWriters;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public abstract class PerformanceTest extends BaseWebDriverTest
{
    long elapsedTime = -1;

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    protected PerformanceTest()
    {
        setIsPerfTest(true);
    }

    public void writePerfDataToFile()
    {
        File xmlFile = new File(TestFileUtils.getLabKeyRoot(), "teamcity-info.xml");

        try (Writer writer = PrintWriters.getPrintWriter(xmlFile))
        {
            xmlFile.createNewFile();

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"actionTime\" value=\"" + elapsedTime + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException ignored) {}
    }
}
