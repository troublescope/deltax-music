@file:Suppress("UnstableApiUsage")

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.net.URI
import java.util.Base64
import java.util.Locale
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}

compose {
    dependencyCompatibility {
        exclude("io.coil-kt.coil3")
    }
}

abstract class GenerateRoomMigrationTestSchemas : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemaDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDirectory.file("app/deltax/music/shared/db/HistoricalRoomSchemas.kt").get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                appendLine("package app.deltax.music.shared.db")
                appendLine("import kotlin.io.encoding.Base64")
                appendLine("internal fun historicalRoomSchema(version: Int): String = when (version) {")
                schemaDirectory
                    .get()
                    .asFile
                    .listFiles()
                    .orEmpty()
                    .filter { it.extension == "json" }
                    .sortedBy { it.nameWithoutExtension.toInt() }
                    .forEach { schema ->
                        val encoded = Base64.getEncoder().encodeToString(schema.readLines().joinToString("") { it.trim() }.toByteArray())
                        appendLine("${schema.nameWithoutExtension} -> Base64.decode(\"$encoded\").decodeToString()")
                    }
                appendLine("else -> error(\"No exported Room schema for version \" + version)")
                appendLine("}")
            },
        )
    }
}

val generateRoomMigrationTestSchemas =
    tasks.register<GenerateRoomMigrationTestSchemas>("generateRoomMigrationTestSchemas") {
        schemaDirectory.set(layout.projectDirectory.dir("schemas/app.deltax.music.shared.db.MusicDatabase"))
        outputDirectory.set(layout.buildDirectory.dir("generated/source/roomMigrationTest/kotlin"))
    }

abstract class GenerateOpusTestFixture : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fixture: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val destination = outputDirectory.file("app/deltax/music/shared/playback/opus/OpusTestFixture.kt").get().asFile
        destination.parentFile.mkdirs()
        val encoded = Base64.getEncoder().encodeToString(fixture.get().asFile.readBytes())
        destination.writeText(
            "package app.deltax.music.shared.playback.opus\n" +
                "import kotlin.io.encoding.Base64\n" +
                "internal fun opusWebmFixture(): ByteArray = Base64.decode(\"$encoded\")\n",
        )
    }
}

val generateOpusTestFixture =
    tasks.register<GenerateOpusTestFixture>("generateOpusTestFixture") {
        fixture.set(layout.projectDirectory.file("src/commonTest/resources/ffmpeg/opus-251.webm"))
        outputDirectory.set(layout.buildDirectory.dir("generated/opusTestFixture"))
    }

tasks.matching { it.name == "lintAnalyzeAndroidHostTest" || it.name == "generateAndroidHostTestLintModel" }.configureEach {
    dependsOn(generateRoomMigrationTestSchemas, generateOpusTestFixture)
}

val appVersion = providers.gradleProperty("appVersion").get()
val innerTubeXVersion = providers.gradleProperty("innerTubeXVersion").get()
val bundledLanguageParser =
    DocumentBuilderFactory
        .newInstance()
        .apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        .newDocumentBuilder()
val bundledLanguageTags =
    file("src/commonMain/composeResources")
        .listFiles()
        .orEmpty()
        .asSequence()
        .filter { it.isDirectory && it.name.startsWith("values-") && it.name != "values-xml" }
        .filter { directory ->
            val catalog = File(directory, "strings.xml").takeIf(File::isFile) ?: return@filter false
            val root = bundledLanguageParser.parse(catalog).documentElement
            root.getElementsByTagName("string").length > 0 || root.getElementsByTagName("plurals").length > 0
        }.map { directory ->
            val qualifier = directory.name.removePrefix("values-")
            val languageTag =
                if (qualifier.startsWith("b+")) {
                    qualifier.removePrefix("b+").replace('+', '-')
                } else {
                    qualifier.replace(Regex("-r(?=[A-Z]{2}(?:$|-))"), "-")
                }
            Locale.forLanguageTag(languageTag).toLanguageTag()
        }.filter { it.isNotBlank() && it != "und" }
        .distinct()
        .sorted()
        .toList()

