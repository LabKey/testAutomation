/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

public class SecurityHelper extends AbstractHelper
{
    public SecurityHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _test.setSiteGroupPermissions(groupName, permissionString);
    }

    public void setProjectPerm(String userOrGroupName, String permission)
    {
        _test.setPermissions(userOrGroupName, permission);
    }

    public void setProjectPerm(String userOrGroupName, String folder, String permission)
    {
        //if(on project?)
        String projectUrl = "/project/" + folder + "/begin.view?";
        if(_test.getCurrentRelativeURL().equals(projectUrl))
            _test.beginAt(projectUrl);
        _test.setPermissions(userOrGroupName, permission);
    }
}
