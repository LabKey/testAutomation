package org.labkey.test.tests.core.admin;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.categories.Git;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.core.admin.AllowedFileExtensionAdminPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Category({Git.class})
public class AllowedFileExtensionsPageTest extends AllowedFileExtensionBaseTest
{
    @BeforeClass
    public static void setupProject()
    {
        AllowedFileExtensionsPageTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        goToProjectHome();
    }

    @Before
    public void beforeTest() throws IOException, CommandException
    {
        log("Use API to delete any existing allowed extensions.");
        AllowedFileExtensionAdminPage.deleteAllAllowedFileExtension(createDefaultConnection());
    }

    /**
     * <p>
     *     Test the 'Allowed File Extension Admin'.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Add several file extensions as allowed extensions, and validate they are listed in alphabetical order.</li>
     *         <li>Click 'Delete All' and cancel out of confirmation.</li>
     *         <li>Validate extension value must start with a '.'</li>
     *         <li>Validate duplicate extensions are not allowed.</li>
     *         <li>Validate blank extension type is not allowed.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testAllowFileExtensionsAdminPage()
    {

        List<String> allowedTypes = new ArrayList<>(fileMap.keySet());

        log(String.format("Add the following as allowed extensions: %s", allowedTypes));
        AllowedFileExtensionAdminPage allowedFileExtensionAdminPage = setAllowedExtensions(allowedTypes, allowedTypes);

        log("Validate that the allowed file extensions are listed in alphabetical order.");
        List<String> actualValues = allowedFileExtensionAdminPage.getAllowedExtensions().stream().map(Input::getValue).toList();
        Collections.sort(allowedTypes);

        checker().withScreenshot()
                .verifyEquals("List of 'Allowed extensions' is not in the expected order.",
                        allowedTypes, actualValues);

        log("Click 'Delete All' but cancel out of the confirmation dialog.");

        allowedFileExtensionAdminPage.deleteAllExtensions(false);

        List<Input> extensions = allowedFileExtensionAdminPage.getAllowedExtensions();

        checker().withScreenshot()
                .verifyEqualsSorted("List of 'Allowed extensions' is not as expected after canceling 'Delete All'.",
                        allowedTypes, extensions.stream().map(Input::getValue).toList());

        // Issue 38785
        // Selenium launches the firefox browser with a preference set that (basically) disabled the dirty bit.
        // As a result, checking for the dirty bit dialog prompt, or a missing dialog, is not doable until issue 38785
        // is addressed.
        // Issue 53039.
//        log("Validate that canceling the 'Delete All' dialog does not set the dirty bit.");
//
//        // Using goToProjectHome will validate no dirty bit is set and navigation can happen.
//        goToProjectHome();
//
//        allowedFileExtensionAdminPage = goToAdminConsole().clickAllowedFileExtensions();

        // Issue 38785
        // Selenium launches the firefox browser with a prefernce set that (basically) disabled the dirty bit.
        // As a result, checking for the dirty bit dialog prompt, or a missing dialog, is doable until 38785 is addressed.
//        String oldExtension = ".txt";
//        String newExtension = ".pdf";
//
//        log(String.format("Edit the extension '%s' and change it to '%s'.", oldExtension, newExtension));
//
//        Input editExtension = allowedFileExtensionAdminPage.getAllowedExtension(allowedFileExtensionAdminPage.getAllowedExtensionIndex(oldExtension));
//
//        editExtension.setValue(newExtension);
//
//        // Issue 53039 validate dirty bit warning.
//        log("Validate that an alert is shown if the change is not saved.");
//        Locator.tagWithClass("a", "brand-logo").findElement(getDriver()).click();
//        Alert alert = waitForAlert();
//
//        checker().withScreenshot()
//                .verifyTrue("Alert text doesn't have expected text.",
//                        alert.getText().contains("Changes you made may not be saved."));
//
//        log("Dismiss the alert.");
//        alert.dismiss();
//
//        log("Save the change.");
//        allowedFileExtensionAdminPage.clickSaveUpdateExtension();

        allowedFileExtensionAdminPage.setExtension("not .an extension");
        String expectedValue = "File extension must start with a '.'";
        String actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for invalid extension.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension(allowedTypes.getFirst());
        expectedValue = String.format("'%s' already exists. Duplicate values not allowed.", allowedTypes.getFirst());
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for duplicate extension.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension(allowedTypes.get(1).toUpperCase());
        expectedValue = String.format("'%s' already exists. Duplicate values not allowed.", allowedTypes.get(1).toUpperCase());
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for duplicate extension but different case.",
                        expectedValue, actualValue);

        allowedFileExtensionAdminPage.setExtension("");
        expectedValue = "File extension must not be blank.";
        actualValue = allowedFileExtensionAdminPage.clickSaveExpectingError();
        checker().withScreenshot()
                .verifyEquals("Incorrect error message for blank extension value.",
                        expectedValue, actualValue);

    }

    @Override
    protected String getProjectName()
    {
        return "Allowed File Extension Page Test Project " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

}
