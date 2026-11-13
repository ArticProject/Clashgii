package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Graphite neutral ramp (unslop Pass 2: Color)
val GraphiteBg = Color(0xFF111113)
val GraphiteSurface = Color(0xFF18181B)
val GraphiteRaised = Color(0xFF202024)
val GraphiteActive = Color(0xFF27272C)
val GraphiteHairline = Color(0x14F4F4F6) // 8% opacity hairline
val GraphiteHairlineStrong = Color(0x24F4F4F6) // 14% opacity for active/selected borders

// Three strict text roles
val InkPrimary = Color(0xFFF4F4F6)
val InkSecondary = Color(0xFFA1A1AA)
val InkMuted = Color(0xFF71717A)

// Off-white primary action surface on dark theme
val ActionLight = Color(0xFFF4F4F6)
val ActionLightInk = Color(0xFF111113)

// Single brand/world accent: Elixir (used strictly for the elixir bar, droplet cost badge, and active selection)
val ElixirAccent = Color(0xFFD946EF)
val ElixirAccentMuted = Color(0xFF2E1536)

// Toned-down semantic colors
val StatusReady = Color(0xFF34D399)
val StatusReadySurface = Color(0xFF11261F)
val StatusDanger = Color(0xFFF87171)
val StatusDangerSurface = Color(0xFF2A1416)

// Backwards-compatible aliases mapped to the quiet graphite ramp so nothing glows
val ObsidianBg = GraphiteBg
val SlateSurface = GraphiteSurface
val ElevatedCard = GraphiteRaised
val BorderSubtle = GraphiteHairline
val ElixirMagenta = ElixirAccent
val ElixirGlow = ElixirAccent
val ElixirDeep = ElixirAccentMuted
val CrownGold = InkSecondary
val CrownGoldSoft = GraphiteRaised
val EmeraldReady = StatusReady
val EmeraldSoft = StatusReadySurface
val CyanTelemetry = InkSecondary
val CrimsonAlert = StatusDanger
val CrimsonSoft = StatusDangerSurface
val TextPrimary = InkPrimary
val TextSecondary = InkSecondary
val TextMuted = InkMuted
