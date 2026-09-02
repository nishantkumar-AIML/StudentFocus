#pragma once
#include <string>
#include <optional>
#include <cstdint>

// Task Entity (Kanban & Gamification)
struct Task {
    std::string id;
    std::string title;
    std::optional<std::string> description; // C++17 feature for nullable fields
    int64_t due_date;                       // Unix timestamp
    int priority;                           // 0: Low, 1: Med, 2: High
    std::string status;                     // "TODO", "IN_PROGRESS", "DONE"
    std::optional<std::string> subject_id;  // Foreign key references Subject::id
    int estimated_minutes;
    bool is_recurring;
    int64_t created_at;
};
