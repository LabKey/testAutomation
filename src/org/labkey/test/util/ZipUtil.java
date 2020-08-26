/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
import org.labkey.test.TestFileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil
{
    private final File _source;

    public ZipUtil(File source)
    {
        _source = source;
    }

    public File tempZip() throws IOException
    {
        TestFileUtils.getTestTempDir();
        return zipInto(TestFileUtils.getTestTempDir());
    }

    public File zipInto(File destDir) throws IOException
    {
        return zipIt(new File(destDir, _source.getName() + ".zip"));
    }

    public File zipIt(File destZip) throws IOException
    {
        List<File> fileList = generateFileList(_source, new ArrayList<>());

        TestLogger.log("Output to Zip : " + destZip.toString());
        Files.createDirectories(destZip.getParentFile().toPath());
        if (destZip.exists())
        {
            Files.delete(destZip.toPath());
        }
        Files.createFile(destZip.toPath());

        byte[] buffer = new byte[1024];

        try(
                FileOutputStream fos = new FileOutputStream(destZip);
                ZipOutputStream zos = new ZipOutputStream(fos)
        )
        {
            for (File file : fileList)
            {
                ZipEntry ze = generateZipEntry(file);
                zos.putNextEntry(ze);
                try (FileInputStream in = new FileInputStream(file))
                {

                    int len;
                    while ((len = in.read(buffer)) > 0)
                    {
                        zos.write(buffer, 0, len);
                    }
                }
            }

            zos.closeEntry();

            TestLogger.log(String.format("Zipped %d files", fileList.size()));
        }

        return destZip;
    }

    private List<File> generateFileList(File node, List<File> fileList)
    {
        // add file only
        if (node.isFile())
        {
            fileList.add(node);
        }

        if (node.isDirectory())
        {
            String[] subNodes = node.list();
            for (String filename : subNodes)
            {
                generateFileList(new File(node, filename), fileList);
            }
        }

        return fileList;
    }

    private ZipEntry generateZipEntry(File file)
    {
        if (_source.equals(file)) // zipping a single file
            return new ZipEntry(file.getName());
        else
            return new ZipEntry(_source.toURI().relativize(file.toURI()).getPath());
    }
}