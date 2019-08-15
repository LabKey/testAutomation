/*
 * Copyright (c) 2019 LabKey Corporation
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
package org.labkey.serverapi.reader;

import org.apache.commons.io.IOCase;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileType implements Serializable
{
    private static final Detector DETECTOR = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

    /**
     * handle TPP's native use of .xml.gz
     **/
    public enum gzSupportLevel
    {
        NO_GZ,      // we don't support gzip for this filetype
        SUPPORT_GZ, // we support gzip for this filetype, but it's not the norm
        PREFER_GZ   // we support gzip for this filetype, and it's the default for new files
    }

    /**
     * A list of possible suffixes in priority order. Later suffixes may also match earlier suffixes
     */
    private List<String> _suffixes;
    /**
     * a list of filetypes to reject - handles the scenario where old pepxml files are "foo.xml" and
     * we have to avoid grabbing "foo.pep-prot.xml"
     */
    private List<FileType> _antiTypes;
    /**
     * The canonical suffix, will be used when creating new files from scratch
     */
    private String _defaultSuffix;

    /**
     * Mime content type.
     */
    private List<String> _contentTypes;

    private Boolean _dir;
    /**
     * If _preferGZ is true, assume suffix.gz for new files to support TPP's transparent .xml.gz useage.
     * When dealing with existing files, non-gz version is still assumed to be the target if found
     **/
    private Boolean _preferGZ;
    /**
     * If _supportGZ is true, accept .suffix.gz as the equivalent of .suffix
     **/
    private Boolean _supportGZ;
    private boolean _caseSensitiveOnCaseSensitiveFileSystems = false;

    /**
     * @param suffixes      list of what are usually the file extensions (but may be some other suffix to
     *                      uniquely identify a file type), in priority order. The first suffix that matches a file will be used
     *                      and files that match the rest of the suffixes will be ignored
     * @param defaultSuffix the canonical suffix, will be used when creating new files from scratch
     * @param contentTypes  Content types for this file type.  If null, a content type will be guessed based on the extension.
     */
    public FileType(List<String> suffixes, String defaultSuffix, List<String> contentTypes)
    {
        this(suffixes, defaultSuffix, false, gzSupportLevel.NO_GZ, contentTypes);
    }

    /**
     * @param suffixes      list of what are usually the file extensions (but may be some other suffix to
     *                      uniquely identify a file type), in priority order. The first suffix that matches a file will be used
     *                      and files that match the rest of the suffixes will be ignored
     * @param defaultSuffix the canonical suffix, will be used when creating new files from scratch
     * @param doSupportGZ   for handling TPP's transparent use of .xml.gz
     * @param contentTypes  Content types for this file type.  If null, a content type will be guessed based on the extension.
     */
    public FileType(List<String> suffixes, String defaultSuffix, boolean dir, gzSupportLevel doSupportGZ, List<String> contentTypes)
    {
        _suffixes = suffixes;
        supportGZ(doSupportGZ);
        _defaultSuffix = defaultSuffix;
        _dir = Boolean.valueOf(dir);
        _antiTypes = new ArrayList<>(0);
        if (!suffixes.contains(defaultSuffix))
        {
            throw new IllegalArgumentException("List of suffixes " + _suffixes + " does not contain the preferred suffix:" + _defaultSuffix);
        }

        if (contentTypes == null)
        {
            MimeMap mm = new MimeMap();
            String contentType = mm.getContentType(defaultSuffix);
            if (contentType != null)
                _contentTypes = Collections.singletonList(contentType);
            else
                _contentTypes = Collections.emptyList();
        }
        else
        {
            _contentTypes = Collections.unmodifiableList(new ArrayList<>(contentTypes));
        }
    }

    /**
     * helper for supporting TPP's use of .xml.gz
     */
    private String tryName(File parentDir, String name)
    {
        if (_supportGZ.booleanValue())  // TPP treats xml.gz as a native format
        {   // in the case of existing files, non-gz copy wins if present
            File f = parentDir != null ? new File(parentDir, name) : new File(name);
        }
        return name;
    }

    /**
     * turn support for gzipped files on and off
     */
    public boolean supportGZ(gzSupportLevel doSupportGZ)
    {
        _supportGZ = Boolean.valueOf(doSupportGZ != gzSupportLevel.NO_GZ);
        _preferGZ = Boolean.valueOf(doSupportGZ == gzSupportLevel.PREFER_GZ);
        return _supportGZ.booleanValue();
    }

    // used to avoid, for example, mistaking protxml ".pep-prot.xml" for pepxml ".xml" file
    private boolean isAntiFileType(String name, byte[] header)
    {
        for (FileType a : _antiTypes)
        {
            if (a.isType(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for a file in the parentDir that matches, in priority order. If one is found, returns its file name.
     * If nothing matches, uses the defaultSuffix to build a file name.
     */
    public String getName(File parentDir, String basename)
    {
        if (_suffixes.size() > 1)
        {
            // Only bother checking if we have more than one possible suffix
            for (String suffix : _suffixes)
            {
                String name = tryName(parentDir, basename + suffix);
                File f = new File(parentDir, name);
            }
        }
        return tryName(parentDir, basename + _defaultSuffix);
    }

    public String getName(String parentDirName, String basename)
    {
        File parentDir = new File(parentDirName);
        return getName(parentDir, basename);
    }

    private String toLowerIfCaseInsensitive(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (_caseSensitiveOnCaseSensitiveFileSystems && IOCase.SYSTEM.isCaseSensitive())
        {
            return s;
        }
        return s.toLowerCase();
    }

    /**
     * Checks if the path matches any of the suffixes
     */
    public boolean isType(String filePath)
    {
        return isType(filePath, null, null);
    }

    /**
     * Checks if the path matches any of the suffixes and the file header if provided.
     */
    public boolean isType(@Nullable String filePath, @Nullable String contentType, @Nullable byte[] header)
    {
        // avoid, for example, mistaking protxml ".pep-prot.xml" for pepxml ".xml"
        if (isAntiFileType(filePath, header))
        {
            return false;
        }

        // Attempt to match by content type.
        if (_contentTypes != null)
        {
            // Use Tika to determine the content type
            if (contentType == null && header != null)
                contentType = detectContentType(filePath, header);

            if (contentType != null)
            {
                contentType = contentType.toLowerCase().trim();
                if (_contentTypes.contains(contentType))
                    return true;
            }
        }

        // Attempt to match by suffix and header.
        if (filePath != null)
        {
            filePath = toLowerIfCaseInsensitive(filePath);
            for (String suffix : _suffixes)
            {
                suffix = toLowerIfCaseInsensitive(suffix);
                if (filePath.endsWith(suffix))
                {
                    if (header == null || isHeaderMatch(header))
                        return true;
                }
                // TPP treats .xml.gz as a native format
                if (_supportGZ.booleanValue() && filePath.endsWith(suffix + ".gz"))
                {
                    if (header == null || isHeaderMatch(header))
                        return true;
                }
            }
        }

        // Attempt to match using only the header.
        if (header != null && isHeaderMatch(header))
            return true;

        return false;
    }

    protected static String detectContentType(String fileName, byte[] header)
    {
        final Metadata metadata = new Metadata();
        metadata.set("resourceName", fileName);
        try (TikaInputStream is = TikaInputStream.get(header, metadata))
        {
            MediaType mediaType = DETECTOR.detect(is, metadata);
            if (mediaType != null)
                return mediaType.toString();

            return null;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the file header matches. This is useful for FileTypes that share an
     * extension, e.g. "txt" or "xml", or when the filename or extension isn't available.
     *
     * @param header First few K of the file.
     * @return True if the header matches, false otherwise.
     */
    public boolean isHeaderMatch(@NotNull byte[] header)
    {
        return false;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileType fileType = (FileType) o;

        if (_supportGZ != null ? !_supportGZ.equals(fileType._supportGZ) : fileType._supportGZ != null) return false;
        if (_preferGZ != null ? !_preferGZ.equals(fileType._preferGZ) : fileType._preferGZ != null) return false;
        if (_dir != null ? !_dir.equals(fileType._dir) : fileType._dir != null) return false;
        if (_defaultSuffix != null ? !_defaultSuffix.equals(fileType._defaultSuffix) : fileType._defaultSuffix != null)
            return false;
        if (_antiTypes != null ? !_antiTypes.equals(fileType._antiTypes) : fileType._antiTypes != null) return false;
        return !(_suffixes != null ? !_suffixes.equals(fileType._suffixes) : fileType._suffixes != null);
    }

    public int hashCode()
    {
        int result;
        result = (_suffixes != null ? _suffixes.hashCode() : 0);
        result = 31 * result + (_defaultSuffix != null ? _defaultSuffix.hashCode() : 0);
        result = 31 * result + (_dir != null ? _dir.hashCode() : 0);
        result = 31 * result + (_supportGZ != null ? _supportGZ.hashCode() : 0);
        result = 31 * result + (_preferGZ != null ? _preferGZ.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        return (_dir == null || !_dir.booleanValue() ? _suffixes.toString() : _suffixes + "/");
    }

}

