package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class InsertIssueDefPage extends LabKeyPage
{
    private final String _dataRegionName;
    private final Input _labelInput;

    private InsertIssueDefPage(WebDriver driver, String originDataRegionName)
    {
        super(driver);
        _dataRegionName = originDataRegionName;
        _labelInput = Input(Locator.input("quf_Label"), getDriver()).waitFor(getDriver());
    }

    private InsertIssueDefPage(WebDriver driver)
    {
        this(driver, IssueListDefDataRegion.NAME_IN_QUERY);
    }

    public InsertIssueDefPage(WebDriver driver, IssueListDefDataRegion originDataRegion)
    {
        this(driver, originDataRegion.getTableName());
    }

    public void setLabel(String label)
    {
        _labelInput.set(label);
    }

    public void selectKind(String kind)
    {
        WebElement input = Locator.tagWithName("select", "quf_Kind").findElement(getDriver());
        new OptionSelect(input).set(kind);
    }

    public CreateListDefConfirmation clickSubmit()
    {
        click(Locator.lkButton("Submit"));
        return new CreateListDefConfirmation(getDriver());
    }

    public IssueListDefDataRegion clickCancel()
    {
        clickAndWait(Locator.lkButton("Cancel"));
        return new IssueListDefDataRegion(_dataRegionName, getDriver());
    }

    public class CreateListDefConfirmation extends Window
    {
        private CreateListDefConfirmation(WebDriver driver)
        {
            super("Create Issue List Definition?", driver);
        }

        public IssueListDefDataRegion clickYes()
        {
            clickButton("Yes");
            return new IssueListDefDataRegion(_dataRegionName, getDriver());
        }

        public InsertIssueDefPage clickYesError()
        {
            clickButton("Yes");
            return new InsertIssueDefPage(getDriver(), _dataRegionName);
        }

        public InsertIssueDefPage clickNo()
        {
            clickButton("No", true);
            return InsertIssueDefPage.this;
        }
    }
}
