package org.labkey.test.tests;

import java.io.File;

/**
 * User: cnathe
 * Date: 11/5/12
 */
public class TimeChartAPITest extends TimeChartTest
{
    @Override
    public void doVerifySteps()
    {
        getDataDateTest();
        getDataVisitTest();
        createParticipantGroups();
        modifyParticipantGroups();
        aggregateTimeChartSQLTest();
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/" + TEST_DATA_API_PATH + "/timechart-api.xml")};
    }
}
