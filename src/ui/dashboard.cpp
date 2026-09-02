#include "ui/dashboard.h"
#include <imgui.h>
#include <vector>
#include <ctime>

DashboardUI::DashboardUI(const std::string& db_path) 
    : db(db_path),
      gamification(db),
      pomodoro(gamification),
      rag(db),
      ai(rag) 
{
    // Initialize caches
    current_subject_id = "";
    task_title_buf[0] = '\0';
    note_title_buf[0] = '\0';
    note_content_buf[0] = '\0';
    chat_input_buf[0] = '\0';

    // Insert dummy subjects if DB is empty to make UI usable out of the box
    if (db.get_all_subjects().empty()) {
        db.create_subject(Subject{"MATH101", "Calculus I", 4, "A+", true});
        db.create_subject(Subject{"CS101", "Programming in C++", 4, "A", true});
        db.create_subject(Subject{"AI202", "Artificial Intelligence", 3, "A", true});
    }
}

DashboardUI::~DashboardUI() = default;

void DashboardUI::Render() {
    // Force immediate mode window to cover whole screen viewport
    ImGui::SetNextWindowPos(ImVec2(0, 0));
    ImGui::SetNextWindowSize(ImGui::GetIO().DisplaySize);
    
    ImGui::Begin("Student Focus OS", nullptr, 
        ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove | ImGuiWindowFlags_NoResize);

    // Welcome Header
    ImGui::TextColored(ImVec4(0.38f, 0.51f, 0.96f, 1.0f), "Welcome back, Student!");
    ImGui::SameLine();
    ImGui::Text(" | Current Focus Subject: ");
    ImGui::SameLine();
    
    // Simple Subject Selection Combo Box
    auto subjects = db.get_all_subjects();
    std::string preview = current_subject_id.empty() ? "General study" : current_subject_id;
    ImGui::PushItemWidth(200.0f);
    if (ImGui::BeginCombo("##SubjectCombo", preview.c_str(), ImGuiComboFlags_HeightSmall)) {
        if (ImGui::Selectable("General study", current_subject_id.empty())) {
            current_subject_id = "";
        }
        for (const auto& sub : subjects) {
            bool is_selected = (current_subject_id == sub.id);
            if (ImGui::Selectable(sub.name.c_str(), is_selected)) {
                current_subject_id = sub.id;
            }
            if (is_selected) {
                ImGui::SetItemDefaultFocus();
            }
        }
        ImGui::EndCombo();
    }
    ImGui::PopItemWidth();
    ImGui::Separator();

    // 2-Column Split
    ImGui::Columns(2, "MainColumns", true);

    RenderLeftColumn();
    
    ImGui::NextColumn();

    RenderRightColumn();

    ImGui::End();
}

void DashboardUI::RenderLeftColumn() {
    // 1. Gamification Widget
    gamification.RenderProfileUI();
    ImGui::Spacing();

    // 2. Pomodoro Widget
    pomodoro.RenderUI();
    ImGui::Spacing();

    // 3. Task Management Widget
    ImGui::BeginChild("TasksWidget", ImVec2(0, 0), true);
    ImGui::TextColored(ImVec4(0.23f, 0.51f, 0.96f, 1.0f), "Study Tasks & Goals");
    ImGui::Separator();

    // Display tasks matching current selection
    std::vector<Task> tasks;
    if (current_subject_id.empty()) {
        tasks = db.get_all_tasks();
    } else {
        tasks = db.get_tasks_by_subject(current_subject_id);
    }

    ImGui::BeginChild("TasksListScroll", ImVec2(0, -50), false, ImGuiWindowFlags_AlwaysVerticalScrollbar);
    if (tasks.empty()) {
        ImGui::TextDisabled("No active tasks found. Create one below!");
    } else {
        for (const auto& task : tasks) {
            bool is_done = (task.status == "DONE");
            std::string label = task.title + (task.subject_id ? " [" + *task.subject_id + "]" : "");
            
            if (ImGui::Checkbox(label.c_str(), &is_done)) {
                Task updated = task;
                updated.status = is_done ? "DONE" : "TODO";
                db.update_task(updated);
            }
            
            ImGui::SameLine(ImGui::GetWindowWidth() - 75);
            if (ImGui::Button(("Delete##" + task.id).c_str())) {
                db.delete_task(task.id);
            }
        }
    }
    ImGui::EndChild();

    ImGui::Separator();
    
    // Add New Task Input
    ImGui::PushItemWidth(ImGui::GetWindowWidth() - 120);
    ImGui::InputText("##NewTaskTitle", task_title_buf, IM_ARRAYSIZE(task_title_buf));
    ImGui::PopItemWidth();
    ImGui::SameLine();
    if (ImGui::Button("Add Task", ImVec2(-FLT_MIN, 0)) && task_title_buf[0] != '\0') {
        std::string new_id = "TASK_" + std::to_string(std::time(nullptr));
        std::optional<std::string> sub_id = current_subject_id.empty() ? std::nullopt : std::make_optional(current_subject_id);
        
        Task newTask{
            new_id,
            task_title_buf,
            std::nullopt,
            std::time(nullptr),
            1, // Medium priority
            "TODO",
            sub_id,
            30,
            false,
            std::time(nullptr)
        };
        db.create_task(newTask);
        task_title_buf[0] = '\0'; // clear buffer
    }

    ImGui::EndChild();
}

