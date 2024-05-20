[![](https://jitpack.io/v/FeryaFox/WakaTimeApiWrapper.svg)](https://jitpack.io/#FeryaFox/WakaTimeApiWrapper)

# WakaTimeApiWrapper

## How to install

### Gradle
Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency
```gradle
dependencies {
        implementation 'com.github.FeryaFox:WakaTimeApiWrapper:1.1'
}
```

### Maven

Add the JitPack repository to your build file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency

```xml
<dependency>
    <groupId>com.github.FeryaFox</groupId>
    <artifactId>WakaTimeApiWrapper</artifactId>
    <version>1.1</version>
</dependency>
```

## Example Usage

### Auth

#### Api key

```java
ApiTokenAuth auth = new ApiTokenAuth("api_key");
```

#### OAuth

First you need to create an OAuth application https://wakatime.com/apps

```java
Scanner sc = new Scanner(System.in);

AccessTokenAuth auth = new AccessTokenAuth(
        "App ID",
        "App Secret",
        "Authorized Redirect URIs"
);
System.out.println(auth.getAuthorizationUrl());
auth.getAccessToken(sc.nextLine());
```

You must copy the received link, follow it, log in and then enter the received code

### Usage

Create Wrapper

```java
WakaTimeWrapper wrapper = new WakaTimeWrapper(auth);
```

Get All Time Since Today
```java
System.out.println(wrapper.getAllTimeSinceToday("Your Project"));
```

## Docs 

To get all available methods, go to the website https://wakatime.com/developers/
