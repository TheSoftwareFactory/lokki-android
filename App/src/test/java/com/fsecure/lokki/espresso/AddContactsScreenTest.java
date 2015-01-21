package com.fsecure.lokki.espresso;

import android.content.Intent;
import android.provider.ContactsContract;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.mock.MockContentProvider;


import com.fsecure.lokki.FirstTimeActivity;
import com.fsecure.lokki.MainActivity;

import org.mockito.Mockito;

public class AddContactsScreenTest extends ActivityInstrumentationTestCase2<MainActivity> {


    public AddContactsScreenTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

    }

    public void testNull() {
        while (true) {
            assertEquals(1, 1);
        }
    }




}