kotlin {
    android {
        namespace = "app.deltax.music.shared"
        compileSdk {
            version = release(37) { minorApiLevel = 1 }
        }
        minSdk = 24
        androidResources.enable = true
        lint { ignoreTestSources = true }
        withHostTest {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
        }
        getByName("commonMain") {
            dependencies {
                implementation("com.github.nyxiereal.glyphmind:glyphmind:0.1.0")
                api("com.github.MetrolistGroup.innertubex:innertubex:$innerTubeXVersion")
                api(libs.runtime)
                api(libs.foundation)
                api(libs.androidx.navigationevent.compose)
                api(libs.material3)
                api(libs.material3.adaptive)
                api(libs.material3.adaptive.navigation.suite)
                api(libs.components.resources)
                implementation(libs.ui.tooling.preview)
                implementation(libs.compose.reorderable)
                implementation(libs.swipe)
                api(libs.markdown.renderer)
                implementation(libs.markdown.renderer.m3)
                implementation(libs.markdown.renderer.coil3)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.json)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(libs.datastore)

                implementation(libs.room.runtime)

                implementation(libs.koin.core)

                api(libs.androidx.navigation3.runtime)

                api(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.datastore)

                implementation(libs.materialKolor)

                api(libs.shimmer)

                implementation("io.coil-kt.coil3:coil-compose:${libs.versions.coil.get()}") {
                    exclude(group = "org.jetbrains.skiko", module = "skiko")
                }
                implementation("io.coil-kt.coil3:coil-network-core:${libs.versions.coil.get()}")
            }
        }
        getByName("commonTest") {
            kotlin.srcDir(generateRoomMigrationTestSchemas)
            kotlin.srcDir(generateOpusTestFixture)
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinxCoroutines.get()}")
                implementation(libs.ktor.client.mock)
                implementation(libs.multiplatform.settings.test)
            }
        }
        val jvmSharedTest =
            create("jvmSharedTest") {
                dependsOn(getByName("commonTest"))
            }
        getByName("androidHostTest") {
            dependsOn(jvmSharedTest)
            dependencies {
                implementation("org.robolectric:robolectric:4.18")
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
            }
        }
        val jvmSharedMain =
            create("jvmSharedMain") {
                dependsOn(getByName("commonMain"))
                dependencies {
                    implementation(project(":listenTogetherProto"))
                    implementation(libs.ktor.client.logging)
                    implementation(libs.ktor.client.cio)
                    implementation(libs.okhttp)
                }
            }
        getByName("androidMain") {
            dependsOn(jvmSharedMain)
            dependencies {
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.androidx.core.ktx)
                implementation(libs.browser)
                api(libs.room.runtime)
                implementation(libs.coil.network.okhttp)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.websockets)
                implementation(libs.timber)

                implementation(libs.media3.exoplayer)
                implementation(libs.media3.common)
                implementation(libs.media3.datasource)
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

val prepareComposeResourceAliases =
    tasks.register<Sync>("prepareComposeResourceAliases") {
        val catalog = layout.projectDirectory.dir("src/commonMain/composeResources")
        from(catalog)
        mapOf("in" to "id", "iw" to "he").forEach { (legacy, modern) ->
            from(catalog.dir("values-$legacy")) { into("values-$modern") }
        }
        into(layout.buildDirectory.dir("generated/composeResourceAliases"))
    }

compose.resources {
    packageOfResClass = "app.deltax.music.shared.generated.resources"
    publicResClass = true
    customDirectory("commonMain", layout.dir(prepareComposeResourceAliases.map { it.destinationDir }))
}

