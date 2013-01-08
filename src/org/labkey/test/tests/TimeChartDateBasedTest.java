/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.test.tests;

import org.labkey.test.TestTimeoutException;

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

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsers(afterTest, USER1, USER2);
        super.doCleanup(afterTest);
    }
}
