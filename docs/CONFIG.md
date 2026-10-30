本分支完整支持 Minecraft 1.21.10，NeoForge 依赖范围为 `[21.10,)`，使用 Java 21。

保留假人创建与管理、行为控制、背包和末影箱操作、自动钓鱼、工具替换、物品补充、常驻与机器人分组。第三方联动以对应 Minecraft 版本可用的依赖为准。

构建命令为 `./gradlew.bat compileJava build --console=plain`，输出 `build/libs/silicone_dolls-1.0.0-mc1.21.10.jar`。构建时优先使用相邻目录中的同版本 RollingGate 源码，可用 `-Prolling_gate_source=路径` 指定源码位置。
