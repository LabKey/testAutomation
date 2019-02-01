package org.labkey.test.util.core.webdav;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebDavUploadHelper
{
    private final WebDavUrlFactory _urlFactory;
    private final Sardine _sardine;
    private int _maxDepth = -1;
    private FileFilter _fileFilter = null;

    public WebDavUploadHelper(@NotNull WebDavUrlFactory urlFactory, @NotNull Sardine sardine)
    {
        _urlFactory = urlFactory;
        _sardine = sardine;
    }

    public WebDavUploadHelper(@NotNull WebDavUrlFactory urlFactory)
    {
        this(urlFactory, SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword()));
    }

    public WebDavUploadHelper(@NotNull String containerPath)
    {
        this(WebDavUrlFactory.webDavUrlFactory(containerPath), SardineFactory.begin(PasswordUtil.getUsername(), PasswordUtil.getPassword()));
    }

    /**
     * @param maxDepth max directory depth when uploading directories
     */
    public void setMaxDepth(int maxDepth)
    {
        _maxDepth = maxDepth;
    }

    /**
     * Sets a custom file filter for uploading directories. Defaults to accepting all files.
     *
     * This only applies to {@link #uploadDirectory(File)} and {@link #uploadDirectoryContents(File)}. Individual file
     * uploads will ignore this filter.
     *
     * @param fileFilter A filter to control which files should be uploaded or 'null' to accept all files
     */
    public void setFileFilter(FileFilter fileFilter)
    {
        _fileFilter = fileFilter;
    }

    public List<File> uploadDirectoryContents(@NotNull File directory)
    {
        return uploadDirectoryContents("", directory);
    }

    public List<File> uploadDirectory(@NotNull File directory)
    {
        return uploadDirectoryContents(directory.getName(), directory);
    }

    private List<File> uploadDirectoryContents(String uploadPrefix, File directory)
    {
        Map<File, String> filesToUpload = getFilesToUpload(uploadPrefix, directory, 0);
        uploadFiles(filesToUpload);
        return new ArrayList<>(filesToUpload.keySet());
    }

    private Map<File, String> getFilesToUpload(String uploadPrefix, File directory, int depth)
    {
        if (depth > _maxDepth || _maxDepth < 0)
            return Collections.emptyMap();

        if (!directory.isDirectory())
            throw new IllegalArgumentException("Not a directory. Use WebDaveHelper.uploadFile to upload a file.");

        Map<File, String> filesToUpload = new HashMap<>();

        File[] fileList = directory.listFiles(_fileFilter);

        for (File file : fileList)
        {
            if (file.isDirectory())
                filesToUpload.putAll(getFilesToUpload(uploadPrefix + "/" + file.getName(), file, depth + 1));
            else
                filesToUpload.put(file, uploadPrefix);
        }

        return filesToUpload;
    }

    public void uploadFiles(@NotNull Map<File, String> filesByPrefix)
    {
        for (File file : filesByPrefix.keySet())
        {
            uploadFile(file, filesByPrefix.get(file));
        }
    }

    public void uploadFile(@NotNull File file)
    {
        uploadFile(file, "");
    }

    public void uploadFile(@NotNull File file, @NotNull String destPrefix)
    {
        if (file.isDirectory())
            throw new IllegalArgumentException(file.getAbsolutePath() + " is a directory. Use uploadDirectory or uploadDirectoryContents");
        String putUrl = _urlFactory.getPath(destPrefix, file.getName());
        String message = file.getAbsolutePath() + " => " + putUrl;
        TestLogger.log("Uploading: " + message);
        try
        {
            _sardine.put(putUrl, file, null);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to upload file: " + message, e);
        }
    }
}
