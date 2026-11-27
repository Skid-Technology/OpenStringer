package com.licel.stringer;

import java.util.ArrayList;

public class ProtectionElementParamsContainer extends FilteredParams {
    protected ArrayList<ProtectionElementParams> elements = new ArrayList<>();

    public ProtectionElementParams createElement() {
        ProtectionElementParams element = new ProtectionElementParams();
        this.elements.add(element);
        return element;
    }

    public ArrayList<ProtectionElementParams> getElements() {
        return this.elements;
    }
}
