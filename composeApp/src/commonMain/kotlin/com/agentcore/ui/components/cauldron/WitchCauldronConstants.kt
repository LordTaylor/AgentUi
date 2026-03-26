// All numeric constants and theme-aware colour palettes for WitchCauldron.
// Centralised here so each draw file stays focused on logic, not magic numbers.
package com.agentcore.ui.components.cauldron

import androidx.compose.ui.graphics.Color

internal object WitchCauldronConstants {
    // === Grid & Scale ===
    const val GRID_SIZE_DEFAULT: Int = 128
    const val GRID_SIZE_NORMALIZED: Float = 128f
    const val SCALE_MIN = 0.1f

    // === Animation Durations (ms) ===
    const val ANIM_FIRE_FRAME_DURATION = 500           // faster flicker
    const val ANIM_FIRE_TIME_DURATION = 3500
    const val ANIM_BOUNCE_DURATION = 340               // snappier bounce
    const val ANIM_BUBBLE_PROGRESS_DURATION = 12000
    const val ANIM_INGREDIENT_PROGRESS_DURATION = 3000
    const val ANIM_PULSE_ALPHA_DURATION = 1200
    const val ANIM_SPOON_DURATION = 2000
    const val IDLE_BREATH_DURATION = 1800              // gentle breathing squash cycle

    // === Animation Values ===
    const val FIRE_FRAME_MAX = 7
    const val BOUNCE_OFFSET_THINKING = 13f             // bigger bounce → more squash
    const val PULSE_ALPHA_MIN = 0.4f
    const val PULSE_ALPHA_MAX = 0.9f

    // === Squash & Stretch ===
    const val SQUASH_MAX = 0.24f         // max squash factor at apex of bounce
    const val IDLE_SQUASH_AMOUNT = 0.04f // subtle idle breathing squash amplitude

    // === Fire Height (bounce-driven) ===
    // Fire is tiny at rest; shoots up to full height at bounce apex.
    const val FIRE_HEIGHT_BASE = 0.18f   // minimum fire when cauldron is not bouncing

    // === Fire Position (vertical, as fraction of gridSize; higher value = lower on screen) ===
    const val FIRE_BACK_VERT_POS  = 0.86f  // back fire layer (drawn behind cauldron body)
    const val FIRE_FRONT_VERT_POS = 0.90f  // front fire layer (drawn in front of cauldron body)

    // === Fire Sway (horizontal oscillation of the two fire offsets) ===
    const val FIRE_SWAY_AMPLITUDE = 8f     // max pixel sway of each fire layer
    const val FIRE_SWAY_FREQ_1    = 2.0    // sway frequency multiplier for back fire
    const val FIRE_SWAY_FREQ_2    = 2.5    // sway frequency multiplier for front fire

    // === Liquid Position ===
    // liquidY = gridSize/2 - LIQUID_Y_OFFSET_MULT * scale + bounceY
    // Increase to move liquid (and bubbles/steam) lower inside the cauldron.
    const val LIQUID_Y_OFFSET_MULT = 24f

    // === Bubble Density by State ===
    const val BUBBLE_DENSITY_IDLE = 4
    const val BUBBLE_DENSITY_SENDING = 8
    const val BUBBLE_DENSITY_RECEIVING = 12
    const val BUBBLE_DENSITY_THINKING = 8
    const val BUBBLE_DENSITY_LOADING = 16

    // === Fire Rendering (cartoon upgrade) ===
    const val FIRE_FRAME_COUNT = 4
    const val FIRE_HALF_WIDTH_MULT = 48
    const val FIRE_INTENSITY_EXPONENT = 2.5f           // was 1.2f — sharper pointed cartoon tips
    const val FIRE_NOISE_FREQ_1 = 12.0
    const val FIRE_NOISE_DX_FACTOR_1 = 0.3
    const val FIRE_NOISE_AMPLITUDE_1 = 0.45f
    const val FIRE_NOISE_FREQ_2 = 5.0
    const val FIRE_NOISE_DX_FACTOR_2 = 0.5
    const val FIRE_NOISE_AMPLITUDE_2 = 0.35f
    const val FIRE_NOISE_DX_FACTOR_3 = 0.8
    const val FIRE_NOISE_AMPLITUDE_3 = 0.3f
    const val FIRE_LICK_FREQ = 6.0
    const val FIRE_LICK_DY_FACTOR = 0.25
    const val FIRE_LICK_DX_FACTOR = 0.12
    const val FIRE_LICK_AMPLITUDE = 3.2                // was 2.5 — more tongue sway
    const val FIRE_FICKER_MULTIPLIER = 2.5f            // was 2f — more flicker
    const val FIRE_BASE_INTENSITY_OFFSET = 0.25f
    const val FIRE_NOISE_HEIGHT_OFFSET = 1.2f

