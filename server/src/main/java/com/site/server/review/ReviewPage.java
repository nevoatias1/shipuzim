package com.site.server.review;

import java.util.List;

public record ReviewPage<T>(List<T> items, int page, int size, long totalElements, int totalPages,
                            double average, long count) {
    @Override public String toString() { return "ReviewPage[redacted]"; }
}
