package cc.softwarefactory.lokki.android.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.datasources.contacts.DefaultContactDataSource;
import cc.softwarefactory.lokki.android.models.Contact;
import cc.softwarefactory.lokki.android.utilities.JsonUtils;
import cc.softwarefactory.lokki.android.utilities.PreferenceUtils;
import cc.softwarefactory.lokki.android.utilities.Utils;

public class ContactService extends ApiService {

    private final String restPath = "contacts";
    private final String TAG = "ContactService";

    private Map<String, Contact> phoneContacts;

    public ContactService(Context context) {
        super(context);
        generatePhoneContactsMapFromList(new DefaultContactDataSource().getContacts(context));
    }

    private void generatePhoneContactsMapFromList(List<Contact> phoneContactsList) {
        phoneContacts = new HashMap<>();
        for(Contact contact : phoneContactsList) {
            if (contact.getEmail() != null)
                phoneContacts.put(contact.getEmail(), contact);
        }
    }


    @Override
    String getTag() {
        return TAG;
    }

    @Override
    String getCacheKey() {
        return PreferenceUtils.KEY_DASHBOARD;
    }

    private boolean contactIdIsValid(Contact contact) {
        return (contact != null && contact.getUserId() != null);
    }
    private boolean contactRequestIsValid(ContactsRequest request) {
        return (request.getItems() != null && request.getItems().size() > 0);
    }

