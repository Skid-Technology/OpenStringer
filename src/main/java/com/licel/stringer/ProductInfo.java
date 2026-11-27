package com.licel.stringer;

import java.util.ResourceBundle;

public final class ProductInfo {
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("productInfo");

    private ProductInfo() {
    }

    public static String getTitle() {
        return RESOURCE_BUNDLE.getString("title");
    }

    public static String getConsoleTitle() {
        return RESOURCE_BUNDLE.getString("consoleTitle");
    }

    public static String getVendor() {
        return RESOURCE_BUNDLE.getString("vendor");
    }

    public static String getVersion() {
        return RESOURCE_BUNDLE.getString("version");
    }

    public static String getBuildId() {
        return RESOURCE_BUNDLE.getString("buildId");
    }

    public static String getInfoText() {
        String lineSeparator = System.lineSeparator();
        return getTitle() + lineSeparator
                + "Version: " + getVersion() + lineSeparator
                + "Build id: " + getBuildId() + lineSeparator
                + "Vendor: " + getVendor();
    }
}
