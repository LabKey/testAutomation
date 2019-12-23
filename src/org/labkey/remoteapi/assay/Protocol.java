package org.labkey.remoteapi.assay;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.ResponseObject;
import org.labkey.remoteapi.domain.Domain;

import java.util.ArrayList;
import java.util.List;

public class Protocol extends ResponseObject
{
    private Long _protocolId;
    private String _name;
    private String _description;
    private String _providerName;
    private List<Domain> _domains = new ArrayList<>();

    private Boolean _allowBackgroundUpload;

    private Boolean _allowSpacesInPath;
    private Boolean _allowTransformationScript;
    private Boolean _backgroundUpload;

    private Boolean _allowEditableResults;
    private Boolean _editableResults;
    private Boolean _editableRuns;
    private Boolean _saveScriptFiles;

    private Boolean _allowQCStates;
    private Boolean _qcEnabled;

    public Protocol()
    {
        super(null);
    }

    public Protocol(JSONObject json)
    {
        super(json);

        _protocolId = (Long)json.get("protocolId");
        _name = (String)json.get("name");
        _description = (String)json.get("description");
        _providerName = (String)json.get("providerName");

        if (json.get("domains") instanceof JSONArray)
        {
            for (Object domain : ((JSONArray)json.get("domains")))
                _domains.add(new Domain((JSONObject)domain));
        }

        if (json.containsKey("allowBackgroundUpload"))
            _allowBackgroundUpload = (Boolean)json.get("allowBackgroundUpload");
        if (json.containsKey("allowEditableResults"))
            _allowEditableResults = (Boolean)json.get("allowEditableResults");
        if (json.containsKey("editableResults"))
            _editableResults = (Boolean)json.get("editableResults");
        if (json.containsKey("editableRuns"))
            _editableRuns = (Boolean)json.get("editableRuns");
        if (json.containsKey("saveScriptFiles"))
            _saveScriptFiles = (Boolean)json.get("saveScriptFiles");
        if (json.containsKey("qcEnabled"))
            _qcEnabled = (Boolean)json.get("qcEnabled");
        if (json.containsKey("allowQCStates"))
            _allowQCStates = (Boolean)json.get("allowQCStates");
        if (json.containsKey("allowSpacesInPath"))
            _allowSpacesInPath = (Boolean)json.get("allowSpacesInPath");
        if (json.containsKey("allowTransformationScript"))
            _allowTransformationScript = (Boolean)json.get("allowTransformationScript");

    }

    public JSONObject toJSONObject()
    {
        JSONObject result = new JSONObject();
        result.put("protocolId", _protocolId);
        result.put("name", _name);
        result.put("description", _description);
        result.put("providerName", _providerName);
        JSONArray domains = new JSONArray();
        result.put("domains", domains);
        for (Domain domain : _domains)
            domains.add(domain.toJSONObject(true));

        if (_allowBackgroundUpload != null)
            result.put("allowBackgroundUpload", _allowBackgroundUpload);
        if (_backgroundUpload != null)
            result.put("backgroundUpload", _backgroundUpload);
        if (_allowEditableResults != null)
            result.put("allowEditableResults", _allowEditableResults);
        if (_editableResults != null)
            result.put("editableResults", _editableResults);
        if (_allowQCStates != null)
            result.put("allowQCStates", _allowQCStates);
        if (_qcEnabled != null)
            result.put("qcEnabled", _qcEnabled);
        if (_allowSpacesInPath != null)
            result.put("allowSpacesInPath", _allowSpacesInPath);
        if (_allowTransformationScript != null)
            result.put("allowTransformationScript", _allowTransformationScript);
        if (_saveScriptFiles != null)
            result.put("saveScriptFiles", _saveScriptFiles);

        return result;
    }

    public Long getProtocolId()
    {
        return _protocolId;
    }

    public String getName()
    {
        return _name;
    }

    public Protocol setName(String name)
    {
        _name = name;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }

    public String getProviderName()
    {
        return _providerName;
    }

    public void setProviderName(String providerName)
    {
        _providerName = providerName;
    }

    public List<Domain> getDomains()
    {
        return _domains;
    }

    public void setDomains(List<Domain> domains)
    {
        _domains = domains;
    }

    public Protocol setAllowBackgroundUpload(boolean allowBackgroundUpload)
    {
        _allowBackgroundUpload = allowBackgroundUpload;
        return this;
    }
    public Boolean getAllowBackgroundUpload()
    {
        return _allowBackgroundUpload;
    }

    public Protocol setAllowEditableResults(boolean allowEditableResults)
    {
        _allowEditableResults = allowEditableResults;
        return this;
    }
    public Boolean getAllowEditableResults()
    {
        return _allowEditableResults;
    }

    public Protocol allowQCStates(boolean qcEnabled)
    {
        _allowQCStates = qcEnabled;
        return this;
    }
    public Boolean getAllowQCStates()
    {
        return _allowQCStates;
    }

    public Protocol setAllowSpacesInPath(Boolean allowSpacesInPath)
    {
        _allowSpacesInPath = allowSpacesInPath;
        return this;
    }
    public boolean getAllowSpacesInPath()
    {
        return _allowSpacesInPath;
    }

    public Protocol setAllowTransformationScript(Boolean allowTransformationScript)
    {
        _allowTransformationScript = allowTransformationScript;
        return this;
    }
    public Boolean getAllowTransformationScript()
    {
        return _allowTransformationScript;
    }

    public Protocol setBackgroundUpload(Boolean backgroundUpload)
    {
        _backgroundUpload = backgroundUpload;
        return this;
    }
    public boolean getBackgroundUpload()
    {
        return _backgroundUpload;
    }

    public Protocol setEditableResults(Boolean editableResults)
    {
        _editableResults = editableResults;
        return this;
    }
    public Boolean getEditableResults()
    {
        return _editableResults;
    }

    public Protocol setEditableRuns(Boolean editableRuns)
    {
        _editableRuns = editableRuns;
        return this;
    }
    public Boolean getEditableRuns()
    {
        return _editableRuns;
    }

    public Protocol setSaveScriptFiles(Boolean saveScriptFiles)
    {
        _saveScriptFiles = saveScriptFiles;
        return this;
    }
    public Boolean getSaveScriptFiles()
    {
        return _saveScriptFiles;
    }

    public Protocol setQCEnabled(boolean qcEnabled)
    {
        _qcEnabled = qcEnabled;
        return this;
    }
    public Boolean getQcEnabled()
    {
        return _qcEnabled;
    }
}
