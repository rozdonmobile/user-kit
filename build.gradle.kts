plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  `maven-publish`
}

// The library *is* the root project, so the JitPack coordinates are the short
// form `com.github.rozdonmobile:user-kit:<tag>` — one line in a consumer, like
// adding the Swift package by its GitHub URL.
group = property("GROUP") as String
version = property("VERSION_NAME") as String

android {
  namespace = "io.github.rozd.userkit"
  compileSdk {
    version = release(37)
  }
  defaultConfig {
    minSdk = 26
    consumerProguardFiles("consumer-rules.pro")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

dependencies {
  // The whole reason this is a *Compose* user kit: `User.info` is snapshot state
  // and `LocalUser` is a CompositionLocal. Runtime only — no UI, no Material.
  // `api`, not `implementation`: the BOM must reach consumers, or the version-less
  // `compose-runtime` this module exposes cannot be resolved from an app.
  api(platform(libs.androidx.compose.bom))
  api(libs.androidx.compose.runtime)
  api(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = project.group.toString()
      artifactId = "user-kit"
      version = project.version.toString()
      afterEvaluate { from(components["release"]) }
      pom {
        name.set("UserKit for Compose")
        description.set("A provider-neutral current-user layer for Jetpack Compose.")
        url.set("https://github.com/rozdonmobile/user-kit")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        scm {
          url.set("https://github.com/rozdonmobile/user-kit")
        }
      }
    }
  }
}
