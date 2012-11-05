package org.labkey.test.tests;

/**
 * User: cnathe
 * Date: 11/5/12
 */
public class TimeChartDateBasedTest extends TimeChartTest
{
    @Override
    protected void doCreateSteps()
    {
        configureStudy();
    }

    @Override
    public void doVerifySteps()
    {
        createChartTest();

        stdDevRegressionTest();

        visualizationTest();

        generateChartPerParticipantTest();

        saveTest();

        timeChartPermissionsTest();

        pointClickFunctionTest();

        multiMeasureTimeChartTest();

        createParticipantGroups();

        participantGroupTimeChartTest();

        multiAxisTimeChartTest();

        aggregateTimeChartUITest();

        filteredTimeChartRegressionTest();
    }
}
