package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

/**
 * Created with IntelliJ IDEA.
 * User: RyanS
 * Date: 9/9/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SchemaHelper extends AbstractHelperWD
{

    public SchemaHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    public void createLinkedSchema(String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(true, projectName, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    @LogMethod(category = LogMethod.MethodType.SETUP)
    public void updateLinkedSchema(String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _editLinkedSchema(false, projectName, targetFolder, name, sourceContainerPath, schemaTemplate, sourceSchemaName, tables, metadata);
    }

    public void _editLinkedSchema(boolean create, String projectName, String targetFolder, String name, String sourceContainerPath, String schemaTemplate, String sourceSchemaName, String tables, String metadata)
    {
        _test.beginAt("/query/" + projectName + "/" + targetFolder + "/admin.view");

        // Click the create new or edit existing link.
        Locator link;
        if (create)
            link = Locator.xpath("//a[text()='new linked schema']");
        else
            link = Locator.xpath("//td[text()='" + name + "']/..//a[text()='edit']");
        _test.waitAndClickAndWait(link);

        _test.waitForElement(Locator.xpath("//input[@name='userSchemaName']"));
        _test.setFormElement(Locator.xpath("//input[@name='userSchemaName']"), name);
        _test.setFormElement(Locator.xpath("//input[@name='dataSource']"), sourceContainerPath);
        _test.waitForElement(Locator.xpath("//li[text()='" + sourceContainerPath + "']"));
        _test.click(Locator.xpath("//li[text()='" + sourceContainerPath + "']"));

        if (schemaTemplate != null)
        {
            _test._shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='schemaTemplate']")));
            _test.setFormElement(Locator.xpath("//input[@name='schemaTemplate']"), schemaTemplate);
            _test.waitForElement(Locator.xpath("//li[text()='" + schemaTemplate + "']"));
            _test.click(Locator.xpath("//li[text()='" + schemaTemplate + "']"));
        }

        if (sourceSchemaName != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('sourceSchemaOverride')/span[text()='Override template value']"));
            }
            _test._shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='sourceSchemaName']")));

            _test.setFormElement(Locator.xpath("//input[@name='sourceSchemaName']"), sourceSchemaName);
            _test.waitForElement(Locator.xpath("//li[text()='"+ sourceSchemaName + "']"));
            _test.click(Locator.xpath("//li[text()='"+ sourceSchemaName + "']"));
        }

        if (tables != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('tablesOverride')/span[text()='Override template value']"));
            }
            _test._shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//input[@name='tables']")));

            _test.click(Locator.xpath("//input[@name='tables']"));
            for (String table : tables.split(","))
            {
                WebElement li = Locator.xpath("//li").containing(table).notHidden().waitForElmement(_test.getDriver(), _test.WAIT_FOR_JAVASCRIPT);
                if (!li.getAttribute("class").contains("selected"))
                    li.click();
            }
            _test.click(Locator.xpath("//input[@name='tables']"));
        }

        if (metadata != null)
        {
            if (schemaTemplate != null)
            {
                // click "Override template value" widget
                _test.click(Locator.xpath("id('metadataOverride')/span[text()='Override template value']"));
            }
            _test._shortWait.until(LabKeyExpectedConditions.elementIsEnabled(Locator.xpath("//textarea[@name='metaData']")));

            _test.setFormElement(Locator.xpath("//textarea[@name='metaData']"), metadata);
        }

        if (create)
            _test.clickButton("Create");
        else
            _test.clickButton("Update");

        // Back on schema admin page, check the linked schema was created/updated.
        _test.waitForElement(Locator.xpath("//td[text()='" + name + "']"));
    }
}
