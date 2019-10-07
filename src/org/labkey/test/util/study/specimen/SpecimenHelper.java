package org.labkey.test.util.study.specimen;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.study.SpecimenDetailGrid;
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
    public WebDriver getWrappedDriver()
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
