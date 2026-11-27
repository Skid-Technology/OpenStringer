package com.licel.stringer.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarIndex {
    public static final String INDEX_NAME = "META-INF/INDEX.LIST";

    private static final boolean META_INF_FILENAMES =
            "true".equals(System.getProperty("sun.misc.JarIndex.metaInfFilenames"));

    private final HashMap<String, LinkedList<String>> indexMap = new HashMap<>();
    private final HashMap<String, LinkedList<String>> jarMap = new HashMap<>();
    private String[] jarFiles;

    public JarIndex() {
    }

    public JarIndex(InputStream in) throws IOException {
        this();
        this.read(in);
    }

    public JarIndex(String[] jarFiles) throws IOException {
        this();
        this.jarFiles = jarFiles;
    }

    public static JarIndex getJarIndex(JarFile jar) throws IOException {
        JarIndex index = null;
        JarEntry entry = jar.getJarEntry(INDEX_NAME);
        if (entry != null) {
            index = new JarIndex(jar.getInputStream(entry));
        }

        return index;
    }

    public String[] getJarFiles() {
        return this.jarFiles;
    }

    private void addToList(String key, String value, HashMap<String, LinkedList<String>> map) {
        LinkedList<String> list = map.get(key);
        if (list == null) {
            list = new LinkedList<>();
            list.add(value);
            map.put(key, list);
        } else if (!list.contains(value)) {
            list.add(value);
        }
    }

    public LinkedList<String> get(String fileName) {
        LinkedList<String> jars = this.indexMap.get(fileName);
        int pos;
        if (jars == null && (pos = fileName.lastIndexOf("/")) != -1) {
            jars = this.indexMap.get(fileName.substring(0, pos));
        }

        return jars;
    }

    public void add(String fileName, String jarName) {
        String packageName;
        int pos = fileName.lastIndexOf("/");
        if (pos != -1) {
            packageName = fileName.substring(0, pos);
        } else {
            packageName = fileName;
        }

        this.addMapping(packageName, jarName);
    }

    public void addResource(String resourceName, String jarName) {
        this.addMapping(resourceName, jarName);
    }

    private void addMapping(String jarItem, String jarName) {
        this.addToList(jarItem, jarName, this.indexMap);
        this.addToList(jarName, jarItem, this.jarMap);
    }

    @SuppressWarnings("unused")
    private void parseJars(String[] files) throws IOException {
        if (files == null) {
            return;
        }

        for (String currentJar : files) {
            ZipFile zrf = new ZipFile(currentJar.replace('/', File.separatorChar));
            Enumeration<? extends ZipEntry> entries = zrf.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();
                if (!fileName.equals("META-INF/")
                        && !fileName.equals(INDEX_NAME)
                        && !fileName.equals("META-INF/MANIFEST.MF")) {
                    if (!META_INF_FILENAMES || !fileName.startsWith("META-INF/")) {
                        this.addMapping(fileName, currentJar);
                    } else if (!entry.isDirectory()) {
                        this.addMapping(fileName, currentJar);
                    }
                }
            }

            zrf.close();
        }
    }

    public void write(OutputStream out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write("JarIndex-Version: 1.0\n\n");
        if (this.jarFiles != null) {
            for (String jar : this.jarFiles) {
                LinkedList<String> jarList = this.jarMap.get(jar);
                if (jarList != null) {
                    for (String item : jarList) {
                        writer.write(item + "\n");
                    }
                }

                writer.write("\n");
            }

            writer.flush();
        }
    }

    public void read(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String currentJar = null;
        Vector<String> jars = new Vector<>();

        String line;
        while ((line = reader.readLine()) != null && !line.endsWith(".jar")) {
        }

        for (; line != null; line = reader.readLine()) {
            if (line.length() != 0) {
                if (line.endsWith(".jar")) {
                    currentJar = line;
                    jars.add(currentJar);
                } else {
                    this.addMapping(line, currentJar);
                }
            }
        }

        this.jarFiles = jars.toArray(new String[jars.size()]);
    }

    public void merge(JarIndex toIndex, String path) {
        for (Map.Entry<String, LinkedList<String>> entry : this.indexMap.entrySet()) {
            String packageName = entry.getKey();

            for (String jarName : entry.getValue()) {
                if (path != null) {
                    jarName = path.concat(jarName);
                }

                toIndex.addMapping(packageName, jarName);
            }
        }
    }
}
