plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij")  version "1.17.3"
}

group = "com.plugin"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("https://www.jetbrains.com/intellij-repository/releases")  // 主仓库
    }
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")  // 历史版本仓库
    }
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html

intellij {
    version = "2024.1.7"          // 精确到第三位版本号
    type = "IC"                   // 社区版基础环境
    plugins.set(listOf("com.intellij.java"))  // 必须的官方模块依赖
    updateSinceUntilBuild = false // 关闭版本范围限制
}


dependencies {
    implementation("com.alibaba:dashscope-sdk-java:2.20.3")
    implementation("org.commonmark:commonmark:0.19.0")
    implementation("com.alibaba:fastjson:1.2.55")
    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("cn.bigmodel.openapi:oapi-java-sdk:release-V4-2.3.2")
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.encoding  = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<Test> {
        systemProperty("file.encoding",  "UTF-8")
    }

    withType<Javadoc> {
        options.encoding  = "UTF-8"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    runIde {
        // 推荐添加内存参数（与vmoptions文件保持一致）
        jvmArgs("-Xms512m", "-Xmx2048m")
        jvmArgs("-Dide.win.frame.auto.update=false")

    }
}
