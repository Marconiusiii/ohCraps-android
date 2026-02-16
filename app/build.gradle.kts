import java.util.Properties

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
	if (keystorePropertiesFile.exists()) {
		keystorePropertiesFile.inputStream().use { load(it) }
	}
}
val isReleaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
	taskName.contains("release", ignoreCase = true)
}

android {
	namespace = "com.marconius.ohcraps"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.marconius.ohcraps"
		minSdk = 23
		targetSdk = 36
		versionCode = 1
		versionName = "1.0.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		if (keystorePropertiesFile.exists()) {
			create("release") {
				storeFile = file(keystoreProperties.getProperty("storeFile"))
				storePassword = keystoreProperties.getProperty("storePassword")
				keyAlias = keystoreProperties.getProperty("keyAlias")
				keyPassword = keystoreProperties.getProperty("keyPassword")
			}
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			if (keystorePropertiesFile.exists()) {
				signingConfig = signingConfigs.getByName("release")
			} else if (isReleaseTaskRequested) {
				throw GradleException(
					"Missing keystore.properties at project root. Copy keystore.properties.example and set your upload key values before running release tasks."
				)
			}
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
}

dependencies {

	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.appcompat:appcompat:1.7.0")

	implementation("com.google.android.material:material:1.12.0")

	implementation("androidx.constraintlayout:constraintlayout:2.1.4")
	implementation("androidx.recyclerview:recyclerview:1.3.2")

	implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
	implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

	implementation("org.jsoup:jsoup:1.17.2")
}
