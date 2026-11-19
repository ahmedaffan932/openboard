package org.dslul.openboard;

import java.util.ArrayList;
import java.util.List;

public class ObservableBool {
    private static boolean myBoolean = false;
    private static final List<BooleanChangeListener> listeners = new ArrayList<>();

    public static void addBooleanChangeListener(BooleanChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeBooleanChangeListener(BooleanChangeListener listener) {
        listeners.remove(listener);
    }

    public static void setMyBoolean(boolean value) {
        if (myBoolean != value) {
            myBoolean = value;
            for (BooleanChangeListener listener : listeners) {
                listener.onBooleanChanged(value);
            }
        }
    }
    public static Boolean getMyBoolean() {
        return myBoolean;
    }
}