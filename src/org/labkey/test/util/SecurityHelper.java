/*
 * Copyright (c) 2013-2016 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;

public class SecurityHelper
{
    protected BaseWebDriverTest _test;

    public SecurityHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _test._permissionsHelper.setSiteGroupPermissions(groupName, permissionString);
    }

    public void setProjectPerm(String userOrGroupName, String permission)
    {
        _test._permissionsHelper.setPermissions(userOrGroupName, permission);
    }

    public void setProjectPerm(String userOrGroupName, String folder, String permission)
    {
        if(_test.getCurrentContainerPath().equals("/" + folder))
            _test.beginAt("/project/" + folder + "/begin.view?");
        _test._permissionsHelper.setPermissions(userOrGroupName, permission);
    }
}
