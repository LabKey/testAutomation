/*
 * Copyright (c) 2015 LabKey Corporation
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
            _elements = new Elements();

        return _elements;
    }

    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        final WebElement studyLabelInput = new LazyWebElement(Locator.name("label"), this);
        final WebElement subjectNounSingularInput = new LazyWebElement(Locator.name("subjectNounSingular"), this);
        final WebElement subjectNounPluralInput = new LazyWebElement(Locator.name("subjectNounPlural"), this);
        final WebElement subjectColumnNameInput = new LazyWebElement(Locator.name("subjectColumnName"), this);

        final WebElement timepointTypeRadio(TimepointType type)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("timepointType", type.toString()), this);
        }
        final WebElement startDateInput = new LazyWebElement(Locator.name("startDate"), this);
        final WebElement defaultTimepointDurationInput = new LazyWebElement(Locator.name("defaultTimepointDuration"), this);

        final WebElement repositoryTypeRadio(RepositoryType type)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("simpleRepository", RepositoryType.SIMPLE == type ? "true" : "false"), this);
        }

        final WebElement securityModeSelect = new LazyWebElement(Locator.name("securityString"), this);

        final WebElement shareDatasetsRadio(boolean share)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("shareDatasets", String.valueOf(share)), this);
        }
        final WebElement shareTimepointsRadio(boolean share)
        {
            return new LazyWebElement(Locator.radioButtonByNameAndValue("shareVisits", String.valueOf(share)), this);
        }

        final WebElement createStudyButton = new LazyWebElement(Locator.lkButton("Create Study"), this);
        final WebElement backButton = new LazyWebElement(Locator.lkButton("Back"), this);
    }

}
