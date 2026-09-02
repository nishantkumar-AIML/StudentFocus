#include "ai_engine/ai_manager.h"
#include <chrono>

AIManager::AIManager(RAGManager& rag_mgr) : rag(rag_mgr) {
    chat_log.push_back("AI: Hello! I am your offline tutor. How can I help you study today?");
}

AIManager::~AIManager() {
    // If worker thread is still running, let it finish or wait
    if (worker_thread.joinable()) {
        worker_thread.join();
    }
}

void AIManager::InferenceWorker(std::string prompt) {
    // 1. Thread lock to update chat state safely
    {
        std::lock_guard<std::mutex> lock(chat_mutex);
        chat_log.push_back("AI: [Thinking...]");
    }

    // 2. Simulate local SLM (Gemma) inference delay
    std::this_thread::sleep_for(std::chrono::milliseconds(1800));

    // Dynamic response generation based on prompt keywords to showcase RAG integration
    std::string response = "I've analyzed your context. ";
    if (prompt.find("Polymorphism") != std::string::npos || prompt.find("polymorphism") != std::string::npos) {
        response += "Your notes on Polymorphism indicate it allows C++ methods to execute dynamically based on the object instance. You should practice writing virtual functions to solidify this concept.";
    } else if (prompt.find("study") != std::string::npos || prompt.find("tutor") != std::string::npos) {
        response += "To study effectively, I recommend setting up a 25-minute Pomodoro focus timer block. We can log your study minutes directly to SQLite.";
    } else {
        response += "RAG pipeline executed successfully. I searched your local SQLite3 database. What subject would you like to review next?";
    }

    // 3. Update UI log safely
    {
        std::lock_guard<std::mutex> lock(chat_mutex);
        if (!chat_log.empty() && chat_log.back() == "AI: [Thinking...]") {
            chat_log.pop_back(); // Remove [Thinking...] indicator
        }
        chat_log.push_back("AI: " + response);
    }
    
    is_generating = false;
}

void AIManager::AskQuestion(const std::string& prompt, const std::string& subject_id) {
    if (is_generating) return; // Prevent multiple requests at once
    
    {
        std::lock_guard<std::mutex> lock(chat_mutex);
        chat_log.push_back("You: " + prompt);
    }

    is_generating = true;

    // Build the RAG contextual prompt
    std::string contextual_prompt = rag.BuildContextualPrompt(prompt, subject_id);

    // Launch background thread and detach so it executes concurrently with the GUI
    if (worker_thread.joinable()) {
        worker_thread.join(); // Clean up previous completed thread if any
    }
    worker_thread = std::thread(&AIManager::InferenceWorker, this, contextual_prompt);
}

std::vector<std::string> AIManager::GetChatLog() {
    std::lock_guard<std::mutex> lock(chat_mutex);
    return chat_log;
}

bool AIManager::IsGenerating() const {
    return is_generating;
}

void AIManager::ClearChat() {
    std::lock_guard<std::mutex> lock(chat_mutex);
    chat_log.clear();
    chat_log.push_back("AI: Chat log cleared. Ask me anything to get started!");
}
