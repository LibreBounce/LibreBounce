# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Common Changelog](https://common-changelog.org), which is in turn based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- CombatJump module; jumps at the right time to gain higher momentum (thatonecoder)
- `SimulateKnockback` option, along with `SimulatedHorizontalKnockback` and `SimulatedVerticalKnockback` values to SmartHit (thatonecoder)
- `AttackableOnHurtTime` and `FlushAttackableOnHurtTime` values to Backtrack (thatonecoder)
- `ÒnlySprinting` option to the Jump mode in Velocity; previously hardcoded (thatonecoder)

### Changed

- Improve Backtrack debugging (thatonecoder)
- Fine-tune SmartHit even further (thatonecoder)

## [0.7.0-beta.1] - 2025-11-24

### Added

- `AttackableHurtTime` integer range value to SmartHit; defines when SmartHit considers the target as ready-to-hit (thatonecoder) 

### Fixed

- `PredictClientMovement` not affecting the simulated distance, in SmartHit (thatonecoder)
- `HurtTime` values not working as intended, in KillAura and AutoClicker (thatonecoder)
- InventoryMove not working, when `Undetected` is enabled and the ClickGUI is enabled on the `Panel` mode (thatonecoder)

### Changed

- Fine-tune and improve SmartHit further (thatonecoder)
- Add missing SmartHit entry and improve other things, in the EN_US localization (thatonecoder)

## [0.7.0-alpha.2] - 2025-11-15

### Added

- **Breaking:** `SmartHit` module; split from KillAura, now also with AutoClicker compatiblity (thatonecoder)

## [0.7.0-alpha.1] - 2025-11-14

### Added

- Movement predicting and many other checks for `SmartHit` to KillAura; note that `PredictClientMovement` alters the tick simulation amount (thatonecoder)
- `BlockLength` value to KillAura, potentially allowing for an improved Hypixel AutoBlock (thatonecoder)
- `CheckEnemySprinting` option for the `SmartAutoBlock` option in KillAura (thatonecoder)
- `NormalizeSwingSpeed` option to the Animations module (thatonecoder)
- `RomanNumerals` option for the Effects HUD element; previously hardcoded (thatonecoder)
- `Line` option to Blink; previously hardcoded (thatonecoder)
- `LineColor` value to Blink; previously used Breadcrumbs' color value (thatonecoder)

### Fixed

- AutoWeapon checking for the client-side slot instead of the server slot (thatonecoder)
- `NotOnHurtTime` checking for own hurttime instead of target hurttime, in KillAura (thatonecoder)
- `JitterYawMultiplier` and `JitterPitchMultiplier` appearing despite `Jitter` being toggled off, in Aimbot (thatonecoder)

### Changed

- **Breaking:** Rename `NotBelowHealth` to `NotBelowOwnHealth`, in KillAura (thatonecoder)
- Lower the minimum value in `EnemiesNearby` to 0 in AutoRod, allowing you to ignore the limit (thatonecoder)
- Lower the minimum value in `PulseDelay` from 500 to 0 in Blink (thatonecoder)
- Make the `Jump` mode in Velocity also use the `HurtTime` range (thatonecoder)
- Improve EN_US localization (thatonecoder)

## [0.6.0] - 2025-10-22

_Updates may slow down from now on, since I am prioritizing quality over quantity._

### Added

- **Breaking:** `Box` mode for `Mark`, in ProjectileAimbot; this also means `Mark` is now a list value (thatonecoder)
- `Panel` ClickGUI mode, similar to Rise 6.0's (thatonecoder)
- `SmartHit` option, along with the `NotAboveRange`, `HurtTimeWhitelist` + `NotOnHurtTime`, `NotBelowHealth` + `NotBelowEnemyHealth`, and `NotOnEdge` + `NotOnEdgeLimit` values to KillAura; experimental (thatonecoder)
- `OnlyBlocks` and `NotOnForward` options to Eagle (thatonecoder)
- `MissClickChanceDistanceMultiply` option to ChestStealer (thatonecoder)
- `Notification` option to AutoPlay, notificating when requeueing (thatonecoder)
- `HeldWeapons` option to Notifier (thatonecoder)

