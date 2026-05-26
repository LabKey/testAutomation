/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.util;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.LabKeySiteWrapper;

/**
 * Bootstrap a server without the initial user validation done by {@link LabKeySiteWrapper#signIn()}
 * Not actually a test. Just piggy-backing on the test harness to make it easier to run.
 */
@Category({})
public class QuickBootstrapPseudoTest
{
    @Test
    public void bootstrap()
    {
        new ApiBootstrapHelper().signIn();
    }
}
