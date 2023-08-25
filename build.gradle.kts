import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.6.7"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("org.unbroken-dome.test-sets") version "4.0.0"
	id("com.palantir.git-version") version "0.15.0"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
	kotlin("plugin.jpa") version "1.6.21"
}

val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

group = "su.vshk"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

testSets {
	create("integrationTest")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.telegram:telegrambots:6.1.0")
	implementation("org.liquibase:liquibase-core:4.10.0")
	implementation("mysql:mysql-connector-java:8.0.29")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
	testImplementation("org.testcontainers:testcontainers:1.17.2")
	testImplementation("org.testcontainers:mysql:1.17.2")
	testImplementation("org.testcontainers:junit-jupiter:1.17.2")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
