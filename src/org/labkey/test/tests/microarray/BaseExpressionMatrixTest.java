package org.labkey.test.tests.microarray;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaseExpressionMatrixTest extends BaseWebDriverTest
{
    protected static final String PIPELINE_NAME = "create-matrix";
    protected static final String ASSAY_NAME = "Test Expression Matrix";
    protected static final File FEATURE_SET_DATA = TestFileUtils.getSampleData("Microarray/expressionMatrix/sample-feature-set.txt");
    protected static final File CEL_FILE1 = TestFileUtils.getSampleData("Affymetrix/CEL_files/sample_file_1.CEL");
    protected static final File CEL_FILE2 = TestFileUtils.getSampleData("Affymetrix/CEL_files/sample_file_2.CEL");
    private static int featureSetId;

    protected String getFeatureSetName()
    {
        return getClass().getSimpleName() + " Feature Set";
    }

    protected int getFeatureSetId()
    {
        return featureSetId;
    }

    @BeforeClass
    public static void doSetup()
    {
        BaseExpressionMatrixTest initTest = (BaseExpressionMatrixTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    private void doSetupSteps()
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("pipelinetest", "Microarray"));

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.enterAdminMode();
        {
            clickProject(getProjectName());
            featureSetId = createFeatureSet(getFeatureSetName(), portalHelper);

            clickProject(getProjectName());
            portalHelper.addWebPart("Assay List");
            _assayHelper.createAssayWithDefaults("Expression Matrix", ASSAY_NAME);
        }
        portalHelper.exitAdminMode();

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(CEL_FILE1);
        _fileBrowserHelper.uploadFile(CEL_FILE2);
    }

    @LogMethod
    protected int createFeatureSet(@LoggedParam String name, PortalHelper portalHelper)
    {
        if (!isElementPresent(PortalHelper.Locators.webPart("Feature Annotation Sets")))
        {
            portalHelper.addWebPart("Feature Annotation Sets");
        }
        clickButton("Import Feature Annotation Set");
        waitForElement(Locator.name("name"));
        setFormElement(Locator.name("name"), name);
        setFormElement(Locator.name("vendor"), "Vendor");
        setFormElement(Locator.name("annotationFile"), BaseExpressionMatrixTest.FEATURE_SET_DATA);
        clickButton("upload");
        clickAndWait(Locator.linkWithText(name));
        return Integer.parseInt(getUrlParam("rowId"));
    }

    protected String getParameterXml(String assayRunName, String protocolName, String description, String featureSet, boolean importValues, Map<String, String> otherParameters)
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("protocolName", protocolName);
        parameters.put("assay name", assayRunName);
        if (description != null)
            parameters.put("assay comments", description);
        if (featureSet != null)
            parameters.put("assay run property, featureSet", featureSet);
        parameters.put("assay run property, importValues", String.valueOf(importValues));

        if (otherParameters != null)
            parameters.putAll(otherParameters);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        sb.append("<bioml>\n");
        for (Map.Entry<String, String> entry : parameters.entrySet())
            sb.append(String.format("  <note label='%s' type='input'>%s</note>\n", entry.getKey(), entry.getValue()));
        sb.append("</bioml>\n");

        return sb.toString();
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("microarray");
    }

    @Override
    protected WebDriverWrapper.BrowserType bestBrowser()
    {
        return WebDriverWrapper.BrowserType.CHROME;
    }
}
