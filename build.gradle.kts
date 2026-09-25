// Kotlin konfigurasjonen er gitt av pluginen 'aap.conventions' i buildSrc
// og settings.gradle.kts


plugins {
    // Provides a no-op 'build' lifecycle task
    base
}

// Call the tasks of the subprojects
for (taskName in listOf("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.tasks.named(taskName) })
    }
}
