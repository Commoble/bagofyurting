# 4.0.0.0
* Updated to minecraft 1.20.1

# 3.0.0.0
* Updated to 1.19.1. This is a save-breaking changes, old data from 1.17.x or earlier will not load correctly in 1.19.1+
* Removed blockentity data transformer API for maintainability. Blockentities that are compatible with vanilla structure nbt should generally work with bag of yurting. Blockentities that are not should be blacklisted by mods or modpacks to prevent problems
