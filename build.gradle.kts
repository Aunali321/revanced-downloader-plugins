import com.android.build.gradle.AppExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

subprojects {
    apply(plugin = "com.android.application")
    apply(plugin = "org.jetbrains.kotlin.android")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

dependencies {
        "compileOnly"(rootProject.libs.plugin.api)
}

    configure<com.android.build.gradle.AppExtension> {
        compileSdkVersion(35)

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionName = version.toString()
        versionCode = versionName!!.filter { it.isDigit() }.toInt()
    }

    buildTypes {
            getByName("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )

                val keystoreFile = file("${rootDir}/keystore.jks")
                signingConfig = if (keystoreFile.exists()) {
                    signingConfigs.create("release") {
                        storeFile = keystoreFile
                        storePassword = System.getenv("KEYSTORE_PASSWORD")
                        keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                        keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                    }
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    applicationVariants.all {
        outputs.all {
            this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                outputFileName = "${project.name}-$version.apk"
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
    }
}

    tasks.register("assembleReleaseSignApk") {
        dependsOn("assembleRelease")

        val apk = layout.buildDirectory.file("outputs/apk/release/${project.name}-$version.apk")

        inputs.file(apk).withPropertyName("input")
        outputs.file(apk.map { it.asFile.resolveSibling("${it.asFile.name}.asc") })

        doLast {
            project.configure<SigningExtension> {
                useGpgCmd()
                sign(*inputs.files.files.toTypedArray())
            }
        }
    }

    tasks.named("publish") {
        dependsOn("assembleReleaseSignApk")
    }
}
