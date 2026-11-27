package com.licel.stringer.io;

public class ZipAlignmentConstants {
    public static final int LOCAL_FILE_HEADER_SIZE = 30;
    public static final int VERSION_NEEDED = 20;
    public static final int EXTRA_FIELD_LENGTH_OFFSET = 28;
    public static final int EXTRA_FIELD_LENGTH_SIZE = 2;
    public static final int DATA_DESCRIPTOR_FLAG = 8;
    public static final int DATA_DESCRIPTOR_SIZE = 16;
    public static final int DEFAULT_ALIGNMENT = 8;
    public static final int PAGE_ALIGNMENT = 4096;
    public static final String ALIGNED_LABEL = "ALIGNED";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final int BUFFER_SIZE = 32768;
}
