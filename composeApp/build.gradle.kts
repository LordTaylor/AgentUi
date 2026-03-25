plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm() // Standard target name "jvm"
    jvmToolchain(17)
    
    sourceSets {
        val jvmMain by getting {
            kotlin.srcDirs("src/desktopMain/kotlin")
            resources.srcDirs("src/desktopMain/resources")
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val jvmTest by getting {
            kotlin.srcDirs("src/desktopTest/kotlin")
            resources.srcDirs("src/desktopTest/resources")
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":core-api"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.markdown.renderer)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
            }
        }
    }
}

// ── Packaging resources directory ─────────────────────────────────────────────
val packagingResourcesDir = project.layout.projectDirectory.dir("packaging/resources")
val osName = System.getProperty("os.name").lowercase()
val platformSubdir = when {
    osName.contains("mac") || osName.contains("darwin") -> "macos"
    osName.contains("win") -> "windows"
    else -> "linux"
}

val copyRustBinary by tasks.registering(Copy::class) {
    val coreDir = project.layout.projectDirectory.dir("../../CoreApp")
    val binaryName = if (osName.contains("win")) "agent-core.exe" else "agent-core"
    val releaseBin = coreDir.file("target/release/$binaryName").asFile
    val debugBin   = coreDir.file("target/debug/$binaryName").asFile

    val sourceBin = when {
        releaseBin.exists() && debugBin.exists() -> {
            if (releaseBin.lastModified() >= debugBin.lastModified()) releaseBin else debugBin
        }
        releaseBin.exists() -> releaseBin
        debugBin.exists() -> debugBin
        else -> null
    }

    if (sourceBin != null) {
        from(sourceBin)
        into(packagingResourcesDir.dir(platformSubdir))
        rename { binaryName }
    } else {
        println("⚠️ [copyRustBinary] No agent-core binary found in release or debug targets!")
    }
}

val copyBuiltinTools by tasks.registering(Copy::class) {
    val toolsDir = project.layout.projectDirectory.dir("../../CoreApp/tools/builtin")
    from(toolsDir)
    into(packagingResourcesDir.dir("$platformSubdir/tools/builtin"))
    onlyIf { toolsDir.asFile.isDirectory }
}

val preparePackagingResources by tasks.registering {
    dependsOn(copyRustBinary, copyBuiltinTools)
}

compose.desktop {
    application {
        mainClass = "com.agentcore.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "agent-core-ui"
            packageVersion = "1.0.0"
            appResourcesRootDir.set(packagingResourcesDir)
        }
    }
}

// Explicitly set mainClass for the run task as a fallback
tasks.withType<JavaExec>().configureEach {
    if (name == "run" || name == "desktopRun") {
        mainClass.set("com.agentcore.MainKt")
    }
}

afterEvaluate {
    listOf(
        "prepareAppResources",
        "createDistributable", "runDistributable",
        "packageDmg", "packagePkg",
        "packageDeb", "packageRpm",
        "packageMsi", "packageExe"
    ).forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn(preparePackagingResources)
    }
}
