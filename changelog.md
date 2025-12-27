# 21.11.0
* Updated to MC 1.21.11 / neoforge 21.11.13-beta

# 5.0.0.0
* Updated to MC 1.21 / neoforge 21.0.65-beta. This is not compatible with old worlds; loading old worlds in 1.21 will cause bags to lose data.
* Removed legacy shaped bag of yurting upgrade recipe type

# 4.0.0.1
* Ignore blocks tagged with "#forge:relocation_not_supported" by default

# 4.0.0.0
* Updated to minecraft 1.20.1

# 3.0.0.0
* Updated to 1.19.1. This is a save-breaking changes, old data from 1.17.x or earlier will not load correctly in 1.19.1+
* Removed blockentity data transformer API for maintainability. Blockentities that are compatible with vanilla structure nbt should generally work with bag of yurting. Blockentities that are not should be blacklisted by mods or modpacks to prevent problems
