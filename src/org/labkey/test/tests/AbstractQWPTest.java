package org.labkey.test.tests;

import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.openqa.selenium.Alert;

import java.util.List;

import static org.junit.Assert.fail;

public abstract class AbstractQWPTest extends BaseWebDriverTest
{
    private static final Pair<String, String> QWP_SCHEMA_LISTING = new Pair<>("List out all queries in schema", "testSchemaOnly");

    abstract protected List<Pair<String, String>> getTabSignalsPairs();

    protected void testQWPDemoPage()
    {
        log("Begin testing QWPDemo page");
        beginAt("/query/" + getProjectName() + "/QWPDemo.view");

        log("Drop and reload QWPDemo test data");
        clickButton("Drop schema and clear test data");
        waitForElement(Locator.button("Populate test data"));
        clickButton("Populate test data");
        sleep(1000);

        log("Testing " + QWP_SCHEMA_LISTING.first);
        click(Locator.linkWithText(QWP_SCHEMA_LISTING.first));

        String alert = waitForSignalOrAlert(3000, QWP_SCHEMA_LISTING.second);
        if (alert != null)
            fail(QWP_SCHEMA_LISTING.first + " failed: " + alert);
        waitForElement(Locator.css("span.labkey-wp-title-text").withText(QWP_SCHEMA_LISTING.first));

        getTabSignalsPairs().stream().forEach(this::testQWPTab);

        log("Drop QWPDemo test data");
        beginAt("/query/" + getProjectName() + "/QWPDemo.view");
        clickButton("Drop schema and clear test data"); // drop domain, needed for clean up project
    }

    // check every 500ms for specified wait amount for either alert or successSignal
    private String waitForSignalOrAlert(long wait, String successSignal)
    {
        long t= System.currentTimeMillis();
        long end = t + wait;
        while (System.currentTimeMillis() < end)
        {
            Alert alert = getAlertIfPresent();
            if (null != alert)
            {
                String alertText = alert.getText();
                alert.accept();
                return alertText;
            }
            if (isElementPresent(Locators.pageSignal(successSignal)))
                return null;
            sleep(500);
        }
        return "Test signal did not appear - " + successSignal;
    }

    private void testQWPTab(Pair<String, String> titleSignalPair)
    {
        log("Testing " + titleSignalPair.first);
        click(Locator.linkWithText(titleSignalPair.first));
        String alert = waitForSignalOrAlert(10000, titleSignalPair.second);
        if (alert != null)
            fail(titleSignalPair.first + " failed: " + alert);

        waitForElement(Locator.css(".labkey-data-region"));
    }

}
