package com.licel.stringer.model;

import java.util.Formatter;
import org.objectweb.asm.Opcodes;

public class MemberInfo {
    public static final int KIND_INVOKESTATIC = 0;
    public static final int KIND_INVOKESPECIAL = 1;
    public static final int KIND_INVOKEVIRTUAL = 2;
    public static final int KIND_NEW = 3;
    public static final int KIND_GETFIELD = 4;
    public static final int KIND_PUTFIELD = 5;
    public static final int KIND_GETSTATIC = 6;
    public static final int KIND_PUTSTATIC = 7;

    private int referenceIndex;
    private int accessKind;
    public int ownerClassIndex;
    public String name;
    public String descriptor;
    public String owner;
    public int encodedId;
    public int referenceMask;

    @Override
    public String toString() {
        return this.referenceIndex + " => " + (this.accessKind < 4 ? "method " : "field  ") + this.owner + "->" + this.name + ":" + this.descriptor;
    }

    public int getEncodedReference() {
        return this.referenceMask << 16 | this.referenceIndex ^ this.referenceMask & 65535;
    }

    public int getReferenceIndex() {
        return this.referenceIndex;
    }

    public void setReferenceIndex(int index) {
        if (index >= 0 && index <= 65535) {
            this.referenceIndex = index;
        } else {
            Formatter formatter = new Formatter();
            throw new RuntimeException(
                    formatter.format(
                            "The number of method references is more than 65536 that exceeds the limitation for Hide Access.%n Please adjust your Hide Access filter by excluding insensitive classes."
                    ).toString()
            );
        }
    }

    public void setReferenceMask(int mask) {
        if (mask >= 0 && mask <= 65535) {
            this.referenceMask = mask;
        } else {
            throw new RuntimeException("" + mask);
        }
    }

    public int getAccessKind() {
        return this.accessKind;
    }

    public void setAccessKindFromOpcode(int opcode) {
        switch (opcode) {
            case Opcodes.GETSTATIC:
                this.accessKind = KIND_GETSTATIC;
                break;
            case Opcodes.PUTSTATIC:
                this.accessKind = KIND_PUTSTATIC;
                break;
            case Opcodes.GETFIELD:
                this.accessKind = KIND_GETFIELD;
                break;
            case Opcodes.PUTFIELD:
                this.accessKind = KIND_PUTFIELD;
                break;
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
                this.accessKind = KIND_INVOKEVIRTUAL;
                break;
            case Opcodes.INVOKESPECIAL:
                this.accessKind = KIND_INVOKESPECIAL;
                break;
            case Opcodes.INVOKESTATIC:
                this.accessKind = KIND_INVOKESTATIC;
                break;
            case Opcodes.NEW:
                this.accessKind = KIND_NEW;
                break;
            default:
                throw new RuntimeException();
        }
    }
}
