#pragma once
#include "database/db_manager.h"
#include "features/gamification.h"
#include "features/pomodoro.h"
#include "ai_engine/rag_manager.h"
#include "ai_engine/ai_manager.h"

class DashboardUI {
private:
    DbManager db;
    GamificationEngine gamification;
    PomodoroEngine pomodoro;
    RAGManager rag;
    AIManager ai;

    // UI state caching
    std::string current_subject_id;
    char task_title_buf[128];
    char note_content_buf[4096];
    char note_title_buf[128];
    char chat_input_buf[256];

    void RenderLeftColumn();
    void RenderRightColumn();

public:
    DashboardUI(const std::string& db_path);
    ~DashboardUI();

    void Render();
};
