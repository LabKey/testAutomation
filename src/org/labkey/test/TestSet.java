/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import org.labkey.test.bvt.*;
import org.labkey.test.daily.*;
import org.labkey.test.drt.*;
import org.labkey.test.module.ModuleTest;
import org.labkey.test.ms2.MS2ClusterTest;
import org.labkey.test.ms2.MascotTest;
import org.labkey.test.ms2.SequestTest;

import java.util.Arrays;
import java.util.List;

public enum TestSet
{
    DRT(new Class[] {
        BasicTest.class,
        JUnitTest.class,
        SecurityTest.class,
        FlowTest.class,
        XTandemTest.class,
        StudyTest.class
    }),

    BVT(DRT, new Class[] {
        WikiTest.class,
        ExpTest.class,
        AssayTest.class,
        StudyBvtTest.class,
        PipelineBvtTest.class,
        //FileContentTest.class,
        ClientAPITest.class,
        MissingValueIndicatorsTest.class,
        ReportTest.class,
        SchemaBrowserTest.class,
        MicroarrayTest.class,
        ViabilityTest.class
    }),

    MS2(new Class[]
    {
        XTandemTest.class,
        MascotTest.class,
        SequestTest.class
    }),

    Daily(600000, new Class[]
    {
        BasicTest.class,
        ModuleAssayTest.class,
        SpecimenTest.class,
        StudyExtraTest.class,
        NabAssayTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
        DataRegionTest.class,
        UserPermissionsTest.class,
        SampleSetTest.class,
        AuditLogTest.class,
        FieldValidatorTest.class,
        ProgrammaticQCTest.class,
        SchemaBrowserTest.class,
        StudySecurityTest.class,
        MS1Bvt.class,
        UniprotAnnotationTest.class, //requires bootstrap
        HTTPApiTest.class,
        MessagesBvtTest.class, //do we need both MessagesTest and MessagesBvtTest?
        MessagesTest.class,
        MS2BvtTest.class,
        MS2BvtTestGZ.class,           
        WikiBvtTest.class,
        ListTest.class,
        UserBvtTest.class,
        IssuesTest.class,
        NabOldTest.class,
        CaBigTest.class,
        TimelineTest.class,
        DbUserSchemaTest.class,
        MenuBarTest.class,
        LuminexTest.class,
        SimpleModuleTest.class,
        JavaClientApiTest.class,
        QuerySnapshotTest.class,
        SCHARPStudyTest.class,
        IDRIParticleSizeTest.class,
        CohortTest.class,
        RlabkeyTest.class
    }),

    Cluster(new Class[]
    {
        MS2ClusterTest.class
    }),

    XTandem(new Class[]
    {
        XTandemTest.class
    }),

    Mascot(new Class[]
    {
        MascotTest.class
    }),

    Sequest(new Class[]
    {
        SequestTest.class
    }),

    Module(new Class[]
    {
        ModuleTest.class
    }),

    Flow(new Class[] {
        FlowTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
    }),

    Study(new Class[] {
            StudyTest.class,
            StudyBvtTest.class,
            StudyManualTest.class,
            StudyExtraTest.class,
            CohortTest.class,
            AssayTest.class,
            ReportTest.class,
            QuerySnapshotTest.class,
            SCHARPStudyTest.class
    }),

    UnitTests(new Class[] {
        JUnitTest.class
    }),

    Data(new Class[] {
            DataRegionTest.class,
            DbUserSchemaTest.class,
            ListTest.class,
            IssuesTest.class,
    }),

    IDRI(new Class[] {
            IDRIParticleSizeTest.class
    }),

    BVTnDaily(BVT, Daily.tests),

    Weekly(BVTnDaily, new Class[] {
            // Add special test classes, not in daily or BVT.
    }),

    CONTINUE(new Class[] {})
    {
        public boolean isSuite()
        {
            return false;
        }
    },

    TEST(new Class[] {})
    {
        public boolean isSuite()
        {
            return false;
        }
    }
    ;


    public Class[] tests;
    private static final int DEFAULT_CRAWLER_TIMEOUT = 90000;
    private int crawlerTimeout;

    TestSet(int timeout, TestSet set, Class... tests)
    {
        Class[] all = new Class[set.tests.length + tests.length];
        System.arraycopy(set.tests, 0, all, 0, set.tests.length);
        System.arraycopy(tests, 0, all, set.tests.length, tests.length);
        setTests(timeout, all);
    }

    TestSet(TestSet set, Class... tests)
    {
        this(DEFAULT_CRAWLER_TIMEOUT, set, tests);
    }

    TestSet(int timeout, Class... tests)
    {
        setTests(timeout, tests);
    }

    TestSet(Class... tests)
    {
        setTests(DEFAULT_CRAWLER_TIMEOUT, tests);
    }

    void setTests(Class... tests)
    {
        setTests(DEFAULT_CRAWLER_TIMEOUT, tests);
    }

    void setTests(int timeout, Class... tests)
    {
        this.tests = tests;
        crawlerTimeout = timeout;
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
        return Arrays.asList(tests);
    }
}
