/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.test.util;

import java.io.File;

// TODO: Make this more generic -- fewer hard-coded assay properties
public class AssayImportOptions
{
    public enum VisitResolverType
    {
        ParticipantVisit,
        ParticipantDate,
        ParticipantVisitDate,
        SpecimenID,
        SpecimenIDParticipantVisit,
        LookupText,
        LookupList
    }

    private String assayId;
    private String cutoff1;
    private String cutoff2;
    private String cutoff3;
    private String virusName;
    private String virusId;
    private String curveFitMethod;
    private File metadataFile;
    private File runFile;
    private String[] ptids;
    private String[] visits;
    private String[] initialDilutions;
    private String[] dilutionFactors;
    private String[] methods;
    private String[] sampleIds;
    private String[] dates;
    private VisitResolverType visitResolver;
    private boolean useDefaultResolver = false;
    private boolean resetDefaults = false;

    private AssayImportOptions(ImportOptionsBuilder builder)
    {
        this.assayId = builder.assayId;
        this.cutoff1 = builder.cutoff1;
        this.cutoff2 = builder.cutoff2;
        this.cutoff3 = builder.cutoff3;
        this.virusName = builder.virusName;
        this.virusId = builder.virusId;
        this.curveFitMethod = builder.curveFitMethod;
        this.metadataFile = builder.metadataFile;
        this.runFile = builder.runFile;
        this.ptids = builder.ptids;
        this.visits = builder.visits;
        this.initialDilutions = builder.initialDilutions;
        this.dilutionFactors = builder.dilutionFactors;
        this.methods = builder.methods;
        this.sampleIds = builder.sampleIds;
        this.dates = builder.dates;
        this.visitResolver = builder.visitResolver;
        this.useDefaultResolver = builder.useDefaultResolver;
        this.resetDefaults = builder.resetDefaults;
    }

    public String getAssayId()
    {
        return assayId;
    }

    public String getCutoff1()
    {
        return cutoff1;
    }

    public String getCutoff2()
    {
        return cutoff2;
    }

    public String getCutoff3()
    {
        return cutoff3;
    }

    public String getVirusName()
    {
        return virusName;
    }

    public String getVirusId()
    {
        return virusId;
    }

    public String getCurveFitMethod()
    {
        return curveFitMethod;
    }

    public File getMetadataFile()
    {
        return metadataFile;
    }

    public File getRunFile()
    {
        return runFile;
    }

    public String[] getPtids()
    {
        return ptids;
    }

    public String[] getVisits()
    {
        return visits;
    }

    public String[] getInitialDilutions()
    {
        return initialDilutions;
    }

    public String[] getDilutionFactors()
    {
        return dilutionFactors;
    }

    public String[] getMethods()
    {
        return methods;
    }

    public String[] getSampleIds()
    {
        return sampleIds;
    }

    public String[] getDates()
    {
        return dates;
    }

    public VisitResolverType getVisitResolver()
    {
        return visitResolver;
    }

    public boolean isUseDefaultResolver()
    {
        return useDefaultResolver;
    }

    public boolean isResetDefaults() { return resetDefaults; }

    // TODO: Create separate builders for each assay type -- this works for most assay types, but many fields aren't relevant
    public static class ImportOptionsBuilder
    {
        private String assayId;
        private String cutoff1;
        private String cutoff2;
        private String cutoff3;
        private String virusName;
        private String virusId;
        private String curveFitMethod;
        private File metadataFile;
        private File runFile;
        private String[] ptids = new String[0];
        private String[] visits = new String[0];
        private String[] initialDilutions = new String[0];
        private String[] dilutionFactors = new String[0];
        private String[] methods = new String[0];
        private String[] sampleIds = new String[0];
        private String[] dates = new String[0];
        private VisitResolverType visitResolver = VisitResolverType.ParticipantVisit;
        public boolean useDefaultResolver;
        public boolean resetDefaults;

        public ImportOptionsBuilder assayId(String assayId)
        {
            this.assayId = assayId;
            return this;
        }

        public ImportOptionsBuilder cutoff1(String cutoff1)
        {
            this.cutoff1 = cutoff1;
            return this;
        }

        public ImportOptionsBuilder cutoff2(String cutoff2)
        {
            this.cutoff2 = cutoff2;
            return this;
        }

        public ImportOptionsBuilder cutoff3(String cutoff3)
        {
            this.cutoff3 = cutoff3;
            return this;
        }

        public ImportOptionsBuilder virusName(String virusName)
        {
            this.virusName = virusName;
            return this;
        }

        public ImportOptionsBuilder virusId(String virusId)
        {
            this.virusId = virusId;
            return this;
        }

        public ImportOptionsBuilder curveFitMethod(String curveFitMethod)
        {
            this.curveFitMethod = curveFitMethod;
            return this;
        }

        public ImportOptionsBuilder metadataFile(File metadataFile)
        {
            this.metadataFile = metadataFile;
            return this;
        }

        public ImportOptionsBuilder runFile(File runFile)
        {
            this.runFile = runFile;
            return this;
        }

        public ImportOptionsBuilder ptids(String[] ptids)
        {
            this.ptids = ptids;
            return this;
        }

        public ImportOptionsBuilder visits(String[] visits)
        {
            this.visits = visits;
            return this;
        }

        public ImportOptionsBuilder initialDilutions(String[] initialDilutions)
        {
            this.initialDilutions = initialDilutions;
            return this;
        }

        public ImportOptionsBuilder dilutionFactors(String[] dilutionFactors)
        {
            this.dilutionFactors = dilutionFactors;
            return this;
        }

        public ImportOptionsBuilder methods(String[] methods)
        {
            this.methods = methods;
            return this;
        }

        public ImportOptionsBuilder sampleIds(String[] sampleIds)
        {
            this.sampleIds = sampleIds;
            return this;
        }

        public ImportOptionsBuilder dates(String[] dates)
        {
            this.dates = dates;
            return this;
        }

        public ImportOptionsBuilder visitResolver(VisitResolverType visitResolver)
        {
            this.visitResolver = visitResolver;
            return this;
        }

        public ImportOptionsBuilder useDefaultResolver(Boolean useDefaultResolver)
        {
            this.useDefaultResolver = Boolean.TRUE.equals(useDefaultResolver);
            return this;
        }

        public ImportOptionsBuilder resetDefaults(Boolean resetDefaults)
        {
            this.resetDefaults = Boolean.TRUE.equals(resetDefaults);
            return this;
        }

        public AssayImportOptions build()
        {
            return new AssayImportOptions(this);
        }
    }
}
