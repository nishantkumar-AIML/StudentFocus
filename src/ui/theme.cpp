#include "ui/theme.h"
#include <imgui.h>

void ThemeManager::ApplyGlassmorphismTheme() {
    ImGuiStyle& style = ImGui::GetStyle();
    ImVec4* colors = style.Colors;

    // --- Geometry (Rounded like Tailwind "rounded-2xl") ---
    style.WindowRounding    = 12.0f;
    style.ChildRounding     = 8.0f;
    style.FrameRounding     = 6.0f;
    style.PopupRounding     = 8.0f;
    style.ScrollbarRounding = 4.0f;
    style.GrabRounding      = 4.0f;
    
    style.WindowBorderSize  = 1.0f;
    style.FrameBorderSize   = 1.0f;

    // --- Colors (Based on your Tailwind mockup) ---
    // Backgrounds: Slate 900 / Slate 800 with Transparency for "Glass" effect
    colors[ImGuiCol_WindowBg]       = ImVec4(0.06f, 0.09f, 0.16f, 0.85f); // var(--color-bg)
    colors[ImGuiCol_ChildBg]        = ImVec4(0.12f, 0.16f, 0.23f, 0.50f); // var(--glass-bg)
    colors[ImGuiCol_PopupBg]        = ImVec4(0.06f, 0.09f, 0.16f, 0.95f);
    
    // Borders: Subtle white lines
    colors[ImGuiCol_Border]         = ImVec4(1.00f, 1.00f, 1.00f, 0.10f); // var(--glass-border)
    colors[ImGuiCol_BorderShadow]   = ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
    
    // Frames (Inputs, Checkboxes)
    colors[ImGuiCol_FrameBg]        = ImVec4(0.12f, 0.16f, 0.23f, 0.60f);
    colors[ImGuiCol_FrameBgHovered] = ImVec4(0.23f, 0.51f, 0.96f, 0.40f); // Accent hover
    colors[ImGuiCol_FrameBgActive]  = ImVec4(0.23f, 0.51f, 0.96f, 0.60f);
    
    // Accents (Indigo 500 / Blue 500)
    colors[ImGuiCol_Button]         = ImVec4(0.23f, 0.51f, 0.96f, 0.80f); // var(--color-accent)
    colors[ImGuiCol_ButtonHovered]  = ImVec4(0.23f, 0.51f, 0.96f, 1.00f);
    colors[ImGuiCol_ButtonActive]   = ImVec4(0.15f, 0.35f, 0.80f, 1.00f);
    colors[ImGuiCol_CheckMark]      = ImVec4(0.23f, 0.51f, 0.96f, 1.00f);
    
    // Text
    colors[ImGuiCol_Text]           = ImVec4(0.97f, 0.98f, 0.99f, 1.00f);
    colors[ImGuiCol_TextDisabled]   = ImVec4(0.60f, 0.65f, 0.71f, 1.00f);
    
    // Headers (Collapsing headers, Selections)
    colors[ImGuiCol_Header]         = ImVec4(0.23f, 0.51f, 0.96f, 0.31f);
    colors[ImGuiCol_HeaderHovered]  = ImVec4(0.23f, 0.51f, 0.96f, 0.80f);
    colors[ImGuiCol_HeaderActive]   = ImVec4(0.23f, 0.51f, 0.96f, 1.00f);
}
