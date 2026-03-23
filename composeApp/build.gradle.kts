plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")
    jvmToolchain(17)
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
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
// agent-core binary and Python tools are bundled here so the UI can start the
// backend automatically without any separate installation step.
//
// Compose Desktop 1.5+ requires platform-specific subdirectories inside appResourcesRootDir:
//   packaging/resources/macos/   → Contents/app/resources/ on macOS
//   packaging/resources/linux/   → lib/app/resources/ on Linux
//   packaging/resources/windows/ → app/resources/ on Windows
val packagingResourcesDir = project.layout.projectDirectory.dir("packaging/resources")

// Detect OS at configuration time for platform-specific resource subdirectory
val osName = System.getProperty("os.name").lowercase()
val platformSubdir = when {
    osName.contains("mac") || osName.contains("darwin") -> "macos"
    osName.contains("win") -> "windows"
    else -> "linux"
}

// Copy the Rust binary for the current platform into the correct platform subdirectory.
// Compose Desktop reads only platform-specific (macos/, linux/, windows/) and common/
// subdirectories — root-level files are ignored by prepareAppResources.
val copyRustBinary by tasks.registering(Copy::class) {
    val coreDir = project.layout.projectDirectory.dir("../../CoreApp")
    val binaryName = if (osName.contains("win")) "agent-core.exe" else "agent-core"
    val releaseBin = coreDir.file("target/release/$binaryName").asFile
    val debugBin   = coreDir.file("target/debug/$binaryName").asFile

    from(if (releaseBin.canExecute()) releaseBin else debugBin)
    into(packagingResourcesDir.dir(platformSubdir))
    rename { binaryName }

    doFirst {
        val src = if (releaseBin.canExecute()) releaseBin else debugBin
        if (!src.canExecute()) throw GradleException(
            "agent-core binary not found — run 'cargo build --release' in CoreApp/ first"
        )
        println("[packaging] Copying $src → packaging/resources/$platformSubdir/$binaryName")
    }
}

// Copy built-in Python tools alongside the binary (platform-specific dir)
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
        mainClass = "MainKt"
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

            // Bundle agent-core binary + Python tools into every distributable
            appResourcesRootDir.set(packagingResourcesDir)
        }
    }
}

// Wire preparePackagingResources before every package/distributable task,
// including Compose Desktop's internal prepareAppResources which reads appResourcesRootDir.
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
