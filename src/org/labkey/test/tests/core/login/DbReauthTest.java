package org.labkey.test.tests.core.login;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.tests.AbstractReauthTest;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.TestUser;

@Category({Daily.class})
public class DbReauthTest extends AbstractReauthTest
{
    private static final TestUser USER1 = new TestUser("db_user1@reauth.test");
    private static final TestUser USER2 = new TestUser("db_user2@reauth.test");

    public DbReauthTest()
    {
        super(new User(USER1.getEmail(), PasswordUtil.getPassword()), new User(USER2.getEmail(), PasswordUtil.getPassword()));
    }

    @Override
    protected void clickSignIn()
    {
        clickAndWait(Locator.tagWithClass("a", "header-link").withText("Sign In"));
    }

    @Override
    protected void authenticate(String email, String password)
    {
        doAndWaitForPageToLoad(() -> fillSignInFormAndSubmit(null, email, password));
    }

    @Override
    protected void authenticateExpectingError(String email, String password)
    {
        fillSignInFormAndSubmit(null, email, password);
    }

    @Override
    protected String getAuthDescription()
    {
        return "Standard database authentication";
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(afterTest, USER1, USER2);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        DbReauthTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        USER1.create(this).setInitialPassword();
        USER2.create(this).setInitialPassword();
    }

}
