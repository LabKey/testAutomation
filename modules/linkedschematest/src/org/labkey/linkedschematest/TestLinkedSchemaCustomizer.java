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
package org.labkey.linkedschematest;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlObject;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.LinkedSchemaCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.data.xml.SchemaCustomizerType;
import org.labkey.data.xml.queryCustomView.FilterType;
import org.labkey.data.xml.queryCustomView.LocalOrRefFiltersType;
import org.labkey.data.xml.queryCustomView.NamedFiltersType;
import org.labkey.data.xml.queryCustomView.OperatorType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * User: kevink
 * Date: 4/8/14
 */
public class TestLinkedSchemaCustomizer implements LinkedSchemaCustomizer
{
    private static Logger LOG = Logger.getLogger(TestLinkedSchemaCustomizer.class);

    // Name of the parameter declaration
    private static final String PARAM_FAMILY = "PARAM_FAMILY";

    private String _family = null;

    public TestLinkedSchemaCustomizer() {  }

    @Override
    public void configure(SchemaCustomizerType schemaCustomizer)
    {
        LOG.debug("configure");
        XmlObject[] families = schemaCustomizer.selectChildren(null, "family");
        if (families != null && families.length > 0)
        {
            SimpleValue simpleValue = (SimpleValue) families[0].changeType(SimpleValue.type);
            _family = simpleValue.getStringValue();
        }
    }

    @Override
    public void afterConstruct(UserSchema schema)
    {
        LOG.debug("afterConstruct");
    }

    @Override
    public void afterConstruct(UserSchema schema, TableInfo table)
    {
        LOG.debug("afterConstruct table");
    }

    @Override
    public void afterConstruct(UserSchema schema, QueryDefinition def)
    {
        LOG.debug("afterConstruct query");
    }

    @Override
    public void customizeNamedFilters(Map<String, NamedFiltersType> filters)
    {
        if (_family != null)
        {
            NamedFiltersType familyFilter = NamedFiltersType.Factory.newInstance();
            familyFilter.setName("FamilyFilter");
            FilterType filterType = familyFilter.addNewFilter();
            filterType.setColumn("Family");
            filterType.setOperator(OperatorType.EQ);
            filterType.setValue(_family);
            filters.put("FamilyFilter", familyFilter);

            NamedFiltersType SubjectIdFamilyFilter = NamedFiltersType.Factory.newInstance();
            SubjectIdFamilyFilter.setName("SubjectIdFamilyFilter");
            SubjectIdFamilyFilter.addWhere("SubjectId IN (SELECT NIMHDemographics.SubjectId FROM NIMHDemographics WHERE Family = " + PARAM_FAMILY + ")");
            filters.put("SubjectIdFamilyFilter", SubjectIdFamilyFilter);
        }
    }

    @Override
    public LocalOrRefFiltersType customizeFilters(String name, TableInfo table, LocalOrRefFiltersType xmlFilters)
    {
        LOG.debug("customizeFilters");
        if (table.getColumn("Family") != null)
        {
            LocalOrRefFiltersType newFilters = LocalOrRefFiltersType.Factory.newInstance();
            newFilters.setRef("FamilyFilter");
            return newFilters;
        }
        else if (table.getColumn("SubjectId") != null)
        {
            LocalOrRefFiltersType newFilters = LocalOrRefFiltersType.Factory.newInstance();
            newFilters.setRef("SubjectIdFamilyFilter");
            return newFilters;
        }

        return xmlFilters;
    }

    @Override
    public Collection<QueryService.ParameterDecl> customizeParameters(String name, TableInfo table, @Nullable LocalOrRefFiltersType xmlFilters)
    {
        LOG.debug("customizeParameters");
        if (table.getColumn("Family") != null || table.getColumn("Subjectid") != null)
            return Collections.singletonList(new QueryService.ParameterDeclaration(PARAM_FAMILY, JdbcType.VARCHAR));

        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> customizeParamValues(TableInfo table)
    {
        LOG.debug("customizeParamValues");
        if (table.getColumn("Family") != null || table.getColumn("SubjectId") != null)
            return Collections.singletonMap(PARAM_FAMILY, _family);

        return Collections.emptyMap();
    }
}
