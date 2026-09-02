#pragma once
#include <thread>
#include <mutex>
#include <atomic>
#include <vector>
#include <string>
#include "ai_engine/rag_manager.h"

class AIManager {
private:
    RAGManager& rag;
    std::thread worker_thread;
    std::mutex chat_mutex;
    std::atomic<bool> is_generating{false};
    std::vector<std::string> chat_log;

    void InferenceWorker(std::string prompt);

public:
    AIManager(RAGManager& rag_mgr);
    ~AIManager();

    void AskQuestion(const std::string& prompt, const std::string& subject_id = "");
    std::vector<std::string> GetChatLog();
    bool IsGenerating() const;
    void ClearChat();
};
