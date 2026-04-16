import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0"
    signing
}

group = "io.github.david-auk"

val rawVersion = System.getenv("VERSION")
    ?: System.getenv("GITHUB_REF_NAME")
    ?: "0.0.0-SNAPSHOT"

version = rawVersion.removePrefix("v.")
    .removePrefix("v")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.21.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
    dependsOn(tasks.matching { it.name == "plainJavadocJar" })
}

mavenPublishing {
    coordinates(group.toString(), "youtube-scraper", version.toString())

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = true
        )
    )

    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("youtube-scraper")
        description.set("A Java library for scraping and mapping YouTube video metadata into clean domain objects.")
        url.set("https://github.com/david-auk/youtube-scraper")

        licenses {
            license {
                name.set("MIT")
                url.set("https://mit-license.org/")
            }
        }

        developers {
            developer {
                id.set("david-auk")
                name.set("David Aukes")
                url.set("https://github.com/david-auk")
            }
        }

        scm {
            url.set("https://github.com/david-auk/youtube-scraper")
            connection.set("scm:git:https://github.com/david-auk/youtube-scraper.git")
            developerConnection.set("scm:git:ssh://git@github.com/david-auk/youtube-scraper.git")
        }
    }
}

val envSigningKey: String? = System.getenv("SIGNING_KEY")
val envSigningPass: String? = System.getenv("SIGNING_PASSWORD")
if (!envSigningKey.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(envSigningKey, envSigningPass)
    }
} else {
    logger.lifecycle("PGP signing not configured from env; if this is CI, set SIGNING_KEY and SIGNING_PASSWORD secrets.")
}