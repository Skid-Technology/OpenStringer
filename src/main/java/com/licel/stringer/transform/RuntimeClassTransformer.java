package com.licel.stringer.transform;

import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.runtime.EmptyClassCreator;
import com.licel.stringer.runtime.IntrinsicsCreator;
import com.licel.stringer.runtime.LruCacheCreator;
import com.licel.stringer.runtime.ReflectionClassCreator;
import com.licel.stringer.runtime.ResourceBundleCompiler;
import com.licel.stringer.runtime.StringerFilterInputStreamCreator;
import com.licel.stringer.runtime.StringerInflaterCreator;
import com.licel.stringer.runtime.StringerInputStreamCreator;
import com.licel.stringer.runtime.StringerStreamHandlerFactoryCreator;
import com.licel.stringer.runtime.StringerURLConnectionCreator;
import com.licel.stringer.runtime.StringerURLHandlerCreator;
import com.licel.stringer.webapp.CatalinaApplicationContextWrapperCreator;
import com.licel.stringer.webapp.WebApplicationDescriptor;
import com.licel.stringer.webapp.WebContextClassLoaderCreator;
import com.licel.stringer.webapp.WebContextLoaderListenerCreator;
import com.licel.stringer.webapp.WebXmlContextWrapperCreator;
import java.util.Properties;
import java.util.ResourceBundle;
import org.objectweb.asm.ClassVisitor;

public class RuntimeClassTransformer extends ClassTransformer {
    public static final int KIND_EMPTY_CLASS = 0;
    public static final int KIND_INPUT_STREAM = 1;
    public static final int KIND_INFLATER = 2;
    public static final int KIND_FILTER_INPUT_STREAM = 3;
    public static final int KIND_URL_HANDLER = 4;
    public static final int KIND_URL_CONNECTION = 5;
    public static final int KIND_RESOURCE_BUNDLE = 6;
    public static final int KIND_STREAM_HANDLER_FACTORY = 7;
    public static final int KIND_REFLECTION_CLASS = 8;
    public static final int KIND_INTRINSICS = 9;
    public static final int KIND_LRU_CACHE = 10;
    public static final int KIND_WEB_CONTEXT_LOADER_LISTENER = 11;
    public static final int KIND_WEB_SERVLET_CONTEXT_WRAPPER = 12;
    public static final int KIND_CATALINA_CONTEXT_WRAPPER = 13;
    public static final int KIND_WEB_CONTEXT_CLASS_LOADER = 14;

    private ClassInfo classInfo;
    private final Object payload;
    private final int generatedClassKind;
    private final CryptoContainer cryptoContainer;

    public RuntimeClassTransformer(int generatedClassKind, int classFileVersion, String className, String superName) {
        this(null, generatedClassKind, classFileVersion, className, superName, null);
    }

    public RuntimeClassTransformer(
            CryptoContainer cryptoContainer,
            int generatedClassKind,
            int classFileVersion,
            String className,
            String superName,
            Object payload) {
        this.classInfo = new ClassInfo(className);
        this.classInfo.superName = superName;
        this.classInfo.classFileVersion = classFileVersion;
        this.generatedClassKind = generatedClassKind;
        this.payload = payload;
        this.cryptoContainer = cryptoContainer;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        switch (this.generatedClassKind) {
            case KIND_INPUT_STREAM:
                return new StringerInputStreamCreator(classVisitor, this.classInfo);
            case KIND_INFLATER:
                return new StringerInflaterCreator(classVisitor, this.classInfo);
            case KIND_FILTER_INPUT_STREAM:
                return new StringerFilterInputStreamCreator(classVisitor, this.classInfo);
            case KIND_URL_HANDLER:
                return new StringerURLHandlerCreator(classVisitor, this.classInfo);
            case KIND_URL_CONNECTION:
                return new StringerURLConnectionCreator(classVisitor, this.classInfo);
            case KIND_RESOURCE_BUNDLE:
                Object[] bundleArgs = (Object[]) this.payload;
                return new ResourceBundleCompiler(
                        classVisitor,
                        this.classInfo,
                        (ResourceBundle) bundleArgs[0],
                        this.cryptoContainer,
                        (String) bundleArgs[1],
                        (String) bundleArgs[2]);
            case KIND_STREAM_HANDLER_FACTORY:
                return new StringerStreamHandlerFactoryCreator(classVisitor, this.classInfo);
            case KIND_REFLECTION_CLASS:
                Object[] reflectionArgs = (Object[]) this.payload;
                this.classInfo = (ClassInfo) reflectionArgs[1];
                return new ReflectionClassCreator(
                        classVisitor,
                        this.classInfo,
                        (Properties) reflectionArgs[0],
                        this.cryptoContainer);
            case KIND_INTRINSICS:
                return new IntrinsicsCreator(classVisitor, this.classInfo);
            case KIND_LRU_CACHE:
                return new LruCacheCreator(classVisitor, this.classInfo);
            case KIND_WEB_CONTEXT_LOADER_LISTENER:
                return new WebContextLoaderListenerCreator(classVisitor, this.classInfo, (WebApplicationDescriptor) this.payload);
            case KIND_WEB_SERVLET_CONTEXT_WRAPPER:
                return new WebXmlContextWrapperCreator(classVisitor, this.classInfo);
            case KIND_CATALINA_CONTEXT_WRAPPER:
                return new CatalinaApplicationContextWrapperCreator(classVisitor, this.classInfo);
            case KIND_WEB_CONTEXT_CLASS_LOADER:
                return new WebContextClassLoaderCreator(classVisitor, this.classInfo, (Properties) this.payload);
            default:
                return new EmptyClassCreator(classVisitor, this.classInfo);
        }
    }

    @Override
    public int getClassWriterFlags() {
        return 0;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
