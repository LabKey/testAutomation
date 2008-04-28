package org.labkey.test;

import org.labkey.test.bvt.*;
import org.labkey.test.daily.UniprotAnnotationTest;
import org.labkey.test.drt.*;
import org.labkey.test.httpunit.MS2AnalyzePerfTest;
import org.labkey.test.httpunit.MS2LoadPerfTest;
import org.labkey.test.module.ModuleTest;
import org.labkey.test.ms2.MS2ClusterTest;
import org.labkey.test.ms2.MascotTest;
import org.labkey.test.ms2.SequestTest;

import java.util.List;
import java.util.Arrays;

public enum TestSet
{
    BVT(new Class[]
    {
        BasicTest.class,
        FlowJoQueryTest.class,
        AssayTest.class,
        LuminexTest.class,
        ListTest.class,
        StudyBvtTest.class,
        StudyExtraTest.class,
        IssuesBvtTest.class,
        UserBvtTest.class,
        UserPermissionsTest.class,
        SpecimenTest.class,
        MS2BvtTest.class,
        SampleSetTest.class,
        MS1Bvt.class,
        CaBigTest.class,
        FileContentTest.class,
        DataRegionTest.class,
        ClientAPITest.class,
        AuditLogTest.class
    }),

    DRT(new Class[]
    {
        BasicTest.class,
        JUnitTest.class,
        SecurityTest.class,
        ExpTest.class,
        FlowTest.class,
        MessagesTest.class,
        XTandemTest.class,
        WikiTest.class,
        StudyTest.class,
        NabTest.class,
        IssuesTest.class,
    }),

    MS2(new Class[]
    {
        XTandemTest.class,
        MascotTest.class,
        SequestTest.class
    }),

    Daily(new Class[]
    {
        UniprotAnnotationTest.class   
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

    Perf(new Class[]
    {
        MS2LoadPerfTest.class,
        MS2AnalyzePerfTest.class
    }),

    Flow(new Class[] {
        FlowTest.class,
        FlowJoQueryTest.class
    }),

    Study(new Class[] {
            StudyBvtTest.class,
            StudyExtraTest.class,
            AssayTest.class
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

    TestSet(Class[] tests)
    {
        setTests(tests);
    }

    void setTests(Class[] tests)
    {
        this.tests = tests;
    }

    public boolean isSuite()
    {
        return true;
    }

    public List<Class> getTestList()
    {
        return Arrays.asList(tests);
    }
}
