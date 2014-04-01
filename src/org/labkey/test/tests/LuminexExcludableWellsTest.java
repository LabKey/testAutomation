/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexExcludableWellsTest extends LuminexTest
{
    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }
    
    public void runUITests()
    {
        runWellExclusionTest();
    }

}
