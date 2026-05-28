/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.util.study.specimen;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.study.specimen.SpecimenDetailGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SpecimenHelper extends WebDriverWrapper
{
    private final WrapsDriver _driverWrapper;

    public SpecimenHelper(WrapsDriver driverWrapper)
    {
        _driverWrapper = driverWrapper;
    }

    public SpecimenHelper(WebDriver driver)
    {
        this(() -> driver);
    }

    @Override
    public @NotNull WebDriver getWrappedDriver()
    {
        return _driverWrapper.getWrappedDriver();
    }

    public SpecimenDetailGrid findSpecimenDetailGrid()
    {
        return new SpecimenDetailGrid(getDriver());
    }

    public void setupRequestStatuses()
    {
        manageRequestStatuses();
        addRequestStatuses(getDefaultStatuses());
    }

    public void manageRequestStatuses()
    {
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Request Statuses"));
    }

    public void addRequestStatuses(List<RequestStatus> statuses)
    {
        Iterator<RequestStatus> iterator = statuses.iterator();
        while (iterator.hasNext())
        {
            RequestStatus requestStatus = iterator.next();
            setNewStatus(requestStatus);
            if (iterator.hasNext())
                clickButton("Save");
        }
        clickButton("Done");
    }

    private void setNewStatus(RequestStatus requestStatus)
    {
        setFormElement(Locator.name("newLabel"), requestStatus.name);
        setCheckbox(Locator.checkboxByName("newFinalState"), requestStatus.finalState);
        setCheckbox(Locator.checkboxByName("newSpecimensLocked"), requestStatus.lockSpecimens);
    }

    public List<RequestStatus> getDefaultStatuses()
    {
        return Arrays.asList(
                new RequestStatus("New Request"),
                new RequestStatus("Processing"),
                new RequestStatus("Completed", true, true),
                new RequestStatus("Rejected", true, false)
        );
    }

    public static class RequestStatus
    {
        private final String name;
        private final boolean finalState;
        private final boolean lockSpecimens;

        public RequestStatus(String name, boolean finalState, boolean lockSpecimens)
        {
            this.name = name;
            this.finalState = finalState;
            this.lockSpecimens = lockSpecimens;
        }

        public RequestStatus(String name)
        {
            this(name, false, true);
        }
    }
}
