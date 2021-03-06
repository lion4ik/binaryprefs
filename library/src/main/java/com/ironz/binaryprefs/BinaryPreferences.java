package com.ironz.binaryprefs;

import com.ironz.binaryprefs.cache.CacheProvider;
import com.ironz.binaryprefs.events.EventBridge;
import com.ironz.binaryprefs.events.OnSharedPreferenceChangeListenerWrapper;
import com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.ironz.binaryprefs.file.transaction.TransactionElement;
import com.ironz.binaryprefs.lock.LockFactory;
import com.ironz.binaryprefs.serialization.SerializerFactory;
import com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.ironz.binaryprefs.task.Completable;
import com.ironz.binaryprefs.task.TaskExecutor;

import java.util.*;
import java.util.concurrent.locks.Lock;

final class BinaryPreferences implements Preferences {

    private final FileTransaction fileTransaction;
    private final EventBridge eventsBridge;
    private final CacheProvider cacheProvider;
    private final TaskExecutor taskExecutor;
    private final SerializerFactory serializerFactory;
    private final Lock readLock;
    private final Lock writeLock;

    BinaryPreferences(FileTransaction fileTransaction,
                      EventBridge eventsBridge,
                      CacheProvider cacheProvider,
                      TaskExecutor taskExecutor,
                      SerializerFactory serializerFactory,
                      LockFactory lockFactory) {
        this.fileTransaction = fileTransaction;
        this.eventsBridge = eventsBridge;
        this.cacheProvider = cacheProvider;
        this.taskExecutor = taskExecutor;
        this.serializerFactory = serializerFactory;
        this.readLock = lockFactory.getReadLock();
        this.writeLock = lockFactory.getWriteLock();
        fetchCache();
    }

    private void fetchCache() {
        readLock.lock();
        try {
            Completable submit = taskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    if (cacheProvider.keys().length != 0) {
                        return;
                    }
                    for (TransactionElement element : fileTransaction.fetch()) {
                        String name = element.getName();
                        byte[] bytes = element.getContent();
                        Object o = serializerFactory.deserialize(name, bytes);
                        cacheProvider.put(name, o);
                    }
                }
            });
            submit.completeBlockingUnsafe();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<String, Object> getAll() {
        readLock.lock();
        try {
            Map<String, Object> all = cacheProvider.getAll();
            HashMap<String, Object> clone = new HashMap<>(all.size());
            for (String key : all.keySet()) {
                Object value = all.get(key);
                Object redefinedValue = serializerFactory.redefineMutable(value);
                clone.put(key, redefinedValue);
            }
            return Collections.unmodifiableMap(clone);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getString(String key, String defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (String) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                Set<String> strings = (Set<String>) cacheProvider.get(key);
                return new HashSet<>(strings);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (int) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (long) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (float) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (boolean) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Persistable> T getPersistable(String key, T defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                T t = (T) cacheProvider.get(key);
                return (T) t.deepClone();
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public byte getByte(String key, byte defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (byte) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public short getShort(String key, short defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (short) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public char getChar(String key, char defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (char) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getDouble(String key, double defValue) {
        readLock.lock();
        try {
            if (cacheProvider.contains(key)) {
                return (double) cacheProvider.get(key);
            }
            return defValue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(String key) {
        readLock.lock();
        try {
            return cacheProvider.contains(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public PreferencesEditor edit() {
        readLock.lock();
        try {
            return new BinaryPreferencesEditor(
                    fileTransaction,
                    eventsBridge,
                    taskExecutor,
                    serializerFactory,
                    cacheProvider,
                    writeLock
            );
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<String> keys() {
        readLock.lock();
        try {
            return Arrays.asList(cacheProvider.keys());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        writeLock.lock();
        try {
            OnSharedPreferenceChangeListenerWrapper wrapper = new OnSharedPreferenceChangeListenerWrapper(this, listener);
            eventsBridge.registerOnSharedPreferenceChangeListener(wrapper);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        writeLock.lock();
        try {
            OnSharedPreferenceChangeListenerWrapper wrapper = new OnSharedPreferenceChangeListenerWrapper(this, listener);
            eventsBridge.unregisterOnSharedPreferenceChangeListener(wrapper);
        } finally {
            writeLock.unlock();
        }
    }
}