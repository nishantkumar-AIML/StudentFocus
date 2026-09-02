#pragma once
#include <string>
#include <optional>
#include <cstdint>

// AI Memory (Local RAG Context)
struct AiMemory {
    int id;                                 // Primary Key auto-increment
    std::string memory_type;                // e.g., "WEAK_SUBJECT", "PDF_SUMMARY"
    std::optional<std::string> ref_id;      
    std::string content_chunk;              // Text chunk fed to Gemma
    int64_t timestamp;
};
