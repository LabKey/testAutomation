package org.labkey.test.tests;

import org.apache.commons.lang3.CharUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.TestFileUtils;
import org.labkey.test.util.TestLogger;

import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Category({})
@RunWith(Parameterized.class)
public class PackageLockJsonTest
{
    private static final Set<String> ALLOWED_DEPENDENCY_HOSTS = Set.of("registry.npmjs.org", "labkey.jfrog.io");

    private final List<String> errors = new ArrayList<>();
    private final File moduleDir;

    public PackageLockJsonTest(File moduleDir, String name)
    {
        this.moduleDir = moduleDir;
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data()
    {
        List<File> allModules = new ArrayList<>();

        File modulesDir = new File(TestFileUtils.getLabKeyRoot(), "server/modules");
        File[] files = modulesDir.listFiles();
        if (files == null)
        {
            throw new RuntimeException("No files found in modules directory: " + modulesDir.getAbsolutePath());
        }
        for (File file : files)
        {
            if (file.isDirectory())
            {
                if (new File(file, "module.properties").exists())
                {
                    allModules.add(file);
                }
                else
                {
                    allModules.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles(), () -> "No files found in " + file.getAbsolutePath()))
                        .filter(f -> f.isDirectory() && new File(f, "module.properties").isFile()).toList());
                }
            }
        }

        return allModules.stream().filter(file -> new File(file, "package-lock.json").exists())
            .map(f -> new Object[]{f, f.getName()}).toList();
    }

    @Test
    public void testPackageLock() throws Exception
    {
        File packageLock = new File(moduleDir, "package-lock.json");
        Assert.assertTrue("No package-lock.json found in module: " + moduleDir.getAbsolutePath(), packageLock.isFile());

        TestLogger.log("Testing module: " + moduleDir.getAbsolutePath());

        JSONObject packages;
        try (Reader reader = Readers.getReader(packageLock))
        {
            JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
            packages = jsonObject.optJSONObject("packages");
            if (packages == null)
                packages = jsonObject.getJSONObject("dependencies"); // old lockfile version
        }

        for (String packageName : packages.keySet())
        {
            if (!packageName.isBlank())
            {
                JSONObject packageJson = packages.getJSONObject(packageName);
                verifyPackage(packageName, packageJson, packageLock);
            }
        }

        Assert.assertTrue("Bad sources: " + errors, errors.isEmpty());
    }

    /// Verify that a package reference in a package-lock.json file only resolves to known hosts and has a valid version
    /// Also checks sub-dependencies
    private void verifyPackage(String packageName, JSONObject packageJson, File packageLockFile)
    {
        String resolved = packageJson.optString("resolved");
        if (resolved.isBlank())
        {
            TestLogger.debug("Resolved field is blank for package " + packageName + " in " + packageLockFile.getAbsolutePath());
        }
        else
        {
            try
            {
                URI resolvedURL = new URI(resolved);
                String host = resolvedURL.getHost();
                if (!ALLOWED_DEPENDENCY_HOSTS.contains(host))
                {
                    String message = "Package " + packageName + " resolved to unrecognized host [" + host + "] in " + packageLockFile.getAbsolutePath();
                    errors.add(message);
                    TestLogger.error(message);
                }
            }
            catch (URISyntaxException e)
            {
                String message = "Package " + packageName + " resolved to an invalid location [" + resolved + "] in " + packageLockFile.getAbsolutePath();
                errors.add(message);
                TestLogger.error(message);
            }
        }

        String version = packageJson.optString("version");
        if (version.isBlank() || !CharUtils.isAsciiNumeric(version.charAt(0)))
        {
            String message = "Package " + packageName + " has bad version [" + version + "] in " + packageLockFile.getAbsolutePath();
            errors.add(message);
            TestLogger.error(message);
        }

        JSONObject transitiveDeps = packageJson.optJSONObject("dependencies", new JSONObject());
        for (String tDep : transitiveDeps.keySet())
        {
            JSONObject packageJsonDep = transitiveDeps.optJSONObject(tDep);
            if (packageJsonDep != null)
            {
                verifyPackage(tDep, packageJsonDep, packageLockFile); // belt and suspenders
            }
            else
            {
                String tVer = transitiveDeps.optString(tDep);
                if (tVer == null || (tVer.contains(":") && !tVer.startsWith("npm:"))) // URL, file, or workspace dependency
                {
                    String message = "Package " + packageName + " has bad transitive dependency [" + tVer + "] in " + packageLockFile.getAbsolutePath();
                    errors.add(message);
                    TestLogger.error(message);
                }
            }
        }
    }
}
