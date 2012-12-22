package org.labkey.test.tests.perf;

import org.labkey.test.BaseWebDriverTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 12/5/12
 * Time: 11:48 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PerformanceTest extends BaseWebDriverTest
{
    long elapsedTime = -1;

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected PerformanceTest()
    {
        setIsPerfTest(true);
    }

    public void writePerfDataToFile()
    {
        FileWriter writer = null;
        try
        {
            File xmlFile = new File(getLabKeyRoot(), "teamcity-info.xml");
            xmlFile.createNewFile();
            writer = new FileWriter(xmlFile);

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + elapsedTime + "\"/>\n");
//            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
//            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException e)
        {
            return;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                }
        }

    }
}
