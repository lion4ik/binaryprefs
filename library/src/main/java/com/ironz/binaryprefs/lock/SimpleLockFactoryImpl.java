package com.ironz.binaryprefs.lock;

import com.ironz.binaryprefs.file.directory.DirectoryProvider;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple lock factory for providing lock by instance and global lock by preference name.
 */
public final class SimpleLockFactoryImpl implements LockFactory {

    private static final String LOCK_EXTENSION = ".lock";
    private static final Map<String, ReadWriteLock> locks = new ConcurrentHashMap<>();

    private final Map<String, Lock> processLocks = new ConcurrentHashMap<>();

    private final String name;
    private final File lockDirectory;

    public SimpleLockFactoryImpl(String name, DirectoryProvider provider) {
        this.name = name;
        this.lockDirectory = provider.getLockDirectory();
        init(name);
    }

    private void init(String name) {
        initLocalLocks(name);
        initProcessLocks(name);
    }

    private void initLocalLocks(String name) {
        if (locks.containsKey(name)) {
            return;
        }
        ReadWriteLock lock = new ReentrantReadWriteLock(true);
        locks.put(name, lock);
    }

    private void initProcessLocks(String name) {
        if (processLocks.containsKey(name)) {
            return;
        }
        File file = new File(lockDirectory, name + LOCK_EXTENSION);
        Lock lock = new ProcessFileLock(file);
        processLocks.put(name, lock);
    }

    @Override
    public Lock getReadLock() {
        ReadWriteLock readWriteLock = locks.get(name);
        return readWriteLock.readLock();
    }

    @Override
    public Lock getWriteLock() {
        ReadWriteLock readWriteLock = locks.get(name);
        return readWriteLock.writeLock();
    }

    @Override
    public Lock getProcessLock() {
        return processLocks.get(name);
    }
}