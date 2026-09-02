#pragma once
#include <string>
#include <vector>
#include <optional>
#include <memory>
#include "models/subject.h"
#include "models/task.h"
#include "models/ai_memory.h"
#include "models/note.h"
#include "models/user_profile.h"

class DbManager {
public:
    DbManager(const std::string& db_path);
    ~DbManager();

    // Subject CRUD
    void create_subject(const Subject& subject);
    std::optional<Subject> get_subject(const std::string& id);
    std::vector<Subject> get_all_subjects();
    void update_subject(const Subject& subject);
    void delete_subject(const std::string& id);

    // Task CRUD
    void create_task(const Task& task);
    std::optional<Task> get_task(const std::string& id);
    std::vector<Task> get_all_tasks();
    std::vector<Task> get_tasks_by_subject(const std::string& subject_id);
    void update_task(const Task& task);
    void delete_task(const std::string& id);

    // AiMemory CRUD
    int create_ai_memory(const AiMemory& memory); // returns the generated auto-increment id
    std::optional<AiMemory> get_ai_memory(int id);
    std::vector<AiMemory> get_all_ai_memories();
    void update_ai_memory(const AiMemory& memory);
    void delete_ai_memory(int id);

    // Note CRUD
    void create_note(const Note& note);
    std::optional<Note> get_note(const std::string& id);
    std::vector<Note> get_all_notes();
    std::vector<Note> get_notes_by_subject(const std::string& subject_id);
    void update_note(const Note& note);
    void delete_note(const std::string& id);

    // UserProfile CRUD (Singleton Row with id = 1)
    UserProfile get_user_profile();
    void update_user_profile(const UserProfile& profile);

private:
    struct Impl;
    std::unique_ptr<Impl> impl;
};
