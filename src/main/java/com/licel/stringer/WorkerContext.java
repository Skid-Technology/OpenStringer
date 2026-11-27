package com.licel.stringer;

import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.io.SourceIterator;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.naming.NameGenerator;
import java.security.SecureRandom;
import java.util.HashMap;

public class WorkerContext {
    public NameGenerator nameGenerator;
    public NameGenerator internalNameGenerator;
    public Config config;
    public SecureRandom secureRandom;
    public SourceIterator sourceIterator;
    public int processedStringCount;
    public CryptoContainer cryptoContainer;
    public HashMap<String, ClassInfo> jdkClassCache = new HashMap<>();
    public long integrityKey0;
    public long integrityKey1;
    public HashMap<String, Object> attributes = new HashMap<>();
}
