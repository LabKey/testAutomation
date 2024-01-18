package org.labkey.test.params.wiki;

import org.json.JSONObject;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.WikiHelper;

import java.io.File;

public class SaveWikiParams
{
    private final String _name;
    private final String _body;
    private String _title = null;
    private WikiHelper.WikiRendererType _format = WikiHelper.WikiRendererType.TEXT_WITH_LINKS;
    private boolean _shouldIndex = true;
    private boolean _showAttachments = true;

    // TODO: Will need to get entityId and pageVersionId to update an existing wiki
    // Only use for creating new wikis (for now)
    private final int _pageVersionId = -1;
    private final String _entityId = null;

    public SaveWikiParams(String name, String body)
    {
        _name = name;
        _body = body;
    }

    public SaveWikiParams(String name)
    {
        this(name, "");
    }

    public SaveWikiParams(String name, File bodySource)
    {
        this(name, TestFileUtils.getFileContents(bodySource));
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();

        json.put("name", _name);
        if (_title != null)
        {
            json.put("title", _title);
        }
        json.put("body", _body);
        json.put("rendererType", _format.name());
        json.put("shouldIndex", _shouldIndex);
        json.put("showAttachments", _showAttachments);
        json.put("pageVersionId", _pageVersionId);
        json.put("entityId", _entityId);

        return json;
    }

    public String getName()
    {
        return _name;
    }

    public String getBody()
    {
        return _body;
    }

    public String getTitle()
    {
        return _title;
    }

    public SaveWikiParams setTitle(String title)
    {
        _title = title;
        return this;
    }

    public WikiHelper.WikiRendererType getFormat()
    {
        return _format;
    }

    public SaveWikiParams setFormat(WikiHelper.WikiRendererType format)
    {
        _format = format;
        return this;
    }

    public boolean isShouldIndex()
    {
        return _shouldIndex;
    }

    public SaveWikiParams setShouldIndex(boolean shouldIndex)
    {
        _shouldIndex = shouldIndex;
        return this;
    }

    public boolean isShowAttachments()
    {
        return _showAttachments;
    }

    public SaveWikiParams setShowAttachments(boolean showAttachments)
    {
        _showAttachments = showAttachments;
        return this;
    }
}
