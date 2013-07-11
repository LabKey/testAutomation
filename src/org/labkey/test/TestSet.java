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
import org.labkey.test.unsupported.PeptideModuleTest;

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

    BVT(
        BasicTest.class,
        JUnitTest.class,
        WikiTest.class,
        ExpTest.class,
        AssayTest.class,
        PipelineTest.class,
        FileContentTest.class,
        ClientAPITest.class,
        ChartingAPITest.class,
        DataReportsTest.class,
        ScatterPlotTest.class,
        MicroarrayTest.class,
        ButtonCustomizationTest.class,
        ReagentTest.class,
        FilterTest.class,
        GroupTest.class,
        StudyRedesignTest.class,
        SecurityTest.class,
        FlowTest.class,
        XTandemTest.class,
        WebDavTest.class
    ),

    //Tests for unsupported/externally developed modules
    External(
        LabModulesTest.class,
        SequenceTest.class,
        ViralLoadAssayTest.class,
        ELISPOT_AssayTest.class,
        HormoneAssayTest.class,
        GenotypeAssaysTest.class,
        PeptideModuleTest.class,
        EHRReportingAndUITest.class,
        EHRDataEntryTest.class,
        EHRApiTest.class,
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
        TargetedMSExperimentTest.class,
        TargetedMSLibraryTest.class,
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
        StudyScheduleTest.class,
        DataViewsTest.class,
        MS1Test.class,
        UniprotAnnotationTest.class, //requires bootstrap
        HTTPApiTest.class,
        MessagesLongTest.class, //do we need both MessagesTest and MessagesLongTest?
        QuantitationTest.class,
        MessagesTest.class,
        SpecimenReplaceTest.class,
        MS2Test.class,
        MS2GZTest.class,
        WikiLongTest.class,
        StudyReloadTest.class,
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
        LuminexExcludedTitrationTest.class,
        SimpleModuleTest.class,
        JavaClientApiTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        SpecimenProgressReportTest.class,
        RISAssayTest.class,
        DatabaseDiagnosticsTest.class,
        StudyDatasetsTest.class,
        AdminConsoleTest.class,
        DatasetPublishTest.class,
        StudyRedesignTest.class,
        KnitrReportTest.class,
        BoxPlotTest.class,
        NonStudyReportsTest.class,
        ParticipantReportTest.class,
        ReportSecurityTest.class,
        AliquotTest.class,
        SpecimenMultipleImportTest.class
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
        TargetedMSExperimentTest.class,
        TargetedMSLibraryTest.class,
        TimeChartDateBasedTest.class,
        TimeChartVisitBasedTest.class,
        TimeChartAPITest.class,
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
        SequestImportTest.class,
        ETLTest.class,
        ReportThumbnailTest.class,
        DrugSensitivityAssayTest.class,
        OConnorExperimentTest.class,
        SpecimenExportTest.class
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
        LuminexPositivityTest.class,
        LuminexExcludedTitrationTest.class
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
        SpecimenReplaceTest.class,
        StudyExportTest.class,
        StudyCohortExportTest.class,
        StudyManualTest.class,
        VaccineProtocolTest.class,
        CohortTest.class,
        AssayTest.class,
        SpecimenMergeTest.class,
        TargetStudyTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        AncillaryStudyTest.class,
        CAVDStudyTest.class,
        ExtraKeyStudyTest.class,
        SampleMindedImportTest.class,
        StudyPublishTest.class
    ),

    Reports(
        TimeChartVisitBasedTest.class,
        TimeChartDateBasedTest.class,
        TimeChartAPITest.class,
        DataReportsTest.class,
        BoxPlotTest.class,
        ScatterPlotTest.class,
        NonStudyReportsTest.class,
        ParticipantReportTest.class,
        ReportSecurityTest.class,
        KnitrReportTest.class
    ),

    Specimen
    (
        StudyTest.class,
        SpecimenReplaceTest.class,
        SpecimenMergeTest.class,
        SampleMindedImportTest.class,
        CreateVialsTest.class,
        SpecimenExportTest.class,
        AliquotTest.class,
        SpecimenMultipleImportTest.class
    ),

    Assays(
        AssayTest.class,
        MissingValueIndicatorsTest.class,
        ElispotAssayTest.class,
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
        LuminexExcludedTitrationTest.class,
        ViabilityTest.class,
        ModuleAssayTest.class,
        FormulationsTest.class,
        ElisaAssayTest.class,
        AffymetrixAssayTest.class
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
        EHRApiTest.class,
        ONPRC_EHRTest.class
    ),

    ONPRC(External, EHR.getTestList()),

    CDSPopulation(
        CDSPopulation.class
    ),

    InDevelopment(
        MassFilterTest.class,
        ListPublishTest.class,
        StudyWDTest.class, // Broken
        CDSTest.class, // Broken
        ONPRC_EHRTest.class, // Unknown?
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
        HashSet<Class> all = new HashSet<>();

        for (List<Class> tests: testLists)
        {
            all.addAll(tests);
        }

        _tests = new ArrayList<>(all);
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
        List<String> testNames = new ArrayList<>();
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