tasks.matching { it.name == "generateResourceAccessorsForCommonMain" }.configureEach {
    outputs.cacheIf { true }
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val generateBuildConfig =
    tasks.register("generateBuildConfig") {
        val outputDir =
            layout.buildDirectory
                .dir("generated/source/config/main/kotlin")
                .get()
                .asFile

        inputs.files(localPropertiesFile)

        val lastFmKey =
            providers
                .gradleProperty("LASTFM_API_KEY")
                .filter { it.isNotEmpty() }
                .orElse(providers.environmentVariable("LASTFM_API_KEY").filter { it.isNotEmpty() })
                .orElse(providers.provider { localProperties.getProperty("LASTFM_API_KEY").orEmpty() }.filter { it.isNotEmpty() })
                .orElse(providers.provider { "" })
                .get()
                .trim()
        val lastFmSecret =
            providers
                .gradleProperty("LASTFM_SECRET")
                .filter { it.isNotEmpty() }
                .orElse(providers.environmentVariable("LASTFM_SECRET").filter { it.isNotEmpty() })
                .orElse(providers.provider { localProperties.getProperty("LASTFM_SECRET").orEmpty() }.filter { it.isNotEmpty() })
                .orElse(providers.provider { "" })
                .get()
                .trim()
        val isIzzyTask = gradle.startParameter.taskNames.any { it.contains("Izzy", ignoreCase = true) }
        val isReleaseTask =
            gradle.startParameter.taskNames.any { name ->
                name.contains("Izzy", ignoreCase = true) ||
                    name.contains("Release", ignoreCase = true) ||
                    name.contains("Benchmark", ignoreCase = true) ||
                    (name.contains("package", ignoreCase = true) && !name.contains("Debug", ignoreCase = true)) ||
                    name.contains("Distributable", ignoreCase = true)
            }
        val metrolistDebugProp =
            providers
                .gradleProperty("deltax.debug")
                .orNull
                ?.toBooleanStrictOrNull()
        val xcodeDebug =
            providers
                .environmentVariable("CONFIGURATION")
                .orNull
                ?.let { !it.equals("Release", ignoreCase = true) }
        val debugValue: Boolean = metrolistDebugProp ?: xcodeDebug ?: !isReleaseTask
        val lyprioBaseUrl =
            providers
                .gradleProperty("LYPRIO_BASE_URL")
                .filter { it.isNotEmpty() }
                .orElse(providers.environmentVariable("LYPRIO_BASE_URL").filter { it.isNotEmpty() })
                .orElse(providers.provider { localProperties.getProperty("LYPRIO_BASE_URL").orEmpty() }.filter { it.isNotEmpty() })
                .orElse(providers.provider { "" })
                .get()
                .trim()
        val lyprioHmacKey =
            providers
                .gradleProperty("LYPRIO_HMAC_KEY")
                .filter { it.isNotEmpty() }
                .orElse(providers.environmentVariable("LYPRIO_HMAC_KEY").filter { it.isNotEmpty() })
                .orElse(providers.provider { localProperties.getProperty("LYPRIO_HMAC_KEY").orEmpty() }.filter { it.isNotEmpty() })
                .orElse(providers.provider { "" })
                .get()
                .trim()
        require(lyprioBaseUrl.isBlank() == lyprioHmacKey.isBlank()) {
            "LYPRIO_BASE_URL and LYPRIO_HMAC_KEY must be configured together"
        }
        if (lyprioBaseUrl.isNotBlank()) {
            val uri = URI(lyprioBaseUrl)
            require(
                !uri.host.isNullOrBlank() &&
                    uri.rawUserInfo == null &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null &&
                    (
                        (uri.scheme == "https" && (uri.port == -1 || uri.port == 443)) ||
                            (debugValue && uri.scheme == "http")
                    ),
            ) { "LYPRIO_BASE_URL must use HTTPS on port 443; HTTP is allowed only in debug builds" }
            require(Base64.getDecoder().decode(lyprioHmacKey).size == 32) {
                "LYPRIO_HMAC_KEY must be a base64-encoded 32-byte key"
            }
        }
        inputs.property("lastFmKey", lastFmKey)
        inputs.property("lastFmSecret", lastFmSecret)
        // shortcut: BuildConfig is per invocation, so an Izzy build leaves Lyprio out of every APK it assembles; build Izzy alone.
        inputs.property("lyprioBaseUrl", if (isIzzyTask) "" else lyprioBaseUrl)
        inputs.property("lyprioHmacKey", if (isIzzyTask) "" else lyprioHmacKey)
        val sourceRevision =
            providers
                .gradleProperty("deltax.sourceRevision")
                .orElse(providers.environmentVariable("GITHUB_SHA"))
                .map(String::trim)
                .filter { it.matches(Regex("[0-9a-fA-F]{40}")) }
                .orElse("")
                .get()
        inputs.property("appVersion", appVersion)
        inputs.property("sourceRevision", sourceRevision)
        inputs.property("bundledLanguageTags", bundledLanguageTags.joinToString(","))

        inputs.property("debug", debugValue)

        outputs.dir(outputDir)
        outputs.cacheIf { false }

        doLast {
            val configFile = outputDir.resolve("app/deltax/music/shared/BuildConfig.kt")
            configFile.parentFile.mkdirs()
            configFile.writeText(
                """
                package app.deltax.music.shared

                import kotlin.experimental.ExperimentalObjCRefinement
                import kotlin.native.HiddenFromObjC

                @OptIn(ExperimentalObjCRefinement::class)
                @HiddenFromObjC
                object BuildConfig {
                    const val LASTFM_API_KEY = "${inputs.properties["lastFmKey"]}"
                    const val LASTFM_SECRET = "${inputs.properties["lastFmSecret"]}"
                    const val LYPRIO_BASE_URL = "${inputs.properties["lyprioBaseUrl"]}"
                    const val LYPRIO_HMAC_KEY = "${inputs.properties["lyprioHmacKey"]}"
                    const val APP_VERSION = "${inputs.properties["appVersion"]}"
                    const val SOURCE_REVISION = "${inputs.properties["sourceRevision"]}"
                    const val BUNDLED_LANGUAGE_TAGS = "${inputs.properties["bundledLanguageTags"]}"
                    const val DEBUG = ${inputs.properties["debug"]}
                }
                """.trimIndent(),
            )
        }
    }

kotlin {
    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir(generateBuildConfig)
        }
    }
}

