@echo off
echo [Antigravity Patcher] Compiling and running the bytecode patcher...
mvn compile exec:java -Dexec.mainClass="com.ruoyi.patcher.JavassistPatcher"
pause
