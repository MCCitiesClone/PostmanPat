plugins {
    id("xyz.jpenilla.run-paper") version "2.3.0"
    kotlin("jvm")
}

group = "me.zodd"
version = "2.3.6"

// Realty API version (JitPack tag on MCCitiesClone/realty).
val realtyVersion = "v1.4.4"

// Hibernia Economy API version (JitPack tag on MCCitiesClone/hibernia-economy).
// Provides the business-api and treasury-api surfaces consumed below. Bump when a
// newer release changes the BusinessApi/TreasuryApi methods used by the econ module.
val hiberniaEconomyVersion = "v2.3.379"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://oss.sonatype.org/content/groups/public/")
    maven(url = "https://repo.essentialsx.net/releases/")
    // DiscordSRV
    maven(url = "https://nexus.scarsz.me/content/repositories/public/")
    // Realty + Hibernia Economy (business-api, treasury-api)
    maven(url = "https://jitpack.io")
    maven(url = "https://maven.enginehub.org/repo/")
    maven(url = "https://repo.extendedclip.com/releases/")
    // LibertyBans: bans-api lives in affero-gpl3; its parent POM + omnibus in lesser-gpl3.
    maven(url = "https://mvn-repo.arim.space/affero-gpl3/")
    maven(url = "https://mvn-repo.arim.space/lesser-gpl3/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.discordsrv:discordsrv:1.29.0")
    compileOnly("net.dv8tion:JDA:4.4.0_352.fix-2")
    compileOnly("net.essentialsx:EssentialsX:2.21.0") {
        isTransitive = false
    }
    compileOnly("org.spongepowered:configurate-hocon:4.1.2")
    compileOnly("org.spongepowered:configurate-extra-kotlin:4.1.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    // Hibernia Economy (MCCitiesClone/hibernia-economy) — resolved via JitPack.
    // Money is moved through Treasury; firm lookups/permissions through the Business API.
    // Both are provided at runtime by the Treasury/Business plugins, so exclude their
    // transitive platform/lombok deps.
    compileOnly("com.github.MCCitiesClone.hibernia-economy:treasury-api:$hiberniaEconomyVersion") {
        isTransitive = false
    }
    compileOnly("com.github.MCCitiesClone.hibernia-economy:business-api:$hiberniaEconomyVersion") {
        isTransitive = false
    }
    // Realty (MCCitiesClone/realty) — resolved via JitPack. Bump the tag above when a
    // newer release changes the RealtyPaperApi surface used by RealtyAddon.
    compileOnly("com.github.MCCitiesClone.realty:realty-paper-api:$realtyVersion") {
        // Platform APIs are provided at runtime by the server; exclude Realty's copies
        // to avoid strict gson/fastutil version conflicts with paper-api.
        exclude("io.papermc.paper")
        exclude("com.sk89q.worldguard")
        exclude("com.sk89q.worldedit")
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")
    // LibertyBans API — pulls omnibus (OmnibusProvider/ReactionStage) transitively for compile.
    // Provided at runtime by the LibertyBans plugin.
    compileOnly("space.arim.libertybans:bans-api:1.1.4")
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

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
