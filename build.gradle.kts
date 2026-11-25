plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

// Чистит задачу сборки без Android SDK (полезно для CI-линта разметки).
tasks.register("verifyProject") {
    group = "verification"
    description = "Проверяет, что исходники и ресурсы на месте."
    doLast {
        val required = listOf(
            "app/src/main/AndroidManifest.xml",
            "app/src/main/java/com/sviridova/nexus/ui/HomeActivity.kt",
            "app/src/main/res/values/strings.xml"
        )
        required.forEach { path ->
            if (!file(path).exists()) throw GradleException("Отсутствует $path")
        }
        println("NEXUS: структура проекта в порядке")
    }
}
