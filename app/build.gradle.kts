plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
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
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
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
