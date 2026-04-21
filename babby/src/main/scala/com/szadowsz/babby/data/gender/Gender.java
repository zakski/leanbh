package com.szadowsz.babby.data.gender;

/**
 * Created on 28/10/2016.
 */
public enum Gender {

    BOY("m","boy","male","masculine"), GIRL("f","girl","female","f","feminine"), BOTH("","both","either");


    final String[] synonyms;

    Gender(String... syns) {
        synonyms = syns;
    }

    @Override
    public String toString() {
        return synonyms[0];
    }
}