dependencies {
    add("androidRuntimeClasspath", libs.ui.tooling)
    add("kspAndroid", libs.room.compiler)
}

class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDirectory: File,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf("room.schemaLocation=${schemaDirectory.path}")
}

ksp {
    arg(RoomSchemaArgProvider(layout.projectDirectory.dir("schemas").asFile))
}

val runLiveLyricsTests =
    providers
        .systemProperty("runLiveLyricsTests")
        .orElse(providers.environmentVariable("RUN_LIVE_LYRICS_TESTS"))
        .orElse("false")

val runLiveInnerTubeClientTests =
    providers
        .systemProperty("runLiveInnerTubeClientTests")
        .orElse(providers.environmentVariable("RUN_LIVE_INNERTUBE_CLIENT_TESTS"))
        .orElse("false")

val runLiveInnerTubeClientBenchmarks =
    providers
        .systemProperty("runLiveInnerTubeClientBenchmarks")
        .orElse(providers.environmentVariable("RUN_LIVE_INNERTUBE_CLIENT_BENCHMARKS"))
        .orElse("false")

val runLiveSignedOutPlaybackMatrix =
    providers
        .systemProperty("runLiveSignedOutPlaybackMatrix")
        .orElse(providers.environmentVariable("RUN_LIVE_SIGNED_OUT_PLAYBACK_MATRIX"))
        .orElse("false")

val runLiveInnerTubePlaybackMatrix =
    providers
        .systemProperty("runLiveInnerTubePlaybackMatrix")
        .orElse(providers.environmentVariable("RUN_LIVE_INNERTUBE_PLAYBACK_MATRIX"))
        .orElse("false")

