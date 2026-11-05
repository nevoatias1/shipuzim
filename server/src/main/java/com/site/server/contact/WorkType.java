package com.site.server.contact;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WorkType {
    CLADDING,
    PAINTING,
    PERGOLA_FIXED,
    PERGOLA_ELECTRIC,
    BALCONY_ENCLOSURE,
    BOTH,
    OTHER;

    @JsonCreator
    public static WorkType fromJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return WorkType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}