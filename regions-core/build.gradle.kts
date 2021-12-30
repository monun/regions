val projectAPI = project(":${rootProject.name}-api")
val pluginName = rootProject.name.split('-').joinToString("") { it.capitalize() }
val packageName = rootProject.name.replace("-", "")
extra.set("pluginName", pluginName)
extra.set("packageName", packageName)

repositories {
    maven(url = "https://maven.enginehub.org/repo/")
}

dependencies {
    implementation(projectAPI)
    implementation("com.sk89q.worldedit:worldedit-bukkit:7.2.8-SNAPSHOT")
}

tasks {
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
            expand(project.extra.properties)
        }
    }

    create<Jar>("paperJar") {
        listOf(projectAPI, project).forEach { from(it.sourceSets["main"].output) }

        archiveVersion.set("")
        archiveBaseName.set(pluginName)

        doLast {
            copy {
                from(archiveFile)
                val plugins = File(rootDir, ".debug/plugins/")
                into(if (File(plugins, archiveFileName.get()).exists()) File(plugins, "update") else plugins)
            }
        }
    }
}