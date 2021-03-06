package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.common.LoggerFactory;
import com.dci.intellij.dbn.common.routine.ThrowableCallable;
import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Document;
import org.jdom.adapters.XML4JDOMAdapter;
import org.jdom.input.DOMBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommonUtil {
    private static final Logger LOGGER = LoggerFactory.createLogger();

    public static boolean isPluginCall() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().contains(".dbn.")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCalledThrough(Class clazz) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        try {
            for (int i = 3; i < stackTraceElements.length; i++) {
                StackTraceElement stackTraceElement = stackTraceElements[i];
                String className = stackTraceElement.getClassName();
                if (clazz.getName().equals(className) /*|| clazz.isAssignableFrom(Class.forName(className))*/) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static double getProgressPercentage(int is, int should) {
        BigDecimal fraction = new BigDecimal(is).divide(new BigDecimal(should), 6, RoundingMode.HALF_UP);
        return fraction.doubleValue();
    }

    @NotNull
    public static <T, E extends Throwable> T nvl(@Nullable T value, @NotNull ThrowableCallable<T, E> defaultValue) throws E {
        return value == null ? defaultValue.call() : value;
    }

    public static <T> T nvlf(@Nullable T value, T... values) {
        int index = 0;
        while (value == null && index < values.length) {
            value = values[index];
            index++;
        }
        return value;
    }

    @NotNull
    public static <T> T nvl(@Nullable T value, @NotNull T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static <T> T nvln(@Nullable T value, @Nullable T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static <T, E extends Throwable> T nvln(@Nullable T value, @NotNull ThrowableCallable<T, E> defaultValue) throws E {
        return value == null ? defaultValue.call() : value;
    }

    public static String nullIfEmpty(String string) {
        if (string != null) {
            string = string.trim();
            if (string.length() == 0) {
                string = null;
            }
        }
        return string;
    }

    public static Document loadXmlFile(Class clazz, String name) {
        InputStream inputStream = clazz.getResourceAsStream(name);
        return createXMLDocument(inputStream);
    }

    @Nullable
    public static Document createXMLDocument(InputStream inputStream) {
        try {
            return new DOMBuilder().build(new XML4JDOMAdapter().getDocument(inputStream, false));
        } catch (Exception e) {
            //LOGGER.warn(e);
        }
        return null;
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        try (Reader in = new InputStreamReader(inputStream)) {
            StringBuilder buffer = new StringBuilder();
            int i;
            while ((i = in.read()) != -1) buffer.append((char) i);
            in.close();
            return buffer.toString();
        }
    }

    public static <T> boolean isOneOf(T[] objects, T object) {
        for (T obj : objects) {
            if (obj == object) return true;
        }
        return false;
    }

    public static <T> int indexOf(T[] objects, T object) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == object) return i;
        }
        return -1;
    }

    @NotNull
    public static <T> T[] list(T... values) {
        return values;
    }

}
