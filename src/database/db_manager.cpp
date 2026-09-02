#include "database/db_manager.h"
#include <sqlite_orm/sqlite_orm.h>

using namespace sqlite_orm;

// Helper to initialize and define the storage schema mapping
inline auto init_storage(const std::string& path) {
    return make_storage(path,
        make_table("subjects",
            make_column("id", &Subject::id, primary_key()),
            make_column("name", &Subject::name),
            make_column("credits", &Subject::credits),
            make_column("target_grade", &Subject::target_grade),
            make_column("is_active", &Subject::is_active)
        ),
        make_table("tasks",
            make_column("id", &Task::id, primary_key()),
            make_column("title", &Task::title),
            make_column("description", &Task::description),
            make_column("due_date", &Task::due_date),
            make_column("priority", &Task::priority),
            make_column("status", &Task::status),
            make_column("subject_id", &Task::subject_id),
            make_column("estimated_minutes", &Task::estimated_minutes),
            make_column("is_recurring", &Task::is_recurring),
            make_column("created_at", &Task::created_at),
            foreign_key(&Task::subject_id).references(&Subject::id).on_delete.cascade()
        ),
        make_table("ai_memories",
            make_column("id", &AiMemory::id, primary_key().autoincrement()),
            make_column("memory_type", &AiMemory::memory_type),
            make_column("ref_id", &AiMemory::ref_id),
            make_column("content_chunk", &AiMemory::content_chunk),
            make_column("timestamp", &AiMemory::timestamp)
        ),
        make_table("notes",
            make_column("id", &Note::id, primary_key()),
            make_column("title", &Note::title),
            make_column("content", &Note::content),
            make_column("subject_id", &Note::subject_id),
            make_column("created_at", &Note::created_at),
            foreign_key(&Note::subject_id).references(&Subject::id).on_delete.cascade()
        ),
        make_table("user_profiles",
            make_column("id", &UserProfile::id, primary_key()),
            make_column("total_xp", &UserProfile::total_xp),
            make_column("current_level", &UserProfile::current_level),
            make_column("current_streak", &UserProfile::current_streak)
        )
    );
}

// Extract the storage type
using StorageType = decltype(init_storage(""));

// The private implementation structure containing the sqlite_orm storage object
struct DbManager::Impl {
    StorageType storage;

    Impl(const std::string& db_path) : storage(init_storage(db_path)) {
        // Sync database schema with defined tables/columns
        storage.sync_schema();
    }
};

DbManager::DbManager(const std::string& db_path) : impl(std::make_unique<Impl>(db_path)) {}
DbManager::~DbManager() = default;

// --- Subject CRUD ---

void DbManager::create_subject(const Subject& subject) {
    impl->storage.replace(subject);
}

std::optional<Subject> DbManager::get_subject(const std::string& id) {
    if (auto ptr = impl->storage.get_pointer<Subject>(id)) {
        return *ptr;
    }
    return std::nullopt;
}

std::vector<Subject> DbManager::get_all_subjects() {
    return impl->storage.get_all<Subject>();
}

void DbManager::update_subject(const Subject& subject) {
    impl->storage.update(subject);
}

void DbManager::delete_subject(const std::string& id) {
    impl->storage.remove<Subject>(id);
}

// --- Task CRUD ---

void DbManager::create_task(const Task& task) {
    impl->storage.replace(task);
}

std::optional<Task> DbManager::get_task(const std::string& id) {
    if (auto ptr = impl->storage.get_pointer<Task>(id)) {
        return *ptr;
    }
    return std::nullopt;
}

std::vector<Task> DbManager::get_all_tasks() {
    return impl->storage.get_all<Task>();
}

std::vector<Task> DbManager::get_tasks_by_subject(const std::string& subject_id) {
    return impl->storage.get_all<Task>(where(c(&Task::subject_id) == subject_id));
}

void DbManager::update_task(const Task& task) {
    impl->storage.update(task);
}

void DbManager::delete_task(const std::string& id) {
    impl->storage.remove<Task>(id);
}

// --- AiMemory CRUD ---

int DbManager::create_ai_memory(const AiMemory& memory) {
    return impl->storage.insert(memory);
}

std::optional<AiMemory> DbManager::get_ai_memory(int id) {
    if (auto ptr = impl->storage.get_pointer<AiMemory>(id)) {
        return *ptr;
    }
    return std::nullopt;
}

std::vector<AiMemory> DbManager::get_all_ai_memories() {
    return impl->storage.get_all<AiMemory>();
}

void DbManager::update_ai_memory(const AiMemory& memory) {
    impl->storage.update(memory);
}

void DbManager::delete_ai_memory(int id) {
    impl->storage.remove<AiMemory>(id);
}

// --- Note CRUD ---

void DbManager::create_note(const Note& note) {
    impl->storage.replace(note);
}

std::optional<Note> DbManager::get_note(const std::string& id) {
    if (auto ptr = impl->storage.get_pointer<Note>(id)) {
        return *ptr;
    }
    return std::nullopt;
}

std::vector<Note> DbManager::get_all_notes() {
    return impl->storage.get_all<Note>();
}

std::vector<Note> DbManager::get_notes_by_subject(const std::string& subject_id) {
    return impl->storage.get_all<Note>(where(c(&Note::subject_id) == subject_id));
}

void DbManager::update_note(const Note& note) {
    impl->storage.update(note);
}

void DbManager::delete_note(const std::string& id) {
    impl->storage.remove<Note>(id);
}

// --- UserProfile CRUD ---

UserProfile DbManager::get_user_profile() {
    if (auto ptr = impl->storage.get_pointer<UserProfile>(1)) {
        return *ptr;
    }
    // If user profile is not found in database, insert and return a default one
    UserProfile default_profile{1, 0, 1, 0};
    impl->storage.replace(default_profile);
    return default_profile;
}

void DbManager::update_user_profile(const UserProfile& profile) {
    impl->storage.replace(profile);
}
