# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org), which is in turn based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Breaking:** `StraightTicksUntilRotation` and `StraightTicksUntilRotation` values to the `Telly` mode in Scaffold; split from `TicksUntilRotation`(thatonecoder)
- Add suffixes like `ms`, `blocks`, etc to many values; although it does not alter any values directly, many value names have been changed to accomodate for this (thatonecoder)
- `Smart` option to AutoClicker (thatonecoder)

### Fixed

- AutoSettings not working (thatonecoder)
- Update checker not working; however, it no longer checks for development builds (thatonecoder)
- `PackedIceSlipperiness` being called `IceSlipperiness`. This caused a bug where the game would crash, if the setting was altered (thatonecoder)

### Changed

- **Breaking:** `HurtTime` in the `IntaveReduce` mode is now an IntRange value, and shared with the `GhostBlock` mode, in Velocity (thatonecoder)
- Fine-tune the `Optimal` target priority mode in KillAura (thatonecoder)

## [0.3.0] - 2025-09-07

### Added

- `Vanilla` mode, along with `Speed`, `IceSlipperiness`, and `PackedIceSlipperiness` values to IceSpeed (thatonecoder)
- `OnlyWhenNearEnemy` and `DistanceToLag` to FakeLag; still rather buggy (thatonecoder)
- `Optimal` mode for target priority to KillAura (thatonecoder)
- `SpacedValues` option to ClickGUI (thatonecoder)
- `SpacedTags` option to ArrayList (thatonecoder)
- `Debug` option to BackTrack (thatonecoder)
- `Debug` option to TickBase (thatonecoder)

### Fixed

- `ShortStopLength` not doing anything, and appearing regardless of whether `SimulateShortStop` was enabled or not in ChestStealer (thatonecoder)

### Changed

- **Breaking:** Rename `CPS-Multiplier` to `CPSMultiplier` in KillAura (thatonecoder)
- **Breaking:** Rename `Custom-Y` to `CustomY` in Criticals (thatonecoder)

## [0.2.0] - 2025-09-04

### Added

- **Breaking:** Configurable randomizability for `SmartDelay` to ChestStealer (thatonecoder)
- Smart delay interoperability with normal delay to ChestStealer (thatonecoder)
- Configurable `SimulateShortStop` values to ChestStealer; previously hardcoded (thatonecoder)
- `LegitimizeHorizontalJitter`, `LegitimizeVerticalJitter`, `LegitimizeHorizontalSlowdown` and `LegitimizeVerticalSlowdown` values to all rotation modules; previously hardcoded (thatonecoder)
- `OnWeb` and `OnLiquid` options to SuperKnockback (thatonecoder)

### Fixed

