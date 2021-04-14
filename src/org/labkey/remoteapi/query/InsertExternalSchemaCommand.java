package org.labkey.remoteapi.query;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class InsertExternalSchemaCommand extends PostCommand<CommandResponse>
{
    /*
    schemaType: external
userSchemaName: fdahpuserregws
dataSource: labkeyDataSource
sourceSchemaName: fdahpuserregws
includeSystem: on
@includeSystem:
@editable:
indexable: on
@indexable:
@fastCacheRefresh:
metaData:
tables: List.of("apppropertiesdetails", "authinfo", "loginattempts", "passwordhistory", "userappdetails", "userdetails");
X-LABKEY-CSRF: 1f4f5eb0adc721ba44c3e44ee58c3e59
X-LABKEY-CSRF: 1f4f5eb0adc721ba44c3e44ee58c3e59
     */
    private final Params _params;

    public InsertExternalSchemaCommand(Params params)
    {
        super("query", "insertExternalSchema");
        _params = params;
    }

    @Override
    protected HttpUriRequest createRequest(URI uri)
    {
        HttpPost request = (HttpPost) super.createRequest(uri);

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

    public static class Params
    {
        private String userSchemaName;
        private String sourceSchemaName;
        private String tables = "";
        private String schemaType = "external";
        private String dataSource = "labkeyDataSource";
        private boolean includeSystem = true;
        private boolean editable = false;
        private boolean indexable = true;
        private boolean fastCacheRefresh = true; // Enable fast refresh by default for tests
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
            form.add(new BasicNameValuePair("tables", tables));
            form.add(new BasicNameValuePair("schemaType", schemaType));
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

        public Params setTables(List<String> tables)
        {
            this.tables = String.join(",", tables);
            return this;
        }

        public Params setSchemaType(String schemaType)
        {
            this.schemaType = schemaType;
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
