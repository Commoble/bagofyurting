Bag of Yurting is a minecraft forge mod that adds a bag that can be used to pick up and store blocks and move them elsewhere.

Built jars of Bag of Yurting are available here

https://www.curseforge.com/minecraft/mc-mods/bag-of-yurting

Mods can use Bag of Yurting in their development workspace by adding the following to their build.gradle:

```
repositories {
	// repo to get Bag of Yurting jars from
	maven { url "https://cubicinterpolation.net/maven/" }
}

dependencies {
	// compile against the API only
	compileOnly fg.deobf("commoble.bagofyurting:${$bagofyurting_branch}:${$bagofyurting_version}:api")
	// use the production jar when running minecraft from dev
	runtimeOnly fg.deobf("commoble.bagofyurting:${$bagofyurting_branch}:${$bagofyurting_version}")
}
```

Where
* `{$bagofyurting_branch}` is e.g. `bagofyurting-1.16.4`
* `{$bagofyurting_version}` is e.g. `1.2.0.0`

The API is only available on branches `bagofyurting-1.16.4` and newer and versions `1.2.0.0` and newer.

A debug jar with full sources is available for easier debugging, which can be used by compiling against :debug instead of :api.
It is not recommended to compile against the debug jar when building production jars, as binary-breaking changes are more likely to occur with it.

More documentation on Bag of Yurting is available on the github wiki:

https://github.com/Commoble/bagofyurting/wiki