### Fixed

- Several Velocity modes not working as intended (thatonecoder)
- `Telly` mode jumping even while not close to an edge, in Scaffold (thatonecoder)
- `WaitForRotations` sneaking despite not being able to rotate yet, in Scaffold (thatonecoder)
- `SkywarsMode` and `BedwarsMode` being displayed despite not being on the `Hypixel` mode, in AutoPlay (thatonecoder)

### Changed

- AntiVoid now has a mode tag (thatonecoder)

### Removed

- `BlockRange`, the `SmartAutoBlock` exclusive option in KillAura; use `BlockMaxRange` instead (thatonecoder)

## [0.5.0] - 2025-09-28

### Added

- **Breaking:** Revamped the Arraylist element, giving nearly full control over its format; please check the code for a better comprehension (thatonecoder)
- **Breaking:** `CategoryCase` and `ModuleCase` options to TabGUI; replaces `UpperCase` (thatonecoder)
- **Breaking:** `HotbarSlotSwitchDelay` to AutoArmor; replaces `DelayedSlotSwitch` ([#28](https://github.com/LibreBounce/LibreBounce/pull/28)) (thatonecoder)
- Legit-like miss-clicking to ChestStealer ([#28](https://github.com/LibreBounce/LibreBounce/pull/28)) (thatonecoder)
- `MarkColor` and `MarkHittableColor` values to KillAura; previously hardcoded (thatonecoder)
- FlagCheck count placeholder to the Text element (thatonecoder)
- `FontShadow` option to Chat (thatonecoder)

### Fixed

- `AACHop4` and `AACHop5` sprinting backwards, causing flags (thatonecoder)

### Changed

- **Breaking:** Make the `Delay`, `StartDelay`, and `CloseDelay` values in AutoSoup to integer ranges (thatonecoder)
- **Breaking:** Rename `Max-Text-Gradient-Colors` to `MaxTextGradientColors`, `Rect-ColorMode` to `RectColorMode`, and `ShadowText` to `TextShadow` in the Arraylist element (thatonecoder)
- **Breaking:** Rename `RectangleRound-Radius` to `RoundedRadius`, and `Text-Shadow` to `TextShadow` in the Keystrokes element (thatonecoder)
- **Breaking:** Rename `RoundRadius` to `RoundedRadius` in the Notifications element (thatonecoder)
- **Breaking:** Rename `View Distance` to `ViewDistance`, `Player Shape` to `PlayerShape`, `Player Size` to `PlayerSize`, `Use ESP Colors` to `UseESPColor`, `FOV Size` to `FOVSize`, `FOV Angle` to `FOVAngle`, `Border-Strength` to `BorderStrength`, `Rainbow-X` to `RainbowX`, and `Rainbow-Y` to `RainbowY`, in the Radar element (thatonecoder)
- **Breaking:** Rename `Rounded-Radius` to `RoundedRadius`, `Border-Strength` to `BorderStrength`, `Rainbow-X` to `RainbowX`, `Rainbow-Y` to `RainbowY`, and `TextPosition-Y` to `TextPositionY` in the TabGUI element (thatonecoder)
- **Breaking:** Rename `Rounded-Radius` to `RoundedRadius`, `Border-Strength` to `BorderStrength`, `Background-ColorMode` to `BackgroundColorMode`, `Background-Color` to `BackgroundColor`, `HealthBar-Gradient1` to `HealthBarGradient1`, `HealthBar-Gradient2` to `HealthBarGradient2`, `Border-Color` to `BorderColor`, `Rainbow-X` to `RainbowX`, and `Rainbow-Y` to `RainbowY` in the Target element (thatonecoder)
- Make even more values have suffixes (thatonecoder)
- Make Parkour not jump when using an item (thatonecoder)

### Removed

- `Tags` and `TagsStyle` values in the Arraylist element; use the features added simultaneously (thatonecoder)
- `Smart` option in AutoClicker; far too buggy ([#30](https://github.com/LibreBounce/LibreBounce/pull/30)) (thatonecoder)

## [0.4.0] - 2025-09-21

### Added

- **Breaking:** `StraightTicksUntilRotation` and `DiagonalTicksUntilRotation` values to the `Telly` mode in Scaffold; split from `TicksUntilRotation` (thatonecoder)
- Suffixes like `ms`, `blocks`, `º`, etc to many values; although it does not alter any values directly, many value names have been changed to accommodate for this (thatonecoder)
- Chat module; originally split from the option `FontChat` in the HUD module (thatonecoder)
- `YawPrevSmoothingTicks`, `PitchPrevSmoothingTicks`, and `PitchPrevSmoothingTicksRandom` to combat modules with rotations (thatonecoder)
- `Smart` option to AutoClicker (thatonecoder)
- `OnFly` and `OnLongJump` options to CameraView (thatonecoder)

### Fixed

- Online settings not working (thatonecoder)
- Update checker not working; however, it no longer checks for development builds (thatonecoder)

### Changed

- **Breaking:** Merge `MaxDelay` and `MinDelay`, but split into an integer range and a range value for the `Modern` and `Legacy` modes respectively, in Backtrack (thatonecoder)
- **Breaking:** `HurtTime` in the `IntaveReduce` and `HurtTimeToClick` modes are now an integer range value, and shared with the `GhostBlock` mode, in Velocity (thatonecoder)
- **Breaking:** Make the `StartDelay` and `CloseDelay` values in AutoArmor and InventoryCleaner integer ranges (thatonecoder)
- **Breaking:** Rename `RoundedHotbar-Radius` to `RoundedHotbarRadius`, and `Hotbar-Color` to `HotbarColor` in HUD (thatonecoder)
- **Breaking:** Rename `Rounded-Radius` to `RoundedRadius` in the scoreboard element (thatonecoder)
- **Breaking:** Rename `MaxRotationDifferenceToSwing` to `MaxAngleDifferenceToSwing` in KillAura (thatonecoder)
- **Breaking:** Rename `RetrieveDelayTicks` to `RetrieveDelay` in NoFall (thatonecoder)
- Increase the `ShortStopLength` value limit to 1000 in ChestStealer (thatonecoder)
- Fine-tune the `Optimal` target priority mode in KillAura (thatonecoder)
- Make the `Debug` options in BackTrack and TickBase subjective (thatonecoder)

### Removed

- `PlaceDelay` option in Scaffold; set the delays both to 0 instead (thatonecoder)

## [0.3.0] - 2025-09-07

### Added

- `Vanilla` mode, along with `Speed`, `IceSlipperiness`, and `PackedIceSlipperiness` values to IceSpeed (thatonecoder)
- `OnlyWhenNearEnemy` and `DistanceToLag` to FakeLag; still rather buggy (thatonecoder)
- `Optimal` target priority mode to KillAura (thatonecoder)
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

[Unreleased]: https://github.com/LibreBounce/LibreBounce/compare/v0.7.0-beta.1...HEAD
[0.7.0-beta.1]: https://github.com/LibreBounce/LibreBounce/compare/v0.7.0-alpha.2...v0.7.0-beta.1
[0.7.0-alpha.2]: https://github.com/LibreBounce/LibreBounce/compare/v0.7.0-alpha.1...v0.7.0-alpha.2
[0.7.0-alpha.1]: https://github.com/LibreBounce/LibreBounce/compare/v0.6.0...v0.7.0-alpha.1
[0.6.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/LibreBounce/LibreBounce/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/LibreBounce/LibreBounce/releases/tag/v0.1.0

