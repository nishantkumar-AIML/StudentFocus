#pragma once

// UserProfile Entity representing user gamification states (singleton row)
struct UserProfile {
    int id; // Fixed to 1 for singleton storage
    int total_xp;
    int current_level;
    int current_streak;
};
