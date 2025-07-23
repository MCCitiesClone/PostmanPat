plugins {
    id("xyz.jpenilla.run-paper") version "2.3.0"
    kotlin("jvm")
}

group = "me.zodd"
version = "2.3.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://oss.sonatype.org/content/groups/public/")
    maven(url = "https://repo.essentialsx.net/releases/")
    // DiscordSRV
    maven(url = "https://nexus.scarsz.me/content/repositories/public/")
    // VaultAPI
    maven(url = "https://jitpack.io")
    // DemocracyBusiness
    maven(url = "https://repo.olziedev.com/")
    maven(url = "https://maven.enginehub.org/repo/")
    maven(url = "https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.discordsrv:discordsrv:1.29.0")
    compileOnly("net.dv8tion:JDA:4.4.0_352.fix-2")
    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        isTransitive = false
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    compileOnly("org.spongepowered:configurate-hocon:4.1.2")
    compileOnly("org.spongepowered:configurate-extra-kotlin:4.1.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    compileOnly("com.olziedev:playerbusinesses-api:1.5.1") {
        isTransitive = false
    }
    compileOnly("com.github.md5sha256.AreaShop:areashop:-SNAPSHOT") {
        exclude("io.papermc")
        exclude("io.github.baked-libs")
        exclude("com.google.inject")
        exclude("org.spigotmc")
        exclude("com.sk89q.worldguard")
        exclude("com.sk89q.worldedit")
    }
    compileOnly("com.gitlab.ruany:LiteBansAPI:0.6.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.runServer {
    minecraftVersion("1.21.4")
}

tasks.compileJava {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}
