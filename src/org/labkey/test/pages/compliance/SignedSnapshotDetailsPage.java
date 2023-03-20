/*
 * Copyright (c) 2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.WebElement;

import java.io.File;

public class SignedSnapshotDetailsPage extends LabKeyPage<SignedSnapshotDetailsPage.ElementCache>
{
    public SignedSnapshotDetailsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public String getSourceSchema()
    {
        return elementCache().sourceSchmea.getText();
    }

    public String getSourceQuery()
    {
        return elementCache().sourceQuery.getText();
    }

    public String getSignedFile()
    {
        return elementCache().signedFile.getText();
    }

    public File clickAndDownloadSignedFile()
    {
        return clickAndWaitForDownload(elementCache().signedFile);
    }

    public String getRows()
    {
        return elementCache().rows.getText();
    }

    public String getFileSize()
    {
        return elementCache().fileSize.getText();
    }

    public String getReason()
    {
        return elementCache().reason.getText();
    }

    public String getSignedBy()
    {
        return elementCache().signedBy.getText();
    }

    public String getDateSigned()
    {
        return elementCache().dateSigned.getText();
    }

    public void clickShowGrid()
    {
        clickAndWait(elementCache().showGridBtn);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement sourceSchmea = new RefindingWebElement(Locators.sourceSchema, this);
        WebElement sourceQuery = new RefindingWebElement(Locators.sourceQuery, this);
        WebElement signedFile = new RefindingWebElement(Locators.signedFile, this);
        WebElement rows = new RefindingWebElement(Locators.rows, this);
        WebElement fileSize = new RefindingWebElement(Locators.fileSize, this);
        WebElement reason = new RefindingWebElement(Locators.reason, this);
        WebElement signedBy = new RefindingWebElement(Locators.signedBy, this);
        WebElement dateSigned = new RefindingWebElement(Locators.dateSigned, this);
        WebElement showGridBtn = new RefindingWebElement(Locators.showGridBtn, this);
    }

    protected static class Locators extends org.labkey.test.Locators
    {
        public static final Locator.XPathLocator sourceSchema = Locator.tagWithClass("td","lk-form-label").withText("Source Schema:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator sourceQuery = Locator.tagWithClass("td","lk-form-label").withText("Source Query:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator signedFile = Locator.tagWithClass("td","lk-form-label").withText("Signed File:").notHidden().append("/following-sibling::td/a");
        public static final Locator.XPathLocator rows = Locator.tagWithClass("td","lk-form-label").withText("Rows:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator fileSize = Locator.tagWithClass("td","lk-form-label").withText("File Size:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator reason = Locator.tagWithClass("td","lk-form-label").withText("Reason:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator signedBy = Locator.tagWithClass("td","lk-form-label").withText("Signed By:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator dateSigned = Locator.tagWithClass("td","lk-form-label").withText("Date Signed:").notHidden().append("/following-sibling::td");
        public static final Locator.XPathLocator showGridBtn = Locator.lkButton("Show Grid");
    }

}
