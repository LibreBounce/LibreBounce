# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org), which is in turn based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `LegitimizeHorizontalJitter`, `LegitimizeVerticalJitter`, `LegitimizeHorizontalSlowdown` and `LegitimizeVerticalSlowdown` values to all rotation modules; previously hardcoded (thatonecoder)
- `OnWeb` and `OnLiquid` to SuperKnockback (thatonecoder)

### Fixed

- `RenderBoxOnSwingFail` option (and some of its adjacent settings) in KillAura not being subjective, NOT DONE YET (thatonecoder)
- `Verus` fly not damaging (thatonecoder)
- Broken update checker ([#8](https://github.com/LibreBounce/LibreBounce/pull/8)) (thatonecoder)
- No space between the name and version, in the window title ([#12](https://github.com/LibreBounce/LibreBounce/pull/12)) (halflin)

### Changed

- **Breaking:** Rename `ForceGround` to `OnlyGround` in TickBase (thatonecoder)
- **Breaking:** Rename `onlyGround` to `OnlyGround` in TimerRange (thatonecoder)

ChestStealer:
- **Breaking:** Configurable randomizability to SmartDelay, along with normal delay interoperability (thatonecoder)
- **Breaking:** Rename `Highlight-Slot` to `HighlightSlot`, `Border-Strength` to `BorderStrength`, `Chest-Debug` to `ChestDebug`, and `ItemStolen-Debug` to `ItemStolenDebug` (thatonecoder)

- **Breaking:** Rename  `Outline-Width` to `OutlineWidth`, `WireFrame-Width` to `WireFrameWidth`, `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, and `Glow-Target-Alpha` to `GlowTargetAlpha` in ProphuntESP (thatonecoder)
- **Breaking:** Rename `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, `Glow-Target-Alpha` to `GlowTargetAlpha`, and `ESP-ColorMode` to `ESPColorMode` in StorageESP (thatonecoder)
- **Breaking:** Rename `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, and `Glow-Target-Alpha` to `GlowTargetAlpha` in ProphuntESP (thatonecoder)
- **Breaking:** Rename `Health-Mode` to `HealthMode` in PointerESP (thatonecoder)

### Removed

- The flag check in the BoostHypixel fly; use FlagCheck + AutoDisable instead (thatonecoder)

## [0.1.0] - 2025-06-09

_Initial release, forked from LiquidBounce Legacy._

### Added

- `LegitimizeHorizontalImperfectCorrelationFactor` and `LegitimizeVerticalImperfectCorrelationFactor` to all rotation modules; previously hardcoded (thatonecoder)

### Fixed

- `SilentGUI` option in ChestStealer ignoring the `ChestTitle` option (MarkGG, thatonecoder)

### Changed

- Switch from build-based versioning to Semantic versioning (starting by 0.1.0) (thatonecoder)
- Improve European Portuguese translation (thatonecoder)
- Rename project to `LibreBounce` (thatonecoder)

### Removed

- Warning to upgrade from `LiquidBounce` Legacy to Nextgen (thatonecoder)

[unreleased]: https://github.com/LibreBounce/LibreBounce/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/LibreBounce/LibreBounce/releases/tag/v0.1.0
