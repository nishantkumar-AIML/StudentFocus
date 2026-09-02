#pragma once
#include <string>

// Subject Entity representing a school subject / course
struct Subject {
    std::string id;
    std::string name;
    int credits;
    std::string target_grade; 
    bool is_active;
};