    public void getContacts() {
        get(restPath, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String json, AjaxStatus status) {
                Log.d(TAG, "contactsCallback");

                if (json == null) {
                    Log.e(TAG, "Error fetching contacts: " + status.getCode() + " - " + status.getMessage());
                    return;
                }
                Log.d(TAG, "contacts JSON returned: " + json);

                try {
                    List<Contact> contacts = JsonUtils.createListFromJson(json, Contact.class);
                    MainApplication.contacts = new ArrayList<Contact>();
                    for (Contact contact : contacts) {
                        MainApplication.contacts.add(getSynchronizedWithPhone(contact));
                    }
                    updateCache();
                    Intent intent = new Intent("CONTACTS-UPDATE");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                } catch (IOException e) {
                    Log.e(TAG, "Error: Failed to parse places JSON.");
                    e.printStackTrace();
                    MainApplication.contacts = new ArrayList<Contact>();
                }
            }
        });
    }

    public List<Contact> getContactsVisibleToMe() {
        List<Contact> visible = new ArrayList<>();
        if (MainApplication.contacts == null) return visible;
        for (Contact contact : MainApplication.contacts) {
            if (!contact.isIgnored() && contact.isVisibleToMe()) visible.add(contact);
        }
        return visible;
    }

    private abstract class ContactsRequest {
        protected abstract void addItem(Contact contact);
        protected abstract List<String> getItems();
        protected abstract void initalizeItems();

        public ContactsRequest(List<Contact> contacts) {
            initalizeItems();
            for (Contact contact : contacts) addItem(contact);
        }

        public ContactsRequest(Contact contact) {
            initalizeItems();
            addItem(contact);
        }
    }

    private class AllowContactsRequest extends ContactsRequest {
        public List<String> emails;

        public AllowContactsRequest(List<Contact> contacts) {
            super(contacts);
        }

        public AllowContactsRequest(Contact contact) {
            super(contact);
        }

        @Override
        protected void addItem(Contact contact) {
           if (contact != null && contact.getEmail() != null) emails.add(contact.getEmail());
        }

        @Override
        protected List<String> getItems() {
            return emails;
        }

        @Override
        protected void initalizeItems() {
            emails = new ArrayList<>();
        }
    }

    private class IgnoreContactsRequest extends ContactsRequest {
        public List<String> ids;

        public IgnoreContactsRequest(List<Contact> contacts) {
            super(contacts);
        }

        public IgnoreContactsRequest(Contact contact) {
            super(contact);
        }

        @Override
        protected void addItem(Contact contact) {
            if (contact != null && contact.getUserId() != null) ids.add(contact.getUserId());
        }

        @Override
        protected List<String> getItems() {
            return ids;
        }

        @Override
        protected void initalizeItems() {
            ids = new ArrayList<>();
        }
    }

    // If you call this, you must manually remember to update contacts with getContacts() in cb.
    public void allowContacts(List<Contact> contacts, AjaxCallback<String> cb) {
        Log.d(TAG, "allowPeople");
        ContactsRequest request = new AllowContactsRequest(contacts);
        if (!contactRequestIsValid(request)) {
            Log.e(TAG, "Attempted to allow 0 emails");
            return;
        }
        try {
            post(restPath + "/allow", JsonUtils.toJSONObject(request), cb);
        } catch (JsonProcessingException | JSONException e) {
            Log.e(TAG, "Failed to create json object from allow contact request");
            e.printStackTrace();
        }
    }

    private void logStatus(String request, AjaxStatus status) {
        Log.d(TAG, request + " result code: " + status.getCode());
        Log.d(TAG, request + " result message: " + status.getMessage());
        if(status.getError() != null) {
            Log.e(TAG, request + " ERROR: " + status.getError());
        }
    }

    public void disallowContact(Contact contact) {
        Log.d(TAG, "disallowUsers");
        if (!contactIdIsValid(contact)) {
            Log.e(TAG, "Attempted to disallow invalid email");
            return;
        }
        delete(restPath + "/allow/" + contact.getUserId(), new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("disallowUser", status);
                if (status.getError() == null) {
                    Log.d(TAG, "updating contacts");
                    getContacts();
                }
            }
        });
    }

    public void ignoreContact(Contact contact) {
        Log.d(TAG, "ignoreUsers");
        ContactsRequest request = new IgnoreContactsRequest(contact);
        if (!contactRequestIsValid(request)) {
            Log.e(TAG, "Attempted to ignore invalid email");
            return;
        }
        try {
            post(restPath + "/ignore", JsonUtils.toJSONObject(request), new AjaxCallback<String>() {
                @Override
                public void callback(String url, String result, AjaxStatus status) {
                    logStatus("ignoreUsers", status);
                    if (status.getError() == null) {
                        Log.d(TAG, "Getting new contacts");
                        getContacts();
                    }
                }
            });
        } catch (JsonProcessingException | JSONException e) {
            Log.e(TAG, "Failed to create json object from ignore contact request");
            e.printStackTrace();
        }
    }

    public void unignoreContact(Contact contact) {
        Log.d(TAG, "unignoreContact");
        if (!contactIdIsValid(contact)) {
            Log.e(TAG, "Attempted to ignore invalid email");
            return;
        }
        delete(restPath + "/ignore/" + contact.getUserId(), new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("unignoreUsers", status);
                if (status.getError() == null) {
                    Log.d(TAG, "Getting new contacts");
                    getContacts();
                }
            }
        });

    }

    private class RenameRequest {
        public String name;
        public RenameRequest(String name) {this.name = name;}
    }

    public void renameContact(Contact contact, String newName) {
        Log.d(TAG, "Rename contact");

        if (!contactIdIsValid(contact)) {
            Log.e(TAG, "Attempted to rename invalid contact");
            return;
        } else if (newName == null || newName.trim().equals("")) {
            Log.e(TAG, "Attempted to rename with invalid name");
            return;
        }

        try {
            post(restPath + "/rename/" + contact.getUserId(), JsonUtils.toJSONObject(new RenameRequest(newName)), new AjaxCallback<String>() {
                @Override
                public void callback(String url, String result, AjaxStatus status) {
                    logStatus("renameContact callback", status);
                    if (status.getError() == null) {
                        Log.d(TAG, "Getting new contacts");
                        getContacts();
                    }
                }
            });
        } catch (JsonProcessingException | JSONException e) {
            Log.e(TAG, "Creating a rename request json failed");
            e.printStackTrace();
            getContacts();
            return;
        }
    }

    public void removeContact(Contact contact) {
        if (!contactIdIsValid(contact)) {
            Log.e(TAG, "Attempted to delete invalid email");
            return;
        }

        delete(restPath + "/" + contact.getUserId(), new AjaxCallback<String>() {
            @Override
            public void callback(String url, String result, AjaxStatus status) {
                logStatus("removeContact", status);
                if (status.getError() == null) {
                    Log.d(TAG, "updating contacts");
                    getContacts();
                }
            }
        });
    }

    public void updateCache() {
        try {
            updateCache(new ObjectMapper().writeValueAsString(MainApplication.contacts));
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Serializing places to JSON failed");
            e.printStackTrace();
        }
    }

    public List<Contact> getFromCache() throws IOException {
        return JsonUtils.createListFromJson(PreferenceUtils.getString(context, getCacheKey()), Contact.class);
    }

    public List<Contact> getPhoneContacts() {
        return new ArrayList(phoneContacts.values());
    }

    // for dependency injenction
    public void setPhoneContacts(List<Contact> phoneContacts) {
        generatePhoneContactsMapFromList(phoneContacts);
    }

    private Contact getSynchronizedWithPhone(Contact contact) {
        Contact synchronizedContact = new Contact();
        Contact phoneContact = phoneContacts.get(contact.getEmail());
        if (phoneContact != null) {
            synchronizedContact.setEmail(contact.getEmail());
            synchronizedContact.setName(contact.getName() == null ? phoneContact.getName() : contact.getName());
            synchronizedContact.setLocation(contact.getLocation());
            synchronizedContact.setCanSeeMe(contact.isCanSeeMe());
            synchronizedContact.setUserId(contact.getUserId());
            synchronizedContact.setIsIgnored(contact.isIgnored());

            Bitmap photo = null;
            if (phoneContact != null) photo = phoneContact.getPhoto();
            synchronizedContact.setPhoto(photo);
            contact = synchronizedContact;
        }
        if (contact.getPhoto() == null) {
            contact.setPhoto(Utils.getDefaultAvatarInitials(context, contact.toString()));
        }
       return contact;
    }
}