val innerTubeSignedOut =
    providers
        .systemProperty("innerTubeSignedOut")
        .orElse(providers.environmentVariable("INNERTUBE_SIGNED_OUT"))
        .orElse(providers.provider { localProperties.getProperty("innerTubeSignedOut").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_SIGNED_OUT").orEmpty() }.filter { it.isNotBlank() })
        .orElse("false")

val failLiveInnerTubeClientFailures =
    providers
        .systemProperty("failLiveInnerTubeClientFailures")
        .orElse(providers.environmentVariable("FAIL_LIVE_INNERTUBE_CLIENT_FAILURES"))
        .orElse("false")

val localInnerTubeBenchmarkIterations =
    providers
        .provider { localProperties.getProperty("innerTubeBenchmarkIterations").orEmpty() }
        .filter { it.isNotBlank() }
val localInnerTubeBenchmarkIterationsUpper =
    providers
        .provider { localProperties.getProperty("INNERTUBE_BENCHMARK_ITERATIONS").orEmpty() }
        .filter { it.isNotBlank() }

val innerTubeBenchmarkIterations =
    providers
        .systemProperty("innerTubeBenchmarkIterations")
        .filter { it.isNotBlank() }
        .orElse(providers.gradleProperty("innerTubeBenchmarkIterations").filter { it.isNotBlank() })
        .orElse(providers.environmentVariable("INNERTUBE_BENCHMARK_ITERATIONS").filter { it.isNotBlank() })
        .orElse(localInnerTubeBenchmarkIterations)
        .orElse(localInnerTubeBenchmarkIterationsUpper)
        .orElse("3")

val innerTubeStreamDelaySeconds =
    providers
        .systemProperty("innerTubeStreamDelaySeconds")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_STREAM_DELAY_SECONDS").filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("innerTubeStreamDelaySeconds").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_STREAM_DELAY_SECONDS").orEmpty() }.filter { it.isNotBlank() })
        .orElse("10")

val innerTubeGroupDelaySeconds =
    providers
        .systemProperty("innerTubeGroupDelaySeconds")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_GROUP_DELAY_SECONDS").filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("innerTubeGroupDelaySeconds").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_GROUP_DELAY_SECONDS").orEmpty() }.filter { it.isNotBlank() })
        .orElse("20")

val innerTubePlaybackCaseDelaySeconds =
    providers
        .systemProperty("innerTubePlaybackCaseDelaySeconds")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PLAYBACK_CASE_DELAY_SECONDS").filter { it.isNotBlank() })
        .orElse(
            providers
                .provider { localProperties.getProperty("innerTubePlaybackCaseDelaySeconds").orEmpty() }
                .filter { it.isNotBlank() },
        ).orElse(
            providers
                .provider { localProperties.getProperty("INNERTUBE_PLAYBACK_CASE_DELAY_SECONDS").orEmpty() }
                .filter { it.isNotBlank() },
        ).orElse("20")

val innerTubePlaybackUrls =
    providers
        .systemProperty("innerTubePlaybackUrls")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PLAYBACK_URLS").filter { it.isNotBlank() })
        .orElse("")

val innerTubePlaybackClientFilter =
    providers
        .systemProperty("innerTubePlaybackClientFilter")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PLAYBACK_CLIENT_FILTER").filter { it.isNotBlank() })
        .orElse("")

val innerTubePoTokenProviderUrl =
    providers
        .systemProperty("innerTubePoTokenProviderUrl")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PO_TOKEN_PROVIDER_URL").filter { it.isNotBlank() })
        .orElse("")

val innerTubePoTokenProviderCapabilities =
    providers
        .systemProperty("innerTubePoTokenProviderCapabilities")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PO_TOKEN_PROVIDER_CAPABILITIES").filter { it.isNotBlank() })
        .orElse("")

val innerTubePlaybackStreamOrdinalFilter =
    providers
        .systemProperty("innerTubePlaybackStreamOrdinalFilter")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_PLAYBACK_STREAM_ORDINAL_FILTER").filter { it.isNotBlank() })
        .orElse("")

val innerTubeClientFilter =
    providers
        .systemProperty("innerTubeClientFilter")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_CLIENT_FILTER").filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("innerTubeClientFilter").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_CLIENT_FILTER").orEmpty() }.filter { it.isNotBlank() })
        .orElse("")

val innerTubeContentTypeFilter =
    providers
        .systemProperty("innerTubeContentTypeFilter")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_CONTENT_TYPE_FILTER").filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("innerTubeContentTypeFilter").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_CONTENT_TYPE_FILTER").orEmpty() }.filter { it.isNotBlank() })
        .orElse("")