    // 5-layer cartoon fire (outer/tallest → inner/hottest)
    const val FIRE_LAYER_1_ALPHA = 0.7f                // deep red outer silhouette
    const val FIRE_LAYER_1_HEIGHT_BASE = 22f           // was 14 — taller outer flame
    const val FIRE_LAYER_1_HEIGHT_MULT = 1.4f
    const val FIRE_LAYER_2_HEIGHT_BASE = 15f           // was 10
    const val FIRE_LAYER_2_HEIGHT_MULT = 1.1f
    const val FIRE_LAYER_3_HEIGHT_BASE = 9f            // was 6
    const val FIRE_LAYER_3_HEIGHT_MULT = 0.95f
    const val FIRE_LAYER_4_HEIGHT_BASE = 5f            // yellow inner
    const val FIRE_LAYER_4_HEIGHT_MULT = 0.85f
    const val FIRE_LAYER_5_HEIGHT_BASE = 2.5f          // white-hot core
    const val FIRE_LAYER_5_HEIGHT_MULT = 0.75f

    // === Cauldron Geometry ===
    const val CAULDRON_CENTER_Y_OFFSET = 8
    const val LEG_WIDTH_MULT = 10
    const val LEG_WIDTH_MIN = 3
    const val LEG_POS_X_MULT = 22
    const val LEG_Y_START_MULT = 26
    const val LEG_Y_END_MULT = 42
    // Cute feet additions
    const val LEG_FOOT_HEIGHT_MULT = 5    // rows at bottom that form the "foot"
    const val LEG_FOOT_EXTRA_HALF_WIDTH = 3  // extra half-width of foot cap vs leg body
    const val LEG_SPLAY_PIXELS = 3f       // total outward splay over full leg height

    const val CAULDRON_RADIUS_MULT = 42
    const val CAULDRON_DIY_MIN_MULT = -26
    const val CAULDRON_DIY_MAX_MULT = 34
    const val CAULDRON_DX_MIN_MULT = -46
    const val CAULDRON_DX_MAX_MULT = 46
    const val CAULDRON_ELLIPSE_Y_SCALE = 0.95f
    const val CAULDRON_RIM_DX_MIN_MULT = -42
    const val CAULDRON_RIM_DX_MAX_MULT = 42
    const val CAULDRON_RIM_DY_MIN_MULT = -30
    const val CAULDRON_RIM_DY_MAX_MULT = -22
    const val CAULDRON_REFLECT_SIZE_MULT = 6
    const val CAULDRON_REFLECT_DX_OFFSET = -22
    const val CAULDRON_REFLECT_DY_OFFSET = -10
    const val CAULDRON_REFLECT_ALPHA = 0.2f
    const val CAULDRON_OUTLINE_EXTRA = 2f
    const val HANDLE_OFFSET_X_MULT = 44
    const val HANDLE_OFFSET_Y_MULT = -26
    const val HANDLE_RADIUS = 5f
    const val HIGHLIGHT_DX = -24
    const val HIGHLIGHT_DY = -10
    const val HIGHLIGHT_W = 3
    const val HIGHLIGHT_H = 5
    val RIVET_POSITIONS = listOf(Pair(-20, 5), Pair(0, 15), Pair(20, 5))

    // === Liquid Rendering ===
    const val LIQUID_Y_MULT = 24
    const val LIQUID_WIDTH_MULT = 36
    const val LIQUID_HORIZ_SWAY_FREQ = 1.5
    const val LIQUID_HORIZ_SWAY_AMPLITUDE = 3
    const val LIQUID_SLOSH_INTENSITY_FACTOR = 0.8f
    const val LIQUID_WAVE_FREQ = 6.0
    const val LIQUID_WAVE_DX_FACTOR = 0.2
    const val LIQUID_WAVE_AMPLITUDE_BASE = 6
    const val LIQUID_CENTER_JUMP_THRESHOLD = 3
    const val LIQUID_CENTER_JUMP_MULTIPLIER = 1.8f
    const val LIQUID_EDGE_FACTOR_EXPONENT = 0.6f
    const val LIQUID_DEPTH_MULT = 8
    const val LIQUID_SURFACE_ALPHA = 0.9f

