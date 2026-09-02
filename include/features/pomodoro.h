#pragma once
#include <chrono>
#include <string>
#include "features/gamification.h"

class PomodoroEngine {
private:
    enum State { STOPPED, RUNNING, PAUSED };
    State current_state = STOPPED;
    
    int focus_minutes = 25;
    std::chrono::steady_clock::time_point start_time;
    std::chrono::seconds accumulated_time{0};
    std::chrono::seconds total_duration;

    GamificationEngine& gamification;

public:
    PomodoroEngine(GamificationEngine& g_engine);

    void Start();
    void Pause();
    void StopAndReward();
    void Reset();

    std::string GetTimeRemainingStr();
    void RenderUI();
};
