# GFIPhotoPicker
A library that implements photo picking capabilities for Gallery, Facebook, and Instagram on your Android application.
This library is experimental for now and is based on the image picker library [Louvre](https://github.com/andremion/Louvre).
This library still can't replace Louvre for **device only** image picker.
For now, only **Gallery** and **Instagram** are available to use.


## Installation
Add this in your **project** `build.gradle` file (not your **app** module `build.gradle` file):
```xml
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.4'
        classpath 'com.github.dcendents:android-maven-gradle-plugin:1.4.1'
    }
}

allprojects {
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
}
```


Then, add the library in your **app** module `build.gradle` file:
```xml
dependencies {
    compile 'com.github.potatodealer:gfiphotopicker:0.0.1'
}
```


## Usage
### 1. Add Style
Choose one of the **GFIPhotoPicker** themes to use in the `PhotoPickerActivity` and override it to define your app color palette.
```xml
<style name="AppTheme.YourApp.Light.DarkActionBar" parent="GFIPhotoPicker.Theme.Light.DarkActionBar">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>
```
```xml
<style name="AppTheme.YourApp.Dark" parent="GFIPhotoPicker.Theme.Dark">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>
```
```xml
<style name="AppTheme.YourApp.Light" parent="GFIPhotoPicker.Theme.Light">
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>
```

For all `PreviewActivity` you just need to define the accent color.
```xml
<style name="AppTheme.YourApp.Preview" parent="GFIPhotoPicker.Theme.Preview">
    <item name="colorAccent">@color/colorAccent</item>
</style>
```

### 2. Add to Manifest
Add `INTERNET` and `READ_EXTERNAL_STORAGE` permission in your `AndroidManifest.xml` file.
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

Declare the **GFIPhotoPicker** activities and provider in `AndroidManifest.xml` file using your new app themes.
```xml
<activity
    android:name="com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity"
    android:parentActivityName=".MainActivity"
    android:theme="@style/AppTheme.YourApp.Light.DarkActionBar"/>
<activity
    android:name="com.github.potatodealer.gfiphotopicker.activity.InstagramLoginActivity"
    android:theme="@style/AppTheme.YourApp.Light.DarkActionBar"/>
<activity
    android:name="com.github.potatodealer.gfiphotopicker.activity.GalleryPreviewActivity"
    android:theme="@style/AppTheme.YourApp.Preview"/>
<activity
    android:name="com.github.potatodealer.gfiphotopicker.activity.InstagramPreviewActivity"
    android:theme="@style/AppTheme.YourApp.Preview"/>
<provider
    android:authorities="com.github.potatodealer.gfiphotopicker.data"
    android:name="com.github.potatodealer.gfiphotopicker.data.InstagramProvider"
    android:enabled="true"
    android:exported="true"/>
```

### 3. Declare in Application
In your `Activity` define your **Instagram Client ID** and **Redirect URI** as well as your **Request Code** and `List<Uri>` to put the selection result, below is an example.
```java
private static final String INSTAGRAM_CLIENT_ID = "YOUR_INSTAGRAM_CLIENT_ID";
private static final String INSTAGRAM_REDIRECT_URI = "YOUR_INSTAGRAM_REDIRECT_URI";
private static final int YOUR_REQUEST_CODE = 777; //can be any number
private static List<Uri> mSelection;
private static List<Uri> mInstagramSelection;
```

To open **GFIPhotoPicker** you just need to add the code below to your `Activity`.
```java
GFIPhotoPicker.init(myActivity)
                .setInstagramClientId(INSTAGRAM_CLIENT_ID)
                .setInstagramRedirectUri(INSTAGRAM_REDIRECT_URI)
                .setRequestCode(YOUR_REQUEST_CODE)
                .setMaxSelection(10)
                .setSelection(mSelection)
                .setInstagramSelection(mInstagramSelection)
                .open();
```

You can also use a `Fragment` to open **GFIPhotoPicker**. In this case, the `Fragment` will get the `onActivityResult` callback.
```java
GFIPhotoPicker.init(myFragment)
                .setInstagramClientId(INSTAGRAM_CLIENT_ID)
                .setInstagramRedirectUri(INSTAGRAM_REDIRECT_URI)
                .setRequestCode(YOUR_REQUEST_CODE)
                .setMaxSelection(10)
                .setSelection(mSelection)
                .setInstagramSelection(mInstagramSelection)
                .open();
```

On your `Activity` or `Fragment` get the selection result on the `onActivityResult` method.
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        mSelection = PhotoPickerActivity.getSelection(data);
        mInstagramSelection = PhotoPickerActivity.getInstagramSelection(data);
        // or implement your own code
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

For sample implementation, fork the repo or download the master zip file.
