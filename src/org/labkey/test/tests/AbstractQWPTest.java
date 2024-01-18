/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.tests;

import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractQWPTest extends BaseWebDriverTest
{
    private static final Pair<String, String> QWP_SCHEMA_LISTING = Pair.of("List out all queries in schema", "testSchemaOnly");

    abstract protected List<Pair<String, String>> getTabSignalsPairs();

    protected void testQWPDemoPage()
    {
        log("Begin testing QWPDemo page");
        beginAt("/simpletest/" + getProjectName() + "/QWPDemo.view");

        log("Drop and reload QWPDemo test data");
        clickButton("Drop schema and clear test data");
        waitForElement(Locator.button("Populate test data"));
        clickButton("Populate test data");
        WebElement populateMessage = Locator.id("populatemessage").waitForElement(shortWait());
        new WebDriverWait(getDriver(), Duration.ofSeconds(60)).until(ExpectedConditions.visibilityOf(populateMessage)).getText();
        assertEquals("Test data is populated!", populateMessage.getText());

        log("Testing " + QWP_SCHEMA_LISTING.getLeft());
        click(Locator.linkWithText(QWP_SCHEMA_LISTING.getLeft()));

        String alert = waitForSignalOrAlert(3000, QWP_SCHEMA_LISTING.getRight());
        if (alert != null)
            fail(QWP_SCHEMA_LISTING.getLeft() + " failed: " + alert);
        waitForElement(Locator.css("span.labkey-wp-title-text").withText(QWP_SCHEMA_LISTING.getLeft()));

        getTabSignalsPairs().stream().forEach(this::testQWPTab);

        log("Drop QWPDemo test data");
        beginAt("/simpletest/" + getProjectName() + "/QWPDemo.view");
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
        log("Testing " + titleSignalPair.getLeft());
        click(Locator.linkWithText(titleSignalPair.getLeft()));
        String alert = waitForSignalOrAlert(10000, titleSignalPair.getRight());
        if (alert != null)
            fail(titleSignalPair.getLeft() + " failed: " + alert);

        waitForElement(Locator.css(".labkey-data-region"));
    }

}
