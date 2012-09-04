# couch25k-android

This repository contains the android version of the couch25k mobile app.

## Prerequisites

couch25k is based on Android 2.2 (API Level 8), Google API (MapView), Touch DB (HEAD 70d89ec8e4d4f4b170a627ac4b9180b42e1b8701) and Ektorp.

* [TouchDB Android](https://github.com/couchbaselabs/TouchDB-Android/)
 * Module TouchDB Android
 * Module TouchDB Android Ektorp (O/R Mapper)

## Project Setup

couch25k-android was build with Eclipse Indigo and ADT 18.0.0. The .project file is included so a simple checkout will work. The dependant projects are included as libs, the revision is listed above. You might want to checkout the current revision of TouchDB to get the latest changes. Simply get the TouchDB projects from GitHub, delete all libraries from the `libs` folder and reference the three TouchDB projects as library projects (Android project properties).
*Just a little hint: It will only work if all 3 projects (couch25k-android and the TouchDB projects) are located in the same folder.*

## Google Maps Key

Google Maps requires a key to work. The key must match the one used for signing the APK. Getting your GoogleMaps key is pretty easy, simply [sign up here](https://developers.google.com/android/maps-api-signup?hl=de). Replace the value for field `android:apiKey` in the *layout/runmap.xml* file with your personal GoogleMaps key.

## Replication

couch25k-android replicates it's data with a remote CouchDB. The target couch is defined as constant `COUCH25K_REMOTE_DB` in class `org.couchto5k.service.RunLogService`. You need to set that entry before starting or packaging couch25-android. The easiest solution for a remote CouchDB is [Iris Couch](http://www.iriscouch.com/).