package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.module.EHRStudyTest;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 8/6/12
 * Time: 10:49 AM
 */
public class EHRTestHelper
{
    private BaseSeleniumWebTest _test;

    public EHRTestHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }

    public Locator getAnimalHistoryRadioButtonLocator(String groupName, String setting)
    {
        //not sure why the radios are in TH elements, but they are...
        return Locator.xpath("//form[@id='groupUpdateForm']/table/tbody/tr/td[text()='"
                + groupName + "']/../th/input[@value='" + setting + "']");
    }

    public String getAnimalHistoryDataRegionName(String title)
    {
        // Specific to the EHR Animal History page.
        _test.waitForElement(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='"+title+"' or starts-with(text(), '"+title+":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), _test.WAIT_FOR_JAVASCRIPT * 3);
        return _test.getAttribute(Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='" + title + "' or starts-with(text(), '" + title + ":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]"), "id").substring(11);
    }

    public void selectDataEntryRecord(String query, String Id, boolean keepExisting)
    {
        _test.getWrapper().getEval("selenium.selectExtGridItem('Id','" + Id + "', -1, 'ehr-" + query + "-records-grid', "+keepExisting+");");
        if(!keepExisting)_test.waitForElement(Locator.xpath("//div[@id='Id']/a[text()='"+Id+"']"), _test.WAIT_FOR_JAVASCRIPT);
    }

    public void clickVisibleButton(String text)
    {
        _test.click(Locator.xpath("//button[text()='"+text+"' and "+ EHRStudyTest.VISIBLE+" and not(contains(@class, 'x-hide-display'))]"));
    }

    public void setDataEntryField(String tabName, String fieldName, String value)
    {
        _test.setFormElement(Locator.xpath("//div[./div/span[text()='" + tabName + "']]//*[(self::input or self::textarea) and @name='" + fieldName + "']"), value);
        _test.fireEvent(Locator.xpath("//div[./div/span[text()='" + tabName + "']]//*[(self::input or self::textarea) and @name='" + fieldName + "']"), BaseSeleniumWebTest.SeleniumEvent.blur);
    }

    public void setPDP(EHRStudyTest.EHRUser user)
    {
        int col = _test.getWrapper().getXpathCount("//table[@id='datasetSecurityFormTable']//th[.='"+user.getGroup()+"']/preceding-sibling::*").intValue() + 1;
        int rowCt = _test.getTableRowCount("datasetSecurityFormTable");
        for (int i = 3; i <= rowCt; i++) // xpath indexing is 1 based
        {
            _test.selectOptionByText(Locator.xpath("//table[@id='datasetSecurityFormTable']/tbody/tr["+i+"]/td["+col+"]//select"), user.getRole().toString());
        }
    }

}