    // === Bubble Rendering ===
    const val BUBBLE_SPREAD_MULT = 28
    const val BUBBLE_RISE_HEIGHT_MULT = 55
    const val BUBBLE_ALPHA_EXPONENT = 0.6f
    const val BUBBLE_COLOR_BRIGHTNESS_FACTOR = 0.9f
    const val BUBBLE_COLOR_BASE_FACTOR = 0.3f
    const val BUBBLE_RADIUS = 3
    const val BUBBLE_MAX_DIST_SQ = 10
    const val BUBBLE_EDGE_DIST_SQ = 8
    const val BUBBLE_EDGE_ALPHA_FACTOR = 0.5f
    const val BUBBLE_HIGHLIGHT_ALPHA_FACTOR = 0.6f

    // === Ingredient Rendering ===
    const val INGREDIENT_COUNT = 3
    const val INGREDIENT_FREQ_BASE = 2.0
    const val INGREDIENT_FREQ_SPEED = 5.0
    const val INGREDIENT_X_SPREAD_MULT = 20
    const val INGREDIENT_SIZE_HALF = 1
    const val INGREDIENT_SIZE_OFFSET = 5

    // === Power Stream Rendering ===
    const val POWER_STREAM_WIDTH = 52
    const val POWER_STREAM_ALPHA_EXPONENT = 1.5f
    const val POWER_STREAM_CORE_WIDTH = 2

    // === Steam/Smoke Rendering ===
    const val STEAM_NUM_BASE = 2
    const val STEAM_NUM_INTENSITY_MULT = 2
    const val STEAM_X_SPREAD_MULT = 22
    const val STEAM_TIME_OFFSET_PER_STREAM = 1.8f
    const val STEAM_PARTICLE_COUNT_BASE = 15
    const val STEAM_PARTICLE_COUNT_INTENSITY_MULT = 8
    const val STEAM_ANIMATION_SPEED = 0.4f
    const val STEAM_PARTICLE_SPACING = 0.12f
    const val STEAM_RISE_HEIGHT_MULT = 50
    const val STEAM_START_OFFSET_FROM_LIQUID = 4
    const val STEAM_SWAY_AMPLITUDE = 12
    const val STEAM_DRIFT_AMPLITUDE = 8
    const val STEAM_DRIFT_SPEED = 0.2f
    const val STEAM_SIZE_BASE_MULT = 6
    const val STEAM_SIZE_GROWTH_MULT = 10
    const val STEAM_ALPHA_MAX = 0.4f

    // === Spoon ===
    const val SPOON_HANDLE_LENGTH = 18
    const val SPOON_BOWL_RADIUS = 3
    const val SPOON_BOWL_RADIUS_SQ = SPOON_BOWL_RADIUS * SPOON_BOWL_RADIUS

    // === Preview Dimensions ===
    const val PREVIEW_PADDING_DP = 16
    const val PREVIEW_ITEM_SIZE_DP = 100
    const val PREVIEW_ITEM_PADDING_DP = 8
    const val PREVIEW_LABEL_FONT_SIZE_SP = 10
}

/**
 * Theme-aware colour palette for the cauldron body, rim, and spoon.
 * Use [CauldronBodyPalette.DARK] on dark backgrounds, [LIGHT] on light ones.
 */
data class CauldronBodyPalette(
    val body: Color,
    val outline: Color,
    val rim: Color,
    val highlight: Color,
    val rivet: Color,
    val spoonHandle: Color,
    val spoonBowl: Color,
    val spoonBowlOutline: Color,
) {
    companion object {
        /** Steel-blue metallic cauldron — clearly visible on dark UI backgrounds. */
        val DARK = CauldronBodyPalette(
            body             = Color(0xFF3A5270),
            outline          = Color(0xFF7BAFD4),
            rim              = Color(0xFF9ECAE1),
            highlight        = Color(0xFF5A8EC0),
            rivet            = Color(0xFFAAD0F0),
            spoonHandle      = Color(0xFFE8C060),
            spoonBowl        = Color(0xFFD8D8D8),
            spoonBowlOutline = Color(0xFF7BAFD4),
        )
        /** Charcoal cauldron — clearly visible on light UI backgrounds. */
        val LIGHT = CauldronBodyPalette(
            body             = Color(0xFF2A2A2A),
            outline          = Color(0xFF000000),
            rim              = Color(0xFF555555),
            highlight        = Color(0xFF666666),
            rivet            = Color(0xFF888888),
            spoonHandle      = Color(0xFF8B6914),
            spoonBowl        = Color(0xFFCCCCCC),
            spoonBowlOutline = Color(0xFF555555),
        )
    }
}