- `Verus` fly not damaging (thatonecoder)
- Broken update checker ([#8](https://github.com/LibreBounce/LibreBounce/pull/8)) (thatonecoder)
- No space between the name and version, in the window title ([#12](https://github.com/LibreBounce/LibreBounce/pull/12)) (halflin)

### Changed

- **Breaking:** Rename `Highlight-Slot` to `HighlightSlot`, `Border-Strength` to `BorderStrength`, `Chest-Debug` to `ChestDebug`, and `ItemStolen-Debug` to `ItemStolenDebug` in ChestStealer (thatonecoder)
- **Breaking:** Rename `Highlight-Slot` to `HighlightSlot`, and `Border-Strength` to `BorderStrength` in AutoArmor & InventoryCleaner (thatonecoder)
- **Breaking:** Rename `ForceGround` to `OnlyGround` in TickBase (thatonecoder)
- **Breaking:** Rename `onlyGround` to `OnlyGround` in TimerRange (thatonecoder)
- **Breaking:** Rename `CustomDamage-Packet1Clip` to `CustomDamagePacket1Clip`, `CustomDamage-Packet2Clip` to `CustomDamagePacket2Clip`, and `CustomDamage-Packet3Clip` to `CustomDamagePacket3Clip` in Damage (thatonecoder)
- **Breaking:** Rename  `Outline-Width` to `OutlineWidth`, `WireFrame-Width` to `WireFrameWidth`, `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, and `Glow-Target-Alpha` to `GlowTargetAlpha` in ProphuntESP (thatonecoder)
- **Breaking:** Rename `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, `Glow-Target-Alpha` to `GlowTargetAlpha`, and `ESP-ColorMode` to `ESPColorMode` in StorageESP (thatonecoder)
- **Breaking:** Rename `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, and `Glow-Target-Alpha` to `GlowTargetAlpha` in ItemESP (thatonecoder)
- **Breaking:** Rename `Glow-Renderscale` to `GlowRenderscale`, `Glow-Radius` to `GlowRadius`, `Glow-Fade` to `GlowFade`, and `Glow-Target-Alpha` to `GlowTargetAlpha` in ProphuntESP (thatonecoder)
- **Breaking:** Rename `Health-Mode` to `HealthMode` in PointerESP (thatonecoder)
- **Breaking:** Rename `Text-ColorMode` to `TextColorMode`, `Text-Gradient-Speed` to `TextGradientSpeed`, `Max-Text-Gradient-Colors` to `MaxTextGradientColors`, `Text-Gradient` to `TextGradient`, `Rounded-Radius` to `RoundedRadius`, `Background-Mode` to `BackgroundMode`, `Background-Gradient-Speed` to `BackgroundGradientSpeed`, `Max-Background-Gradient-Colors` to `MaxBackgroundGradientColors`, `Background-Gradient` to `BackgroundGradient`, `Rainbow-X` to `RainbowX`, `Rainbow-Y` to `RainbowY`, `Gradient-X` to `GradientX`, and `Gradient-Y` to `GradientY` in BedPlates (thatonecoder)
- **Breaking:** Rename `Hotbar-Highlight-Colors` to `HotbarHightlightColors`, `Hotbar-Background-Colors` to `HotbarBackgroundColors`, `Hotbar-Gradient-Speed` to `HotbarGradientSpeed`, `Max-Hotbar-Gradient-Colors` to `MaxHotbarGradientColors`, `Hotbar-Gradient` to `HotbarGradient`, `HotbarBorder-Highlight-Width` to `HotbarBorderHighlightWidth`, `HotbarBorder-Highlight-Colors` to `HotbarBorderHighlightColors`, `HotbarBorder-Background-Width` to `HotbarBorderBackgroundWidth`, `HotbarBorder-Background-Colors` to `HotbarBorderBackgroundColors`, `Rainbow-X` to `RainbowX`, `Rainbow-Y` to `RainbowY`, `Gradient-X` to `GradientX`, and `Gradient-Y` to `GradientY` in BedPlates (thatonecoder)
- **Breaking:** Rename a lot of things in the Arraylist, Text, and Inventory HUD elements; just check the git log, at this point (thatonecoder)
- **Breaking:** Rename `Scrolls` to `Scrolling` in ClickGUI (thatonecoder)

### Removed

- The flag check in the BoostHypixel fly; use FlagCheck + AutoDisable instead (thatonecoder)

## [0.1.0] - 2025-06-09

_Initial release, forked from LiquidBounce Legacy._

### Added

- `LegitimizeHorizontalImperfectCorrelationFactor` and `LegitimizeVerticalImperfectCorrelationFactor` to all rotation modules; previously hardcoded (thatonecoder)

### Fixed

- `SilentGUI` option in ChestStealer ignoring the `ChestTitle` option (thatonecoder, MarkGG)

### Changed

- Switch from build-based versioning to Semantic versioning (starting by 0.1.0) (thatonecoder)
- Improve European Portuguese translation (thatonecoder)
- Rename project to `LibreBounce` (thatonecoder)

### Removed

- Warning to upgrade from `LiquidBounce` legacy to nextgen (thatonecoder)

[unreleased]: https://github.com/LibreBounce/LibreBounce/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/LibreBounce/LibreBounce/releases/tag/v0.1.0
