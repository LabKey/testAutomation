/*
 * Copyright (c) 2009-2016 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.AssayImporter;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.QCAssayScriptHelper;

/**
 * @deprecated TODO: Move shared functionality to a Helper class
 * This class does not leave enough flexibility in test design.
 */
@Deprecated
public abstract class AbstractQCAssayTest extends AbstractAssayTest
{
    @LogMethod
    public void prepareProgrammaticQC()
    {
        QCAssayScriptHelper javaEngine = new QCAssayScriptHelper(this);
        javaEngine.ensureEngineConfig();
    }

    public void deleteEngine()
    {
        QCAssayScriptHelper javaEngine = new QCAssayScriptHelper(this);
        javaEngine.deleteEngine();
    }

    protected void startCreateNabAssay(String name)
    {
        clickButton("New Assay Design");
        checkRadioButton(Locator.radioButtonByNameAndValue("providerName", "TZM-bl Neutralization (NAb)"));
        clickButton("Next");

        Locator assayName = Locator.xpath("//input[@id='AssayDesignerName']");
        waitForElement(assayName, WAIT_FOR_JAVASCRIPT);
        setFormElement(assayName, name);

        log("Setting up NAb assay");
    }

    /**
     * Import a new run into this assay
     */
    protected void importData(AssayImportOptions options)
    {
        AssayImporter importer = new AssayImporter(this, options);
        importer.doImport();
    }
}
