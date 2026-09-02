#pragma once
#include <string>
#include "database/db_manager.h"

class RAGManager {
private:
    DbManager& db;

public:
    RAGManager(DbManager& database);

    std::string BuildContextualPrompt(const std::string& user_query, const std::string& subject_id);
};
