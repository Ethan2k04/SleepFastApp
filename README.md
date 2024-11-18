# Sleep Fast App

## **✨ Your reliable sleep tracker**

<img src="https://github.com/user-attachments/assets/c81e9319-e62e-43ce-bfaa-78c0a446f02f" alt="ic_app_icon" width="200" />

# About

It is an android app (unfinished) that monitor user's sleep behaviour (such as snore) and envaluate their sleep quality

# Preview

<img src="https://github.com/user-attachments/assets/c08b8215-82d7-4e5d-af9c-607efb2c76cf" alt="ic_app_icon" width="200" />

# Progress

**Currently Finished:**

- Sleep alarm
- Snore detection
- Basic UI

**To Be Done:**

- Sleep quality analysis
- Data visualization

# Dependencies

This project uses the following dependencies:

## AndroidX Libraries

- **Core KTX**: `implementation(libs.androidx.core.ktx)`
- **AppCompat**: `implementation(libs.androidx.appcompat)`
- **Material Components**: `implementation(libs.material)`
- **Activity**: `implementation(libs.androidx.activity)`
- **ConstraintLayout**: `implementation(libs.androidx.constraintlayout)`
- **SwipeRefreshLayout**: `implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")`

## Testing Libraries

- **JUnit**: `testImplementation(libs.junit)`
- **AndroidX JUnit**: `androidTestImplementation(libs.androidx.junit)`
- **Espresso Core**: `androidTestImplementation(libs.androidx.espresso.core)`

## TensorFlow Lite

- **TensorFlow Lite Task Audio**: `implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")`

## Lifecycle

- **Lifecycle ViewModel KTX**: `implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")`

## Room

- **Room Runtime**: 
  ```groovy
  val room_version = "2.6.1"
  implementation("androidx.room:room-runtime:$room_version")
  annotationProcessor("androidx.room:room-compiler:$room_version")
  kapt("androidx.room:room-compiler:$room_version")
  implementation("androidx.room:room-ktx:$room_version")
