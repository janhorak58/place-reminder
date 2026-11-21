# osmdroid reads preferences and uses reflection-free APIs; keep public API to be safe.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
# Room-generated code is kept by Room's own consumer rules. Play Services ships its own consumer rules.
-keep class io.github.janhorak58.placereminder.data.** { *; }
