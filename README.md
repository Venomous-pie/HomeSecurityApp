# Smart Home Security Control Hub

A mobile application for managing home security devices, including camera streaming between devices.

## Features

- Set up devices as cameras or monitors
- Stream live video from camera devices to monitor devices
- Control and monitor multiple cameras
- Secure connection between devices

## Setup

### Camera Device Setup

1. Install the app on the device you want to use as a camera
2. Choose the "Camera" role during setup
3. Grant camera and microphone permissions
4. Start streaming from the camera screen

### Monitor Device Setup

1. Install the app on the device you want to use as a monitor
2. Choose the "Monitor" role during setup
3. Select the camera you want to view from the available cameras list
4. Connect to the camera to view the live stream

## WebRTC Streaming

The app uses WebRTC for real-time video streaming between devices, with signaling handled through Firebase Realtime Database.

### Important Note

For production use, you need to:

1. Create a Firebase project
2. Add your Android app to the Firebase project
3. Download the `google-services.json` file and replace the placeholder in the app directory
4. Enable the Realtime Database in your Firebase project

## Permissions

The app requires the following permissions:
- Camera
- Microphone
- Internet

## Building from Source

1. Clone the repository
2. Set up Firebase and replace the `google-services.json` file
3. Build using Android Studio or Gradle:
   ```
   ./gradlew assembleDebug
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details. 