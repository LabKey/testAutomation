package org.labkey.remoteapi.assay;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;

import java.util.HashMap;
import java.util.Map;

public class GetProtocolCommand extends Command<ProtocolResponse>
{
    private String _providerName;
    private Long _protocolId;
    private Boolean _copy;

    public GetProtocolCommand(String providerName)
    {
        super("assay", "getProtocol");
        _providerName = providerName;
    }

    public GetProtocolCommand(Long protocolId)
    {
        super("assay", "getProtocol");
        _protocolId = protocolId;
    }

    public GetProtocolCommand(Long protocolId, boolean copy)
    {
        super("assay", "getProtocol");
        _protocolId = protocolId;
        _copy = copy;
    }

    @Override
    public Map<String, Object> getParameters()
    {
        Map<String,Object> params = new HashMap<>();
        if (_protocolId != null) {
            params.put("protocolId", _protocolId);
            params.put("copy", _copy);
        }
        else if (_providerName != null)
        {
            params.put("providerName", _providerName);
        }
        return params;
    }

    @Override
    protected ProtocolResponse createResponse(String text, int status, String contentType, JSONObject json)
    {
        return new ProtocolResponse(text, status, contentType, json, this);
    }

    public String getProviderName()
    {
        return _providerName;
    }

    public void setProviderName(String providerName)
    {
        _providerName = providerName;
    }

    public Long getProtocolId()
    {
        return _protocolId;
    }

    public void setProtocolId(Long protocolId)
    {
        _protocolId = protocolId;
    }

    public Boolean getCopy()
    {
        return _copy;
    }

    public void setCopy(Boolean copy)
    {
        _copy = copy;
    }
}
