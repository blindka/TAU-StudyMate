# Enum constants are stored in the database by name (Converters use .name / valueOf),
# so R8 must not rename them. Everything else in the models can be shrunk.
-keep enum com.tau.studymate.data.model.** { *; }
