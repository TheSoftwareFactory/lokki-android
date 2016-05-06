package cc.softwarefactory.lokki.android.junit.modelTests;

/**
 * Created on 21.4.2016 for the unit testing on functions of Contact class
 */

import org.junit.Test;
import cc.softwarefactory.lokki.android.models.Contact;
import static org.junit.Assert.*;

public class ContactTests {

    private String name = "LokkiUserOne";
    Contact ContactObj = new Contact();

    @Test
    public void setNameTest() {     // Test to check the setName() & getName() methods
        ContactObj.setName(name);
        assertEquals(ContactObj.getName(), name);
    }

    @Test
    public void isIgnoredTest(){    // Test to check the isIgnored() & setIsIgnored() methods
        ContactObj.setIsIgnored(true);
        boolean isIgnoredResult = ContactObj.isIgnored();
        assertTrue("Unit Test Comment: Issue in the isIgnoredTest() method in Contact.java", isIgnoredResult);
    }

    @Test
    public void canSeeMeTest(){    // Test to check the canSeeMe() & setCanSeeMe() methods
        ContactObj.setCanSeeMe(true);
        boolean canSeeMeResult = ContactObj.isCanSeeMe();
        assertTrue("Unit Test Comment: Issue in the canSeeMeTest() method in Contact.java", canSeeMeResult);
    }

    @Test
    public void isVisibleToMeTest(){    // Test to check the isVisibleToMe() methods
        boolean isVisibleToMeResult = ContactObj.isVisibleToMe();
        assertFalse("Unit Test Comment: Issue in the isVisibleToMe() method in Contact.java", isVisibleToMeResult);
    }
}
