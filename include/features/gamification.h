#pragma once
#include <string>
#include "database/db_manager.h"

class GamificationEngine {
private:
    DbManager& db;
    UserProfile profile;

public:
    GamificationEngine(DbManager& database);

    void AddXP(int amount);
    int GetXP() const;
    int GetLevel() const;
    int GetStreak() const;
    std::string GetPetAvatar() const;

    void RenderProfileUI();
};
