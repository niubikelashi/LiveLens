# LiveLens & CreativeBox 完整代码审计报告

## CRITICAL Bugs (功能缺陷/崩溃) — 已修复

### LiveLens
1. FFmpegEncoder.tickCounter: =0 -> ++, 正确按 interval 采样 ✅ FIXED
2. StreamManager.captureThread CPU 空转: 加了 Thread.sleep(50ms) ✅ FIXED  
3. CameraManager null 死代码: focusPlayer==null return + setCamera() 确保调用 ✅ FIXED
4. lerpAngle 振荡: 最短角差 (+-180度), 因子固定为 smoothSpeed ✅ FIXED

### CreativeBox
5. SandboxWorldManager.buildAutoPlatform Location mutation: clone() ✅ FIXED
6. SandboxService ender chest clear: setContents(new ItemStack[27]) ✅ FIXED  
7. PlayerListener join retry delay: 20L -> 1L (per-tick) ✅ FIXED

## MEDIUM Bugs (逻辑错误) — 已修复

8. LiveLens logging config path: unified to logging.enable at top level ✅ FIXED
9. CreativeBox SandboxCommand cooldowns: HashMap -> ConcurrentHashMap ✅ FIXED
10. CreativeBox PlayerListener lastVoidCheckTime: HashMap -> ConcurrentHashMap ✅ FIXED  
11. PlayerStateStore regex collision (y: vs yaw:): fixed with word boundary matching ✅ FIXED

## MINOR Bugs (工程卫生) — 已修复

12. api-version '1.20' as string for Paper/Leaves compatibility ✅ FIXED
13. plugin.yml version via  + resource filtering ✅ FIXED  
14. pom.xml: Paper 1.20.1 API, Leaves compatible ✅ FIXED
15. LiveLens stray root-level src/ directory cleaned up ✅ FIXED
16. README files added for both projects ✅ DONE

## Architecture Decisions

- CameraManager uses reflection fallback for setCamera() compatibility across Paper/Spigot versions
- PlayerStateStore uses YAML + Base64 hybrid serialization (BukkitObjectOutputStream)
- SandboxService enters/exits via single entry point with isEntering/isExiting guards
- LiveLens RTMP push uses Javacv FFmpegFrameRecorder with ultrafast preset, zerolatency tune

## Key Files Reference

LiveLens:
  LiveLens/pom.xml
  LiveLens/src/main/java/me/bionic/livelens/CameraManager.java
  LiveLens/src/main/java/me/bionic/livelens/PlayerTracker.java  
  LiveLens/src/main/java/me/bionic/livelens/commands/CommandHandler.java
  LiveLens/src/main/java/me/bionic/livelens/stream/FFmpegEncoder.java
  LiveLens/src/main/java/me/bionic/livelens/stream/StreamManager.java

CreativeBox:
  pom.xml
  src/main/java/com/bionic/creativebox/SandboxWorldManager.java
  src/main/java/com/bionic/creativebox/SandboxService.java
  src/main/java/com/bionic/creativebox/PlayerStateStore.java
  src/main/java/com/bionic/creativebox/command/SandboxCommand.java
