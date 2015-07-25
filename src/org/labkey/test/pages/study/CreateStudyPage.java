package org.labkey.test.pages.study;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import static org.labkey.test.util.StudyHelper.RepositoryType;
import static org.labkey.test.util.StudyHelper.SecurityMode;
import static org.labkey.test.util.StudyHelper.TimepointType;

public class CreateStudyPage extends LabKeyPage
{
    private Elements _elements;

    public CreateStudyPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public CreateStudyPage setLabel(String label)
    {
        _test.setFormElement(elements().studyLabelInput, label);
        return this;
    }

    public CreateStudyPage setSubjectNounSingular(String nounSingular)
    {
        _test.setFormElement(elements().subjectNounSingularInput, nounSingular);
        return this;
    }

    public CreateStudyPage setSubjectNounPlural(String nounPlural)
    {
        _test.setFormElement(elements().subjectNounPluralInput, nounPlural);
        return this;
    }

    public CreateStudyPage setSubjectColumnName(String columnName)
    {
        _test.setFormElement(elements().subjectColumnNameInput, columnName);
        return this;
    }

    public CreateStudyPage setTimepointType(TimepointType type)
    {
        elements().timepointTypeRadio(type).click();
        return this;
    }

    public CreateStudyPage setStartDate(String startDate)
    {
        _test.setFormElement(elements().startDateInput, startDate);
        return this;
    }

    public CreateStudyPage setDefaultTimepointDuration(String defaultTimepointDuration)
    {
        _test.setFormElement(elements().defaultTimepointDurationInput, defaultTimepointDuration);
        return this;
    }

    public CreateStudyPage setRepositoryType(RepositoryType repositoryType)
    {
        elements().repositoryTypeRadio(repositoryType).click();
        return this;
    }

    public CreateStudyPage setSecurityMode(SecurityMode securityMode)
    {
        _test.selectOptionByValue(elements().securityModeSelect, securityMode.toString());
        return this;
    }

    public CreateStudyPage setSharedDatasets(boolean share)
    {
        elements().shareDatasetsRadio(share).click();
        return this;
    }

    public CreateStudyPage setShareTimepoints(boolean share)
    {
        elements().shareTimepointsRadio(share).click();
        return this;
    }

    public LabKeyPage createStudy()
    {
        _test.clickAndWait(elements().createStudyButton);
        return new LabKeyPage(_test);
    }

    public LabKeyPage cancel()
    {
        _test.clickAndWait(elements().backButton);
        return new LabKeyPage(_test);
    }

    private Elements elements()
    {
        if (null == _elements)
            _elements = new Elements(_test.getDriver());

        return _elements;
    }

    private class Elements extends ComponentElements
    {
        protected Elements(SearchContext context)
        {
            super(context);
        }

        final WebElement studyLabelInput = new LazyWebElement(Locator.name("label"), context);
        final WebElement subjectNounSingularInput = new LazyWebElement(Locator.name("subjectNounSingular"), context);
        final WebElement subjectNounPluralInput = new LazyWebElement(Locator.name("subjectNounPlural"), context);
        final WebElement subjectColumnNameInput = new LazyWebElement(Locator.name("subjectColumnName"), context);

        final WebElement timepointTypeRadio(TimepointType type)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("timepointType", type.toString()), context);
        }
        final WebElement startDateInput = new LazyWebElement(Locator.name("startDate"), context);
        final WebElement defaultTimepointDurationInput = new LazyWebElement(Locator.name("defaultTimepointDuration"), context);

        final WebElement repositoryTypeRadio(RepositoryType type)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("simpleRepository", RepositoryType.SIMPLE == type ? "true" : "false"), context);
        }

        final WebElement securityModeSelect = new LazyWebElement(Locator.name("securityString"), context);

        final WebElement shareDatasetsRadio(boolean share)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("shareDatasets", String.valueOf(share)), context);
        }
        final WebElement shareTimepointsRadio(boolean share)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("shareVisits", String.valueOf(share)), context);
        }

        final WebElement createStudyButton = new LazyWebElement(Locator.lkButton("Create Study"), context);
        final WebElement backButton = new LazyWebElement(Locator.lkButton("Back"), context);
    }

}
