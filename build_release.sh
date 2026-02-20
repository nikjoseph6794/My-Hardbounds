#!/bin/bash
echo "Building Release Bundle (AAB)..."
./gradlew clean bundleRelease
echo "Done! Your AAB is located at:"
echo "app/build/outputs/bundle/release/app-release.aab"
echo "You can upload this file to the Google Play Console."
