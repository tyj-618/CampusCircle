package com.tyj.campuscircle.ai;

import java.util.List;

public record RetrievalQuery(
        String question,
        List<Long> allowedSchoolIds,
        int limit
) {

    public RetrievalQuery {
        question = question == null ? "" : question.trim();
        allowedSchoolIds = allowedSchoolIds == null ? List.of() : List.copyOf(allowedSchoolIds);
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
