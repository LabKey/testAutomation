package org.labkey.test.tests;

/**
 * User: cnathe
 * Date: 11/2/12
 */
public class TimeChartVisitBasedTest extends TimeChartTest
{
    @Override
    protected void doCreateSteps()
    {
        configureVisitStudy();
    }

    @Override
    public void doVerifySteps()
    {
        visitBasedChartTest();
    }
}
