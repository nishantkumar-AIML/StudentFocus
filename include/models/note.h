#pragma once
#include <string>
#include <cstdint>

// Note Entity representing study notes
struct Note {
    std::string id;
    std::string title;
    std::string content;
    std::string subject_id; // Foreign key referencing Subject::id
    int64_t created_at;
};
