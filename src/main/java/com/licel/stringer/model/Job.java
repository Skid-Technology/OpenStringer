package com.licel.stringer.model;

public class Job {
    public static final int TRANSFORM_CLASS = 0;
    public static final int COPY_ENTRY = 1;
    public static final int ENCRYPT_RESOURCE = 2;
    public static final int REMOVE_ENTRY = 3;
    public static final int COMPILE_BUNDLE = 4;
    public static final int PATCH_MANIFEST = 5;
    public static final int TRANSFORM_WEB_XML = 6;

    public int type;
    public Object payload;

    public Job(int type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
