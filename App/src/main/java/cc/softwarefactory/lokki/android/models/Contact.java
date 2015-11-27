package cc.softwarefactory.lokki.android.models;

/**
 * Contact model. Getting field synchronizes fields with phone's contacts.
 */
public class Contact extends Person implements Comparable<Contact> {
    private String name;
    private boolean isIgnored;
    private boolean canSeeMe;

    @Override
    public int compareTo(Contact another) {
        if (this.getName() != null && another.getName() != null)
            return this.getName().compareTo(another.getName());
        return this.getEmail().compareTo(another.getEmail());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public void setIsIgnored(boolean isIgnored) {
        this.isIgnored = isIgnored;
    }

    public boolean isCanSeeMe() {
        return canSeeMe;
    }

    public void setCanSeeMe(boolean canSeeMe) {
        this.canSeeMe = canSeeMe;
    }

    public boolean isVisibleToMe() {
        return (this.getLocation() != null);
    }

    @Override
    public String toString() {
        if (name == null) return this.getEmail();
        return name;
    }

    private String getUnambiguousEmail(String addr) {
        return addr.trim().toLowerCase();
    }

    public boolean emailIsSameAs(String email) {
        return getUnambiguousEmail(this.getEmail()).equals(getUnambiguousEmail(email));
    }
}
