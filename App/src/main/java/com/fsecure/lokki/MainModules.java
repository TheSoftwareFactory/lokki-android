package com.fsecure.lokki;

import com.fsecure.lokki.utils.ContactUtils;
import com.fsecure.lokki.utils.DefaultContactUtils;

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;

@Module (
        library = true,
        injects = {
                MainApplication.class,
                AddContactsFragment.class
        }
)
public class MainModules {

    @Provides @Singleton
    ContactUtils provideContactUtils() {
        return new DefaultContactUtils();
    }

}
