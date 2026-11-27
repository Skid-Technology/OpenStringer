package com.licel.stringer.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipAlignmentWriter {
    private static final long LOCAL_FILE_HEADER_SIGNATURE = 0x04034B50L;
    private static final long CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014B50L;
    private static final long END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054B50L;
    private static final int UTF8_ENCODING_FLAG = 2048;
    private static final int MAX_COMMENT_LENGTH = 65535;
    private static final byte MANIFEST_EXTRA_HEADER_BYTE = 85;

    private final File inputFile;
    private final int alignment;
    private final File outputFile;
    private final byte[] manifestExtraData;
    @SuppressWarnings("unused")
    private final byte[] extraMetadata;
    private ZipFile zipFile;
    private RandomAccessFile input;
    private CountingOutputStream output;
    private final List<ZipEntryRecord> entries = new ArrayList<>();
    private long inputOffset = 0L;
    private int totalPadding = 0;

    public ZipAlignmentWriter(File inputFile, File outputFile, byte[] manifestExtraData, byte[] extraMetadata) {
        this(inputFile, ZipAlignmentConstants.DEFAULT_ALIGNMENT, outputFile, manifestExtraData, extraMetadata);
    }

    public ZipAlignmentWriter(
            File inputFile,
            int alignment,
            File outputFile,
            byte[] manifestExtraData,
            byte[] extraMetadata) {
        this.inputFile = inputFile;
        this.alignment = alignment;
        this.outputFile = outputFile;
        this.manifestExtraData = manifestExtraData;
        this.extraMetadata = extraMetadata;
    }

    public void align() throws IOException {
        try {
            this.openFiles();
            this.writeLocalEntries();
            this.writeCentralDirectory();
        } catch (Exception e) {
            this.outputFile.delete();
        } finally {
            try {
                this.closeFiles();
            } catch (Exception e) {
                this.outputFile.delete();
            }
        }
    }

    private void openFiles() throws IOException {
        this.zipFile = new ZipFile(this.inputFile);
        this.input = new RandomAccessFile(this.inputFile, "r");
        this.output = new CountingOutputStream(
                new BufferedOutputStream(new FileOutputStream(this.outputFile), ZipAlignmentConstants.BUFFER_SIZE));
    }

    private void writeLocalEntries() throws IOException {
        int entryCount = this.zipFile.size();
        if (entryCount == 0) {
            return;
        }

        Enumeration<? extends ZipEntry> zipEntries = this.zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            int flags = entry.getMethod() == ZipEntry.STORED ? 0 : ZipAlignmentConstants.DATA_DESCRIPTOR_FLAG;
            flags |= UTF8_ENCODING_FLAG;

            long localHeaderOffset = this.output.getBytesWritten();
            byte[] nameBytes = entry.getName().getBytes(ZipAlignmentConstants.CHARSET_UTF8);
            int originalHeaderLength = ZipAlignmentConstants.LOCAL_FILE_HEADER_SIZE
                    + (entry.getExtra() != null ? entry.getExtra().length : 0)
                    + nameBytes.length;

            int padding;
            if (entry.getMethod() != ZipEntry.STORED) {
                padding = 0;
            } else {
                long dataOffset = this.output.getBytesWritten() + originalHeaderLength;
                if (!entry.getName().endsWith(".so")) {
                    padding = (int) ((this.alignment - dataOffset % this.alignment) % this.alignment);
                } else {
                    padding = (int) ((ZipAlignmentConstants.PAGE_ALIGNMENT
                            - dataOffset % ZipAlignmentConstants.PAGE_ALIGNMENT)
                            % ZipAlignmentConstants.PAGE_ALIGNMENT);
                }

                this.totalPadding += padding;
            }

            ZipEntryRecord record = new ZipEntryRecord(entry, localHeaderOffset, flags, padding);
            this.entries.add(record);

            byte[] extra = entry.getExtra();
            if (entry.getName().equals("META-INF/MANIFEST.MF") && this.manifestExtraData != null) {
                extra = new byte[this.manifestExtraData.length + 4];
                extra[0] = MANIFEST_EXTRA_HEADER_BYTE;
                extra[1] = MANIFEST_EXTRA_HEADER_BYTE;
                extra[2] = (byte) ((this.manifestExtraData.length >>> 0) & 0xFF);
                extra[3] = (byte) ((this.manifestExtraData.length >>> 8) & 0xFF);
                System.arraycopy(this.manifestExtraData, 0, extra, 4, this.manifestExtraData.length);
            }

            if (extra == null) {
                extra = new byte[padding];
                Arrays.fill(extra, (byte) 0);
            } else {
                byte[] paddedExtra = new byte[extra.length + padding];
                System.arraycopy(extra, 0, paddedExtra, 0, extra.length);
                Arrays.fill(paddedExtra, extra.length, paddedExtra.length, (byte) 0);
                extra = paddedExtra;
            }

            entry.setExtra(extra);

            this.output.writeUInt32(LOCAL_FILE_HEADER_SIGNATURE);
            this.output.writeUInt16(ZipAlignmentConstants.VERSION_NEEDED);
            this.output.writeUInt16(flags);
            this.output.writeUInt16(entry.getMethod());

            int[] dosDateTime = encodeDosDateTime(entry.getTime());
            this.output.writeUInt16(dosDateTime[0]);
            this.output.writeUInt16(dosDateTime[1]);

            this.output.writeUInt32(entry.getCrc());
            this.output.writeUInt32(entry.getCompressedSize());
            this.output.writeUInt32(entry.getSize());
            this.output.writeUInt16(nameBytes.length);
            this.output.writeUInt16(extra.length);
            this.output.write(nameBytes);
            this.output.write(extra, 0, extra.length);

            this.inputOffset += originalHeaderLength;

            long dataLength;
            if ((flags & ZipAlignmentConstants.DATA_DESCRIPTOR_FLAG) != 0) {
                dataLength = (entry.isDirectory() ? 0L : entry.getCompressedSize())
                        + ZipAlignmentConstants.DATA_DESCRIPTOR_SIZE;
            } else {
                dataLength = entry.isDirectory() ? 0L : entry.getCompressedSize();
            }

            if (dataLength > 0L) {
                this.input.seek(this.inputOffset);
                long copied = 0L;
                byte[] copyBuffer = new byte[ZipAlignmentConstants.BUFFER_SIZE];

                while (copied < dataLength) {
                    int chunkSize = (int) Math.min(ZipAlignmentConstants.BUFFER_SIZE, dataLength - copied);
                    int read = this.input.read(copyBuffer, 0, chunkSize);
                    if (read <= 0) {
                        break;
                    }

                    this.output.write(copyBuffer, 0, read);
                    copied += read;
                }
            }

            this.inputOffset += dataLength;
        }
    }

    private void writeCentralDirectory() throws IOException {
        long centralDirectoryOffset = this.output.getBytesWritten();

        for (ZipEntryRecord record : this.entries) {
            ZipEntry entry = record.entry;
            int[] dosDateTime = encodeDosDateTime(entry.getTime());

            this.output.writeUInt32(CENTRAL_DIRECTORY_HEADER_SIGNATURE);
            this.output.writeUInt16(ZipAlignmentConstants.VERSION_NEEDED);
            this.output.writeUInt16(ZipAlignmentConstants.VERSION_NEEDED);
            this.output.writeUInt16(record.flags);
            this.output.writeUInt16(entry.getMethod());
            this.output.writeUInt16(dosDateTime[0]);
            this.output.writeUInt16(dosDateTime[1]);
            this.output.writeUInt32(entry.getCrc());
            this.output.writeUInt32(entry.getCompressedSize());
            this.output.writeUInt32(entry.getSize());

            byte[] nameBytes = entry.getName().getBytes(ZipAlignmentConstants.CHARSET_UTF8);
            this.output.writeUInt16(nameBytes.length);

            byte[] extra = entry.getExtra();
            int centralExtraLength = extra != null ? extra.length - record.padding : 0;
            this.output.writeUInt16(centralExtraLength);

            byte[] commentBytes;
            if (entry.getComment() != null) {
                commentBytes = entry.getComment().getBytes(ZipAlignmentConstants.CHARSET_UTF8);
                this.output.writeUInt16(Math.min(commentBytes.length, MAX_COMMENT_LENGTH));
            } else {
                commentBytes = null;
                this.output.writeUInt16(0);
            }

            this.output.writeUInt16(0);
            this.output.writeUInt16(0);
            this.output.writeUInt32(0L);
            this.output.writeUInt32(record.localHeaderOffset);
            this.output.write(nameBytes);

            if (extra != null) {
                this.output.write(extra, 0, centralExtraLength);
            }

            if (commentBytes != null) {
                this.output.write(commentBytes, 0, Math.min(commentBytes.length, MAX_COMMENT_LENGTH));
            }
        }

        long centralDirectorySize = this.output.getBytesWritten() - centralDirectoryOffset;
        int entryCount = this.entries.size();

        this.output.writeUInt32(END_OF_CENTRAL_DIRECTORY_SIGNATURE);
        this.output.writeUInt16(0);
        this.output.writeUInt16(0);
        this.output.writeUInt16(entryCount);
        this.output.writeUInt16(entryCount);
        this.output.writeUInt32(centralDirectorySize);
        this.output.writeUInt32(centralDirectoryOffset);

        if (this.zipFile.getComment() != null) {
            byte[] zipCommentBytes = this.zipFile.getComment().getBytes(ZipAlignmentConstants.CHARSET_UTF8);
            this.output.writeUInt16(zipCommentBytes.length);
            this.output.write(zipCommentBytes);
        } else {
            this.output.writeUInt16(0);
        }

        this.output.flush();
    }

    private static int[] encodeDosDateTime(long timeMillis) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date(timeMillis));
        int year = calendar.get(Calendar.YEAR);
        int dosDate;
        int dosTime;
        if (year < 1980) {
            dosDate = 33;
            dosTime = 0;
        } else {
            dosDate = calendar.get(Calendar.DAY_OF_MONTH);
            dosDate = ((calendar.get(Calendar.MONTH) + 1) << 5) | dosDate;
            dosDate = ((calendar.get(Calendar.YEAR) - 1980) << 9) | dosDate;
            dosTime = calendar.get(Calendar.SECOND) >> 1;
            dosTime = (calendar.get(Calendar.MINUTE) << 5) | dosTime;
            dosTime = (calendar.get(Calendar.HOUR_OF_DAY) << 11) | dosTime;
        }

        return new int[]{dosTime, dosDate};
    }

    private void closeFiles() throws IOException {
        try {
            if (this.zipFile != null) {
                this.zipFile.close();
            }
        } finally {
            try {
                if (this.input != null) {
                    this.input.close();
                }
            } finally {
                if (this.output != null) {
                    this.output.close();
                }
            }
        }
    }
}
