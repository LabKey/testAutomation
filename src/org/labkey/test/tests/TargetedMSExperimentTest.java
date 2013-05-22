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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4HelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.UIContainerHelper;

public class TargetedMSExperimentTest extends TargetedMSTest
{

    public TargetedMSExperimentTest()
    {
        // We want to use the UI when creating the project/folder so that we can verify that we get the wizard
        // that has the extra steps
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setupAndImportData(FolderType.Experiment);
        verifyImportedData();
        verifyModificationSearch();
    }

    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyImportedData()
    {
        clickAndWait(Locator.linkContainingText("Targeted MS Dashboard"));
        clickAndWait(Locator.linkContainingText(SKY_FILE));
        verifyRunSummaryCounts();
        verifyPeptide();
    }
}
