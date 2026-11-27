package com.licel.stringer.signing;

public interface ZipEntryFilter {
    boolean checkEntry(String entryName);
}
