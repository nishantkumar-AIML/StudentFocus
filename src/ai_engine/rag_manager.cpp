#include "ai_engine/rag_manager.h"
#include <sstream>

RAGManager::RAGManager(DbManager& database) : db(database) {}

std::string RAGManager::BuildContextualPrompt(const std::string& user_query, const std::string& subject_id) {
    // 1. System Prompt (AI Persona)
    std::string prompt = "<start_of_turn>user\nYou are an expert academic tutor for an engineering student. ";
    prompt += "Answer the question strictly based on the provided context.\n\n";

    // 2. Fetch Relevant Context from Database (Notes & Tasks)
    std::stringstream context_stream;
    
    if (!subject_id.empty()) {
        auto notes = db.get_notes_by_subject(subject_id);
        if (!notes.empty()) {
            context_stream << "=== CONTEXT (STUDENT'S NOTES) ===\n";
            for (const auto& note : notes) {
                context_stream << "- Title: " << note.title << "\n";
                context_stream << "  Content: " << note.content << "\n\n";
            }
        }
        
        auto tasks = db.get_tasks_by_subject(subject_id);
        if (!tasks.empty()) {
            context_stream << "=== CONTEXT (STUDENT'S TASKS) ===\n";
            for (const auto& task : tasks) {
                context_stream << "- Task: " << task.title << " (Status: " << task.status << ")\n";
            }
            context_stream << "\n";
        }
    } else {
        // Fetch general notes if no subject specified
        auto notes = db.get_all_notes();
        if (!notes.empty()) {
            context_stream << "=== CONTEXT (STUDENT'S NOTES) ===\n";
            for (const auto& note : notes) {
                context_stream << "- Title: " << note.title << "\n";
                context_stream << "  Content: " << note.content << "\n\n";
            }
        }
    }

    std::string context = context_stream.str();
    if (!context.empty()) {
        prompt += context + "=================================\n\n";
    }

    // 3. Append User Query
    prompt += "Question: " + user_query + "<end_of_turn>\n<start_of_turn>model\n";

    return prompt;
}
