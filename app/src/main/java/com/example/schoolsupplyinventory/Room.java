package com.example.schoolsupplyinventory;

public enum Room {
    COMLAB_A("COMLAB-A"),
    COMLAB_B("COMLAB-B"),
    COMLAB_C("COMLAB-C"),
    ITE_OFFICE("ITE OFFICE");


    private String mDisplayName;

    Room(String displayName) {
        mDisplayName = displayName;
    }

    @Override
    public String toString() {
        return mDisplayName;
    }
}
