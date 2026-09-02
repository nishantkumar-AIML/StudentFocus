#include "features/pomodoro.h"
#include <iomanip>
#include <sstream>
#include <imgui.h>

PomodoroEngine::PomodoroEngine(GamificationEngine& g_engine) : gamification(g_engine) {
    total_duration = std::chrono::minutes(focus_minutes);
}

void PomodoroEngine::Start() {
    if (current_state == RUNNING) return;
    start_time = std::chrono::steady_clock::now();
    current_state = RUNNING;
}

void PomodoroEngine::Pause() {
    if (current_state != RUNNING) return;
    auto now = std::chrono::steady_clock::now();
    accumulated_time += std::chrono::duration_cast<std::chrono::seconds>(now - start_time);
    current_state = PAUSED;
}

void PomodoroEngine::StopAndReward() {
    if (current_state == STOPPED) return;
    
    auto now = std::chrono::steady_clock::now();
    std::chrono::seconds elapsed = accumulated_time;
    if (current_state == RUNNING) {
        elapsed += std::chrono::duration_cast<std::chrono::seconds>(now - start_time);
    }

    int minutes_studied = elapsed.count() / 60;
    
    // Reward 1 XP per minute studied!
    if (minutes_studied > 0) {
        gamification.AddXP(minutes_studied);
    }

    Reset();
}

void PomodoroEngine::Reset() {
    current_state = STOPPED;
    accumulated_time = std::chrono::seconds(0);
    total_duration = std::chrono::minutes(focus_minutes);
}

std::string PomodoroEngine::GetTimeRemainingStr() {
    if (current_state == STOPPED) {
        return std::to_string(focus_minutes) + ":00";
    }

    auto now = std::chrono::steady_clock::now();
    std::chrono::seconds elapsed = accumulated_time;
    if (current_state == RUNNING) {
        elapsed += std::chrono::duration_cast<std::chrono::seconds>(now - start_time);
    }

    auto remaining = total_duration - elapsed;

    if (remaining.count() <= 0) {
        // Timer finished! Reward for the full focus duration
        gamification.AddXP(focus_minutes);
        Reset();
        return "00:00";
    }

    int mins = remaining.count() / 60;
    int secs = remaining.count() % 60;

    std::stringstream ss;
    ss << std::setfill('0') << std::setw(2) << mins << ":"
       << std::setfill('0') << std::setw(2) << secs;
    return ss.str();
}

void PomodoroEngine::RenderUI() {
    ImGui::BeginChild("PomodoroPanel", ImVec2(0, 115), true);
    ImGui::TextColored(ImVec4(0.9f, 0.4f, 0.4f, 1.0f), "Focus Timer");
    ImGui::Separator();
    
    // Large display using scale
    ImGui::SetWindowFontScale(2.0f);
    ImGui::Text("%s", GetTimeRemainingStr().c_str());
    ImGui::SetWindowFontScale(1.0f); // Reset scale

    ImGui::SameLine(ImGui::GetWindowWidth() - 250);

    if (current_state == STOPPED || current_state == PAUSED) {
        if (ImGui::Button("Start", ImVec2(70, 30))) Start();
    } else {
        if (ImGui::Button("Pause", ImVec2(70, 30))) Pause();
    }
    
    ImGui::SameLine();
    
    if (ImGui::Button("Stop & Log", ImVec2(100, 30))) {
        StopAndReward();
    }
    
    ImGui::SameLine();
    
    if (ImGui::Button("Reset", ImVec2(60, 30))) {
        Reset();
    }

    ImGui::EndChild();
}
