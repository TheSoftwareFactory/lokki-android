package cc.softwarefactory.lokki.android.models;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class JSONMap<V> extends JSONModel implements Map<String, V> {

    //implementing class must handle mapping json key to map.
    protected abstract Map<String, V> getMap();

    public void update(String id, V value) {
        getMap().put(id, value);
    }

    public boolean has(String key) {
        return getMap().containsKey(key);
    }

    @Override
    public void clear() {
        getMap().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return getMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getMap().containsValue(value);
    }

    @NonNull
    @Override
    public Set<Entry<String, V>> entrySet() {
        return getMap().entrySet();
    }

    @Override
    public V get(Object key) {
        return getMap().get(key);
    }

    @Override
    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    @NonNull
    @Override
    public Set<String> keySet() {
        return getMap().keySet();
    }

    @Override
    public V put(String key, V value) {
        return getMap().put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> map) {
        getMap().putAll(map);
    }

    @Override
    public V remove(Object key) {
        return getMap().remove(key);
    }

    @Override
    public int size() {
        return getMap().size();
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return getMap().values();
    }
}
