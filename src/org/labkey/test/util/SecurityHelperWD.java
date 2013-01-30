package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 1/17/13
 * Time: 11:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class SecurityHelperWD extends AbstractHelperWD
{
    public SecurityHelperWD(BaseWebDriverTest test)
    {
        super(test);
    }

    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _test.setSiteGroupPermissions(groupName, permissionString);
    }


    /**
     *
     * @param webpart
     * @param permission
     * @param folder null=current folder
     */
    public void setWebpartPermission(String webpart, String permission, String folder)
    {
        _test.click(Locator.xpath("//th[@title='" + webpart + "']/span/a/img"));
        Locator permissions = Locator.tagWithText("span", "Permissions");
        _test.waitForElement(permissions);
        _test.click(permissions);
        _test._ext4Helper.waitForMask();
        _test.waitForText("Check Permission");
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
