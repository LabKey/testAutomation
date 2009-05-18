/*
 * Copyright (c) 2005-2009 LabKey Corporation
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
import org.labkey.test.daily.UniprotAnnotationTest;
import org.labkey.test.daily.FlowImportTest;
import org.labkey.test.drt.*;
import org.labkey.test.module.ModuleTest;
import org.labkey.test.ms2.MS2ClusterTest;
import org.labkey.test.ms2.MascotTest;
import org.labkey.test.ms2.SequestTest;

import java.util.List;
import java.util.Arrays;

public enum TestSet
{
    DRT(new Class[] {
        BasicTest.class,
        JUnitTest.class,
        SecurityTest.class,
        ExpTest.class,
        FlowTest.class,
        XTandemTest.class,
        WikiTest.class,
        StudyTest.class
    }),

    BVT(DRT, new Class[] {
        MessagesTest.class,
        NabTest.class,
        IssuesTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
        AssayTest.class,
        LuminexTest.class,
        ListTest.class,
        StudyBvtTest.class,
        StudyExtraTest.class,
        UserBvtTest.class,
        UserPermissionsTest.class,
        SpecimenTest.class,
        SampleSetTest.class,
        MS2BvtTest.class,
        MS1Bvt.class,
        PipelineBvtTest.class,
        CaBigTest.class,
        FileContentTest.class,
        DataRegionTest.class,
        ClientAPITest.class,
        WikiBvtTest.class,
        MessagesBvtTest.class,
        AuditLogTest.class,
        MicroarrayTest.class,
        HTTPApiTest.class,
        TimelineTest.class,
        FieldValidatorTest.class,
        DbUserSchemaTest.class,
        MissingValueIndicatorsTest.class,
        ModuleAssayTest.class,
        SimpleModuleTest.class,
        JavaClientApiTest.class,
        ProgrammaticQCTest.class
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

    Flow(new Class[] {
        FlowTest.class,
        FlowJoQueryTest.class,
        FlowImportTest.class,
    }),

    Study(new Class[] {
            StudyBvtTest.class,
            StudyExtraTest.class,
            AssayTest.class,
            StudyImportTest.class
    }),

    Data(new Class[] {
            DataRegionTest.class,
            DbUserSchemaTest.class,
            ListTest.class,
            IssuesTest.class,
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

    TestSet(TestSet set, Class... tests)
    {
        Class[] all = new Class[set.tests.length + tests.length];
        System.arraycopy(set.tests, 0, all, 0, set.tests.length);
        System.arraycopy(tests, 0, all, set.tests.length, tests.length);
        setTests(all);
    }

    TestSet(Class... tests)
    {
        setTests(tests);
    }

    void setTests(Class... tests)
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