void DashboardUI::RenderRightColumn() {
    // 1. Thread-safe AI Chatbox
    ImGui::BeginChild("AIChatWidget", ImVec2(0, ImGui::GetWindowHeight() * 0.44f), true);
    ImGui::TextColored(ImVec4(0.23f, 0.51f, 0.96f, 1.0f), "Offline AI Tutor (Gemma)");
    ImGui::Separator();

    // Render Chat log
    ImGui::BeginChild("ChatLogsScroll", ImVec2(0, -40), false, ImGuiWindowFlags_AlwaysVerticalScrollbar);
    auto logs = ai.GetChatLog();
    for (const auto& msg : logs) {
        ImGui::TextWrapped("%s", msg.c_str());
    }
    ImGui::EndChild();

    // Input line
    bool is_generating_currently = ai.IsGenerating();
    if (is_generating_currently) {
        ImGui::BeginDisabled();
    }
    
    ImGui::PushItemWidth(ImGui::GetWindowWidth() - 90);
    bool chat_submitted = ImGui::InputText("##ChatInput", chat_input_buf, IM_ARRAYSIZE(chat_input_buf), ImGuiInputTextFlags_EnterReturnsTrue);
    ImGui::PopItemWidth();
    ImGui::SameLine();
    if ((chat_submitted || ImGui::Button("Send")) && chat_input_buf[0] != '\0') {
        ai.AskQuestion(chat_input_buf, current_subject_id);
        chat_input_buf[0] = '\0';
    }
    
    if (is_generating_currently) {
        ImGui::EndDisabled();
    }

    ImGui::EndChild();
    ImGui::Spacing();

    // 2. Class Notes Editor
    ImGui::BeginChild("NotesWidget", ImVec2(0, 0), true);
    ImGui::TextColored(ImVec4(0.23f, 0.51f, 0.96f, 1.0f), "Study Notes & Flashcard Generator");
    ImGui::Separator();

    ImGui::InputText("Note Title", note_title_buf, IM_ARRAYSIZE(note_title_buf));
    
    ImGui::InputTextMultiline("##NoteContent", note_content_buf, IM_ARRAYSIZE(note_content_buf),
        ImVec2(-FLT_MIN, ImGui::GetContentRegionAvail().y - 45));

    if (ImGui::Button("Save Note", ImVec2(90, 30)) && note_title_buf[0] != '\0') {
        std::string note_id = "NOTE_" + std::to_string(std::time(nullptr));
        Note newNote{
            note_id,
            note_title_buf,
            note_content_buf,
            current_subject_id.empty() ? "CS101" : current_subject_id, 
            std::time(nullptr)
        };
        db.create_note(newNote);
        note_title_buf[0] = '\0';
        note_content_buf[0] = '\0';
    }
    
    ImGui::SameLine();
    if (ImGui::Button("Generate Flashcards", ImVec2(160, 30)) && note_content_buf[0] != '\0') {
        std::string query = "Analyze this note and generate spaced repetition flashcards:\n" + std::string(note_content_buf);
        ai.AskQuestion(query, current_subject_id);
    }
    
    ImGui::SameLine();
    if (ImGui::Button("Clear Chat", ImVec2(90, 30))) {
        ai.ClearChat();
    }

    ImGui::EndChild();
}
