/*
 * Copyright (c) 2013 LabKey Corporation
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
import org.labkey.test.Locator;

public class SecurityHelper extends AbstractHelper
{
    private PortalHelper _portalHelper;

    public SecurityHelper(BaseWebDriverTest test)
    {
        super(test);
        _portalHelper = new PortalHelper(test);
    }

    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _test.setSiteGroupPermissions(groupName, permissionString);
    }

    public void openWebpartPermissionWindow(String webpart)
    {
        _portalHelper.clickWebpartMenuItem(webpart, false, "Permissions");
        _test._ext4Helper.waitForMask();
        _test.waitForText("Check Permission");
    }

    /**
     *
     * @param webpart
     * @param permission
     * @param folder null=current folder
     */
    public void setWebpartPermission(String webpart, String permission, String folder)
    {
        openWebpartPermissionWindow(webpart);
        
        _test._ext4Helper.selectComboBoxItem("Required Permission:", permission);

        if(folder==null)
            _test._ext4Helper.selectRadioButton("Check Permission On:", "Current Folder");
        else
        {
            _test._ext4Helper.selectRadioButton("Check Permission On:", "Choose Folder");
            _test.click(Locator.tagWithText("div", folder));
        }
        _test.click(Locator.tagWithText("span", "Save"));
    }

    /**
     *
     * @param webpart
     * @param expectedPermission The permission that is expected to be set.
     * @param expectedFolder The folder that is expected to be selected, null=current folder
     */
    public void checkWebpartPermission(String webpart, String expectedPermission, String expectedFolder)
    {
        openWebpartPermissionWindow(webpart);
        
        _test.assertFormElementEquals("permission", expectedPermission);

        if(expectedFolder == null)
        {
            _test.assertFormElementEquals("permissionContainer", "");
        }
        else
        {
            _test.assertFormElementEquals("permissionContainer", expectedFolder);
        }

        _test.click(Locator.tagWithText("span", "Cancel"));
        _test._ext4Helper.waitForMaskToDisappear();
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
