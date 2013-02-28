/*
 * Copyright (c) 2005-2013 LabKey Corporation
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

package org.labkey.test;

import org.labkey.test.module.*;
import org.labkey.test.ms2.MS2ClusterTest;
import org.labkey.test.ms2.MascotTest;
import org.labkey.test.ms2.QuantitationTest;
import org.labkey.test.ms2.SequestImportTest;
import org.labkey.test.ms2.SequestTest;
import org.labkey.test.tests.*;
import org.labkey.test.tests.perf.StudyImportPerfTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public enum TestSet
{
    DRT(
        BasicTest.class,
        JUnitTest.class,
        SecurityShortTest.class,
        FlowShortTest.class,
        XTandemShortTest.class,
        StudyShortTest.class
    ),
    //new tests added to the DRTs should be added to BVT as well

    BVT(
        BasicTest.class,
        JUnitTest.class,
        WikiTest.class,
        ExpTest.class,
        AssayTest.class,
        PipelineTest.class,
        FileContentTest.class,
        ClientAPITest.class,
        ReportTest.class,
        MicroarrayTest.class,
        ButtonCustomizationTest.class,
        ReagentTest.class,
        FilterTest.class,
        GroupTest.class,
        StudyRedesignTest.class,
        SecurityTest.class,
        FlowTest.class,
        XTandemTest.class,
//        StudyWDTest.class,
        WebDavTest.class
    ),

    Unsupported(
        LabModulesTest.class,
        SequenceTest.class,
        ViralLoadAssayTest.class,
        ELISPOT_AssayTest.class,
        ElectrochemiluminescenceAssayTest.class,
        GenotypeAssaysTest.class,
        FlowAssaysTest.class
    ),

    Perf(
        StudyImportPerfTest.class
    ),

    MS2(
        XTandemTest.class,
        MascotTest.class,
        SequestTest.class,
        MS2Test.class,
        MS2GZTest.class,
        LibraTest.class,
        TargetedMSTest.class,
        SequestImportTest.class
    ),

    DailyA(600000,
        BasicTest.class,
        EmbeddedWebPartTest.class,
        ModuleAssayTest.class,
        SpecimenTest.class,
        VaccineProtocolTest.class,
        NabAssayTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
//        FlowNormalizationTest.class, // TODO: R scripts stalling in pipeline
        FlowCBCTest.class,
        FlowSpecimenTest.class,
        DataRegionTest.class,
        UserPermissionsTest.class,
        SampleSetTest.class,
        AuditLogTest.class,
        FieldValidatorTest.class,
        ProgrammaticQCTest.class,
        SchemaBrowserTest.class,
        StudySecurityTest.class,
        MS1Test.class,
        UniprotAnnotationTest.class, //requires bootstrap
        HTTPApiTest.class,
        MessagesLongTest.class, //do we need both MessagesTest and MessagesLongTest?
        QuantitationTest.class,
        MessagesTest.class,
        MS2Test.class,
        MS2GZTest.class,
        WikiLongTest.class,
        ListTest.class,
        UserTest.class,
        IssuesTest.class,
        NabOldTest.class,
        TimelineTest.class,
        ExternalSchemaTest.class,
        LinkedSchemaTest.class,
        MenuBarTest.class,
        AssayAPITest.class,
        LuminexUploadAndCopyTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexJavaTransformTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class,
        SimpleModuleTest.class,
        JavaClientApiTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        SpecimenProgressReportTest.class,
        RISAssayTest.class,
        DatabaseDiagnosticsTest.class,
        StudyDatasetsTest.class,
        AdminConsoleTest.class
    ),

    DailyB(600000,
        FormulationsTest.class,
        CohortTest.class,
        RlabkeyTest.class,
        ScriptValidationTest.class,
        SearchTest.class,
        WorkbookTest.class,
        CustomizeViewTest.class,
        ElispotAssayTest.class,
        CreateVialsTest.class,
        SpecimenMergeTest.class,
        TargetStudyTest.class,
        TargetedMSTest.class,
        TimeChartDateBasedTest.class,
        TimeChartVisitBasedTest.class,
        TimeChartAPITest.class,
        EHRReportingAndUITest.class,
        EHRDataEntryTest.class,
        EHRApiTest.class,
        //ONPRC_EHRTest.class,
        ComplianceTrainingTest.class,
        GpatAssayTest.class,
        FolderTest.class,
        StudyDemoModeTest.class,
        LibraTest.class,
        AncillaryStudyTest.class,
        AncillaryStudyFromSpecimenRequestTest.class,
        FolderExportTest.class,
        GenotypingTest.class,
        HaplotypeAssayTest.class,
        MissingValueIndicatorsTest.class,
        PipelineCancelTest.class,
        StudyExportTest.class,
        StudyProtectedExportTest.class,
        StudyCohortExportTest.class,
        ViabilityTest.class,
        ProjectSettingsTest.class,
        //CDSTest.class, // Broken
        ExtraKeyStudyTest.class,
        CAVDStudyTest.class,
        SampleMindedImportTest.class,
        PivotQueryTest.class,
        StudyPublishTest.class,
        DatabaseDiagnosticsTest.class,
        HiddenEmailTest.class,
        ElisaAssayTest.class,
        ContainerContextTest.class,
        ICEMRModuleTest.class,
        FlowAnalysisResolverTest.class,
        WebpartPermissionsTest.class,
        SurveyTest.class,
        NWBioTrustTest.class,
        SequestImportTest.class
    ),

    Daily(600000, DailyA.getTestList(), DailyB.getTestList()),

    MiniTest(
        LuminexUploadAndCopyTest.class,
        LuminexJavaTransformTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class
    ),

    IE(
        BasicTest.class,
        FlowTest.class,
        SecurityTest.class,
        WikiTest.class,
        GpatAssayTest.class,
        EmbeddedWebPartTest.class,
        AssayTest.class,
        FolderTest.class,
        StudyTest.class
    ),

    Cluster(
        MS2ClusterTest.class
    ),

    XTandem(
        XTandemTest.class
    ),

    Mascot(
        MascotTest.class
    ),

    Sequest(
        SequestTest.class
    ),

    Module(ModuleTest.class),

    Flow(
        FlowTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
        FlowNormalizationTest.class,
        FlowCBCTest.class,
        FlowSpecimenTest.class
    ),

    // Many (but not all) of the tests that use wiki functionality
    Wiki(
        WikiTest.class,
        WikiLongTest.class,
        ClientAPITest.class,
        ButtonCustomizationTest.class,
        EmbeddedWebPartTest.class,
        TimelineTest.class
    ),

    Study(
        StudyTest.class,
        StudyExportTest.class,
        StudyCohortExportTest.class,
        StudyManualTest.class,
        VaccineProtocolTest.class,
        CohortTest.class,
        AssayTest.class,
        SpecimenMergeTest.class,
        TargetStudyTest.class,
        ReportTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        AncillaryStudyTest.class,
        CAVDStudyTest.class,
        ExtraKeyStudyTest.class,
        SampleMindedImportTest.class,
        StudyPublishTest.class
    ),

    Assays(
        AssayTest.class,
        MissingValueIndicatorsTest.class,
//        ElispotAssayTest.class,
        TargetStudyTest.class,
        NabOldTest.class,
        NabAssayTest.class,
        NabHighThroughputAssayTest.class,
        LuminexUploadAndCopyTest.class,
        LuminexExcludableWellsTest.class,
        LuminexMultipleCurvesTest.class,
        LuminexJavaTransformTest.class,
        LuminexRTransformTest.class,
        LuminexEC50Test.class,
        LuminexGuideSetTest.class,
        LuminexAsyncImportTest.class,
        LuminexPositivityTest.class,
        ViabilityTest.class,
        ModuleAssayTest.class,
        FormulationsTest.class,
        ElisaAssayTest.class
    ),

    UnitTests(
        JUnitTest.class
    ),

    Chrome(
        ListExportTest.class
    ),

    Data(
        DataRegionTest.class,
        ExternalSchemaTest.class,
        LinkedSchemaTest.class,
        ListTest.class,
        IssuesTest.class,
        ScriptValidationTest.class,
        FilterTest.class,
        PivotQueryTest.class,
        ContainerContextTest.class
    ),

    IDRI(
        FormulationsTest.class
    ),

    BVTnDaily(BVT, Daily._tests),

    Weekly(600000, BVTnDaily,
        // Add special test classes, not in daily or BVT.
        SecurityTestExtended.class,
        NabMigrationTest.class
    ),

    EHR(
        EHRReportingAndUITest.class,
        EHRDataEntryTest.class,
        EHRApiTest.class
        //ONPRC_EHRTest.class
    ),

    ONPRC(Unsupported, EHR.getTestList()),

    CDSPopulation(
        CDSPopulation.class
    ),

    InDevelopment(
        StudyReloadTest.class,
        ExperimentalFeaturesTest.class // currently no experimental features being tested
    ),

    CONTINUE()
    {
        public boolean isSuite()
        {
            return false;
        }
    },

    TEST()
    {
        public boolean isSuite()
        {
            return false;
        }
    }
    ;


    private List<Class> _tests;
    private static final int DEFAULT_CRAWLER_TIMEOUT = 90000;
    private int crawlerTimeout;

    TestSet(int timeout, List<Class>... testLists)
    {
        HashSet<Class> all = new HashSet<Class>();

        for (List<Class> tests: testLists)
        {
            all.addAll(tests);
        }

        _tests = new ArrayList<Class>(all);
        crawlerTimeout = timeout;
    }

    TestSet(int timeout, TestSet set, Class... tests)
    {
        this(timeout, set.getTestList(), Arrays.asList(tests));
    }

    TestSet(TestSet set, List<Class> tests)
    {
        this(DEFAULT_CRAWLER_TIMEOUT, set.getTestList(), tests);
    }

    TestSet(int timeout, Class... tests)
    {
        this(timeout, Arrays.asList(tests));
    }

    TestSet(Class... tests)
    {
        this(DEFAULT_CRAWLER_TIMEOUT, Arrays.asList(tests));
    }

    void setTests(List<Class> tests)
    {
        _tests = tests;
    }

    public boolean isSuite()
    {
        return true;
    }

    public int getCrawlerTimeout()
    {
        return crawlerTimeout;
    }

    public List<Class> getTestList()
    {
        return _tests;
    }

    public List<String> getTestNames()
    {
        ArrayList<String> testNames = new ArrayList<String>();
        for (Class test : _tests)
            testNames.add(test.getSimpleName());
        return testNames;
    }

    // Move the named test to the Nth position in the list, maintaining the order of all other tests.
    public boolean prioritizeTest(Class priorityTest, int N)
    {
        if (_tests.contains(priorityTest))
        {
            _tests.remove(priorityTest);
            _tests.add(N, priorityTest);
            return true;
        }
        return false;
    }

    public void randomizeTests()
    {
        Collections.shuffle(_tests);
    }
}
