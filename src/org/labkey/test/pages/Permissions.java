/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.pages;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;

/**
 * Created by susanh on 4/21/15.
 */
public class Permissions
{
    private BaseWebDriverTest _test;
    private String _projectName;

    /**
     * @param test the test driver in which the page is being used
     * @param projectName name of the project in which permissions are being set or null for site-wide permissions
     */
    public Permissions(BaseWebDriverTest test, @Nullable String projectName)
    {
        this._test = test;
        this._projectName = projectName;
    }
}