val innerTubeStreamOrdinalFilter =
    providers
        .systemProperty("innerTubeStreamOrdinalFilter")
        .filter { it.isNotBlank() }
        .orElse(providers.environmentVariable("INNERTUBE_STREAM_ORDINAL_FILTER").filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("innerTubeStreamOrdinalFilter").orEmpty() }.filter { it.isNotBlank() })
        .orElse(providers.provider { localProperties.getProperty("INNERTUBE_STREAM_ORDINAL_FILTER").orEmpty() }.filter { it.isNotBlank() })
        .orElse("")

val sabrVideoId = providers.systemProperty("sabrVideoId").orElse("")
val sabrUsePlaybackClock = providers.systemProperty("sabrUsePlaybackClock").orElse("true")
val signedOutProbeVideoId =
    providers
        .systemProperty("signedOutProbeVideoId")
        .orElse(providers.environmentVariable("SIGNED_OUT_PROBE_VIDEO_ID"))
        .orElse("")

val javaToolchainService = extensions.getByType<JavaToolchainService>()

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
    javaLauncher.set(
        javaToolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        },
    )
    inputs.property("runLiveLyricsTests", runLiveLyricsTests)
    inputs.property("runLiveAppleMusicTests", providers.environmentVariable("RUN_LIVE_APPLE_MUSIC_TESTS").orElse("false"))
    inputs.property("appleMusicTestCountry", providers.environmentVariable("APPLE_MUSIC_TEST_COUNTRY").orElse(""))
    inputs.property("appleMusicTestLanguage", providers.environmentVariable("APPLE_MUSIC_TEST_LANGUAGE").orElse(""))
    inputs.property("runLiveInnerTubeClientTests", runLiveInnerTubeClientTests)
    inputs.property("runLiveInnerTubeClientBenchmarks", runLiveInnerTubeClientBenchmarks)
    inputs.property("runLiveSignedOutPlaybackMatrix", runLiveSignedOutPlaybackMatrix)
    inputs.property("runLiveInnerTubePlaybackMatrix", runLiveInnerTubePlaybackMatrix)
    inputs.property("innerTubeSignedOut", innerTubeSignedOut)
    inputs.property("failLiveInnerTubeClientFailures", failLiveInnerTubeClientFailures)
    inputs.property("innerTubeBenchmarkIterations", innerTubeBenchmarkIterations)
    inputs.property("innerTubeStreamDelaySeconds", innerTubeStreamDelaySeconds)
    inputs.property("innerTubeGroupDelaySeconds", innerTubeGroupDelaySeconds)
    inputs.property("innerTubePlaybackCaseDelaySeconds", innerTubePlaybackCaseDelaySeconds)
    inputs.property("innerTubePlaybackUrls", innerTubePlaybackUrls)
    inputs.property("innerTubePlaybackClientFilter", innerTubePlaybackClientFilter)
    inputs.property("innerTubePoTokenProviderUrl", innerTubePoTokenProviderUrl)
    inputs.property("innerTubePoTokenProviderCapabilities", innerTubePoTokenProviderCapabilities)
    inputs.property("innerTubePlaybackStreamOrdinalFilter", innerTubePlaybackStreamOrdinalFilter)
    inputs.property("innerTubeClientFilter", innerTubeClientFilter)
    inputs.property("innerTubeContentTypeFilter", innerTubeContentTypeFilter)
    inputs.property("innerTubeStreamOrdinalFilter", innerTubeStreamOrdinalFilter)
    inputs.property("sabrVideoId", sabrVideoId)
    inputs.property("sabrUsePlaybackClock", sabrUsePlaybackClock)
    inputs.property("signedOutProbeVideoId", signedOutProbeVideoId)
    systemProperty("runLiveLyricsTests", runLiveLyricsTests.get())
    systemProperty(
        "runLiveSpotifyTests",
        providers
            .systemProperty("runLiveSpotifyTests")
            .orElse(providers.environmentVariable("RUN_LIVE_SPOTIFY_TESTS"))
            .orElse("false")
            .get(),
    )
    systemProperty(
        "liveUploadFile",
        providers
            .systemProperty("liveUploadFile")
            .orElse(providers.environmentVariable("LIVE_UPLOAD_FILE"))
            .orElse("")
            .get(),
    )
    systemProperty("runLiveInnerTubeClientTests", runLiveInnerTubeClientTests.get())
    systemProperty("runLiveInnerTubeClientBenchmarks", runLiveInnerTubeClientBenchmarks.get())
    systemProperty("runLiveSignedOutPlaybackMatrix", runLiveSignedOutPlaybackMatrix.get())
    systemProperty("runLiveInnerTubePlaybackMatrix", runLiveInnerTubePlaybackMatrix.get())
    systemProperty("innerTubeSignedOut", innerTubeSignedOut.get())
    systemProperty("failLiveInnerTubeClientFailures", failLiveInnerTubeClientFailures.get())
    systemProperty("innerTubeBenchmarkIterations", innerTubeBenchmarkIterations.get())
    systemProperty("innerTubeStreamDelaySeconds", innerTubeStreamDelaySeconds.get())
    systemProperty("innerTubeGroupDelaySeconds", innerTubeGroupDelaySeconds.get())
    systemProperty("innerTubePlaybackCaseDelaySeconds", innerTubePlaybackCaseDelaySeconds.get())
    systemProperty("innerTubePlaybackUrls", innerTubePlaybackUrls.get())
    systemProperty("innerTubePlaybackClientFilter", innerTubePlaybackClientFilter.get())
    systemProperty("innerTubePoTokenProviderUrl", innerTubePoTokenProviderUrl.get())
    systemProperty("innerTubePoTokenProviderCapabilities", innerTubePoTokenProviderCapabilities.get())
    systemProperty("innerTubePlaybackStreamOrdinalFilter", innerTubePlaybackStreamOrdinalFilter.get())
    systemProperty("innerTubeClientFilter", innerTubeClientFilter.get())
    systemProperty("innerTubeContentTypeFilter", innerTubeContentTypeFilter.get())
    systemProperty("innerTubeStreamOrdinalFilter", innerTubeStreamOrdinalFilter.get())
    systemProperty("sabrVideoId", sabrVideoId.get())
    systemProperty("sabrUsePlaybackClock", sabrUsePlaybackClock.get())
    systemProperty("signedOutProbeVideoId", signedOutProbeVideoId.get())
    systemProperty(
        "liveInnerTubeBenchmarkReportDir",
        layout.buildDirectory
            .dir("reports/live-innertube")
            .get()
            .asFile.absolutePath,
    )
    environment("RUN_LIVE_LYRICS_TESTS", runLiveLyricsTests.get())
    environment("RUN_LIVE_INNERTUBE_CLIENT_TESTS", runLiveInnerTubeClientTests.get())
    environment("RUN_LIVE_INNERTUBE_CLIENT_BENCHMARKS", runLiveInnerTubeClientBenchmarks.get())
    environment("RUN_LIVE_SIGNED_OUT_PLAYBACK_MATRIX", runLiveSignedOutPlaybackMatrix.get())
    environment("RUN_LIVE_INNERTUBE_PLAYBACK_MATRIX", runLiveInnerTubePlaybackMatrix.get())
    environment("INNERTUBE_PLAYBACK_CLIENT_FILTER", innerTubePlaybackClientFilter.get())
    environment("INNERTUBE_PO_TOKEN_PROVIDER_CAPABILITIES", innerTubePoTokenProviderCapabilities.get())
    environment("INNERTUBE_SIGNED_OUT", innerTubeSignedOut.get())
    environment("FAIL_LIVE_INNERTUBE_CLIENT_FAILURES", failLiveInnerTubeClientFailures.get())
    environment("INNERTUBE_CLIENT_FILTER", innerTubeClientFilter.get())
    environment("INNERTUBE_CONTENT_TYPE_FILTER", innerTubeContentTypeFilter.get())
    environment("INNERTUBE_STREAM_ORDINAL_FILTER", innerTubeStreamOrdinalFilter.get())
    if (
        runLiveInnerTubeClientTests.get() == "true" ||
        runLiveInnerTubeClientBenchmarks.get() == "true" ||
        runLiveSignedOutPlaybackMatrix.get() == "true" ||
        runLiveInnerTubePlaybackMatrix.get() == "true"
    ) {
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
        testLogging.showStandardStreams = true
    }
}
