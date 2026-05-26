/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.remoteapi.query;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class InsertExternalSchemaCommand extends PostCommand<CommandResponse>
{
    private final Params _params;

    public InsertExternalSchemaCommand(Params params)
    {
        super("query", "insertExternalSchema");
        _params = params;
    }

    @Override
    protected HttpPost createRequest(URI uri)
    {
        HttpPost request = super.createRequest(uri);

        try
        {
            request.setEntity(_params.toFormEntity());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }

        return request;
    }

    @Override
    public CommandResponse execute(Connection connection, String folderPath) throws IOException, CommandException
    {
        CommandResponse response = super.execute(connection, folderPath);
        validateTables(connection, folderPath);
        return response;
    }

    private void validateTables(Connection connection, String folderPath) throws IOException, CommandException
    {
        GetQueriesCommand command = new GetQueriesCommand(_params.userSchemaName);
        GetQueriesResponse response = command.execute(connection, folderPath);
        try
        {
            Assert.assertEquals(String.format("Wrong tables published to %s schema '%s'.", _params.schemaType(), _params.userSchemaName),
                    new HashSet<>(_params.tables), new HashSet<>(response.getQueryNames()));
        }
        catch (AssertionError e)
        {
            throw new CommandException(e.getMessage());
        }
    }

    public static class Params
    {
        private String userSchemaName;
        private String sourceSchemaName;
        private List<String> tables = new ArrayList<>();
        private String dataSource = "labkeyDataSource";
        private boolean includeSystem = true;
        private final boolean editable = false;
        private boolean indexable = true;
        private final boolean fastCacheRefresh = true; // Enable fast refresh by default for tests
        private String metaData = "";

        public Params(String userSchemaName, String sourceSchemaName)
        {
            setUserSchemaName(userSchemaName);
            setSourceSchemaName(sourceSchemaName);
        }

        private UrlEncodedFormEntity toFormEntity() throws UnsupportedEncodingException
        {
            List<NameValuePair> form = new ArrayList<>();
            form.add(new BasicNameValuePair("userSchemaName", userSchemaName));
            form.add(new BasicNameValuePair("sourceSchemaName", sourceSchemaName));
            form.add(new BasicNameValuePair("tables", String.join(",", tables)));
            form.add(new BasicNameValuePair("schemaType", schemaType()));
            form.add(new BasicNameValuePair("dataSource", dataSource));
            if (includeSystem)
            {
                form.add(new BasicNameValuePair("includeSystem", "on"));
            }
            if (editable)
            {
                form.add(new BasicNameValuePair("editable", "on"));
            }
            if (indexable)
            {
                form.add(new BasicNameValuePair("indexable", "on"));
            }
            if (fastCacheRefresh)
            {
                form.add(new BasicNameValuePair("fastCacheRefresh", "on"));
            }
            form.add(new BasicNameValuePair("metadata", metaData));

            return new UrlEncodedFormEntity(form);
        }

        protected String schemaType()
        {
            return "external";
        }

        public Params setUserSchemaName(String userSchemaName)
        {
            this.userSchemaName = userSchemaName;
            return this;
        }

        public Params setSourceSchemaName(String sourceSchemaName)
        {
            this.sourceSchemaName = sourceSchemaName;
            return this;
        }

        public Params setTables(Collection<String> tables)
        {
            this.tables = new ArrayList<>(tables);
            return this;
        }

        public Params setDataSource(String dataSource)
        {
            this.dataSource = dataSource;
            return this;
        }

        public Params setIncludeSystem(boolean includeSystem)
        {
            this.includeSystem = includeSystem;
            return this;
        }

        public Params setIndexable(boolean indexable)
        {
            this.indexable = indexable;
            return this;
        }

        public Params setMetaData(String metaData)
        {
            this.metaData = metaData;
            return this;
        }
    }
}
