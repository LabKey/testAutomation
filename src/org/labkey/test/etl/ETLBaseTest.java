/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test.etl;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.tests.SimpleModuleTest;
import org.labkey.test.util.DataIntegrationHelper;

import java.util.Collections;
import java.util.List;

public abstract class ETLBaseTest extends BaseWebDriverTest
{
    static final String TRANSFORM_APPEND_DESC = "Append Test";
    static final String TRANSFORM_TRUNCATE_DESC = "Truncate Test";
    static final String TRANSFORM_BYRUNID = "{simpletest}/appendIdByRun";
    static final String APPEND_WITH_ROWVERSION = "appendWithRowversion";
    static final String APPEND = "append";
    static final String APPEND_SELECT_ALL = "appendSelectAll";
    static final String TRANSFORM_BAD_THROW_ERROR_SP = "{simpletest}/SProcBadThrowError";
    protected static final String TRANSFORM_APPEND = "{simpletest}/append";
    protected static final String TRANSFORM_TRUNCATE = "{simpletest}/truncate";
    public ETLHelper _etlHelper = new ETLHelper(this, getProjectName());
    protected DataIntegrationHelper _diHelper = _etlHelper.getDiHelper();

    @Nullable
    @Override
    protected abstract String getProjectName();

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("dataintegration");
    }

    @Override
    public void checkQueries()
    {
        log("Skipping query check. Some tables used by queries in simpletest module are not created in this test");
        log("Query check from " + SimpleModuleTest.class.getSimpleName() + " should cover anything this would check");
    }

    protected void doSetup()
    {
        _etlHelper.doSetup();
    }
}
