@echo off
echo [Antigravity Patcher] Compiling and running the Single Class patcher...
mvn compile exec:java -Dexec.mainClass="com.ruoyi.patcher.SingleClassPatcher"
pause
