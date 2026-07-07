plugins {
    alias(libs.plugins.fabric.loom)
}

group = property("maven_group")!!
version = "${libs.versions.project.get()}+mc${libs.versions.minecraft.get()}"

base {
    archivesName.set(property("archives_base_name")!! as String)
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("tridentsloadchunks") {
            sourceSet(sourceSets.getByName("main"))
            sourceSet(sourceSets.getByName("client"))
        }
    }
}


repositories {
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    implementation(libs.fabric.api)
}

tasks.processResources {
    inputs.property("version", libs.versions.project.get())
    inputs.property("minecraft_version", libs.versions.minecraft.get())
    inputs.property("loader_version", libs.versions.fabric.loader.get())
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(mutableMapOf(
            "version" to libs.versions.project.get(),
            "minecraft_version" to libs.versions.minecraft.get(),
            "loader_version" to libs.versions.fabric.loader.get()
        ))
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${property("archives_base_name")}" }
    }
}