#include "features/gamification.h"
#include <imgui.h>

GamificationEngine::GamificationEngine(DbManager& database) : db(database) {
    profile = db.get_user_profile();
}

void GamificationEngine::AddXP(int amount) {
    profile.total_xp += amount;
    int new_level = (profile.total_xp / 100) + 1;
    if (new_level > profile.current_level) {
        profile.current_level = new_level;
    }
    db.update_user_profile(profile);
}

int GamificationEngine::GetXP() const {
    return profile.total_xp;
}

int GamificationEngine::GetLevel() const {
    return profile.current_level;
}

int GamificationEngine::GetStreak() const {
    return profile.current_streak;
}

std::string GamificationEngine::GetPetAvatar() const {
    if (profile.current_level >= 20) return "🦉 Wise Owl";
    if (profile.current_level >= 10) return "🦅 Soaring Eagle";
    if (profile.current_level >= 5)  return "🐥 Curious Chick";
    if (profile.current_level >= 2)  return "🐣 Hatching Scholar";
    return "🥚 Novice Learner";
}

void GamificationEngine::RenderProfileUI() {
    // Child panel
    ImGui::BeginChild("GamificationPanel", ImVec2(0, 105), true);
    ImGui::TextColored(ImVec4(0.6f, 0.4f, 0.9f, 1.0f), "Gamification Profile");
    ImGui::Separator();
    
    ImGui::Text("Virtual Pet Rank: %s", GetPetAvatar().c_str());
    ImGui::Text("Current Level: %d", profile.current_level);
    
    // Custom XP Progress Bar
    float xp_progress = (profile.total_xp % 100) / 100.0f;
    std::string bar_text = std::to_string(profile.total_xp % 100) + " / 100 XP";
    ImGui::ProgressBar(xp_progress, ImVec2(-1.0f, 0.0f), bar_text.c_str());
    
    ImGui::EndChild();
}
