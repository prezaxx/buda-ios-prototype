plugins {
 alias(libs.plugins.kotlinMultiplatform)
 alias(libs.plugins.composeMultiplatform)
 alias(libs.plugins.composeCompiler)
 alias(libs.plugins.kotlin.serialization)
}
kotlin {
 jvm()
 listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
  target.binaries.framework { baseName = "ComposeApp"; isStatic = true }
 }
 sourceSets {
  commonMain.dependencies {
   implementation(libs.compose.runtime)
   implementation(libs.compose.foundation)
   implementation(libs.compose.material3)
   implementation(libs.compose.ui)
   implementation(libs.kotlinx.coroutines.core)
   implementation(libs.kotlinx.datetime)
   implementation(libs.kotlinx.serialization.json)
   implementation(libs.ktor.core)
   implementation(libs.ktor.client.content.negotiation)
   implementation(libs.ktor.serialization.kotlinx.json)
   implementation(libs.whyoleg.crypto.core)
   implementation(libs.ksoup)
   implementation(libs.urlencoder)
  }
  iosMain.dependencies { implementation(libs.ktor.darwin); implementation(libs.whyoleg.crypto.apple) }
  jvmMain.dependencies { implementation(libs.ktor.okhttp); implementation(libs.whyoleg.crypto.jdk) }
  commonTest.dependencies { implementation("io.ktor:ktor-client-mock:3.4.1"); implementation(libs.kotlin.test); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2") }
 }
}
