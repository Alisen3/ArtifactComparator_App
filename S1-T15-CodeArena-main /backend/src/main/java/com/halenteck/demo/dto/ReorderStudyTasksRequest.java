package com.halenteck.demo.dto;

import java.util.List;

public record ReorderStudyTasksRequest(
        List<Long> orderedTaskIds
) {
}

