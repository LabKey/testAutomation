package org.labkey.test.components.ui.workflow;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

public class DefineTemplateTaskRow extends DefineTaskRowBase
{

    public DefineTemplateTaskRow(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    public DefineTemplateTaskRow setName(String name)
    {
        super.setName(name);
        return this;
    }

    @Override
    public DefineTemplateTaskRow setDescription(String description)
    {
        super.setDescription(description);
        return this;
    }

    @Override
    public DefineTemplateTaskRow setAssaysToPerform(String assayName)
    {
        return setAssaysToPerform(Arrays.asList(assayName));
    }

    @Override
    public DefineTemplateTaskRow setAssaysToPerform(List<String> assayNames)
    {
        super.setAssaysToPerform(assayNames);
        return this;
    }

    @Override
    public DefineTemplateTaskRow clearAssaysToPerform()
    {
        super.clearAssaysToPerform();
        return this;
    }

    @Override
    public DefineTemplateTaskRow deleteTask()
    {
        super.deleteTask();
        return this;
    }

}
