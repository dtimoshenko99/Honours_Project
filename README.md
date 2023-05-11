# Honours_Project
A GitHub repo to hold Honours project code

The following dependencies were used:

```
dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0") {
            because("kotlin-stdlib-jdk7 is now a part of kotlin-stdlib")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0") {
            because("kotlin-stdlib-jdk8 is now a part of kotlin-stdlib")
        }
    }
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.firebase:firebase-database:20.2.1'
    implementation 'com.google.firebase:firebase-auth:22.0.0'
    implementation 'com.facebook.android:facebook-android-sdk:latest.release'
    implementation 'com.google.android.ads:mediation-test-suite:3.0.0'
    implementation 'com.google.android.gms:play-services-auth:20.5.0'
    implementation "androidx.browser:browser:1.5.0"
    implementation 'com.google.firebase:firebase-firestore:24.6.0'
    implementation platform('com.google.firebase:firebase-bom:31.5.0')
    implementation 'com.google.firebase:firebase-messaging:23.1.2'
    implementation 'androidx.annotation:annotation:1.6.0'
    testImplementation 'junit:junit:4.13.2'
    implementation "com.gorisse.thomas.sceneform:sceneform:1.21.0"
}
```
