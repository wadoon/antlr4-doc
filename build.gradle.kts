import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("idea")
}

group = "com.github.wadoon"
version = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
}

val antlr4 by configurations.creating

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.antlr:antlr4-runtime:4.7.1")
    implementation("com.github.ajalt:clikt:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")
    implementation("com.atlassian.commonmark:commonmark:0.13.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.13.0")
    antlr4("org.antlr:antlr4:4.7.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "com.github.wadoon.antlr4doc.Tool"
}


/*tasks.getByName<Shadow>("shadowJar") {
    archiveClassifier = "exe"
    archiveBaseName = "key"
}*/

val antlr4Output = "$projectDir/build/generated-src/antlr4/main/org/antlr/parser/antlr4"
tasks.create<JavaExec>("runAntlr4") {
    //see incremental task api, prevents rerun if nothing has changed.
    inputs.dir("src/main/antlr4/")
    outputs.dir("$projectDir/build/generated/antlr4/main/")
    classpath = antlr4
    main = "org.antlr.v4.Tool"
    args = listOf(
        "-visitor",
        "-Xexact-output-dir", "-o", antlr4Output,
        "-package", "org.antlr.parser.antlr4",
        "src/main/antlr4/LexBasic.g4",
        "src/main/antlr4/ANTLRv4Lexer.g4", "src/main/antlr4/ANTLRv4Parser.g4"
    )
    doFirst {
        file(antlr4Output).mkdirs()
        println("create $antlr4Output")
    }
}

sourceSets {
    main {
        java.srcDir("$projectDir/build/generated-src/antlr4/main")
    }
}

tasks.create<JavaExec>("runExample") {
    dependsOn(tasks.getByName("classes"))
    classpath = sourceSets["main"].runtimeClasspath

    group = "applicaton"
    main = application.mainClassName
    args = listOf("--complete-html", "-o", "examples/Parser.html", "examples/Parser.g4")
}

tasks.getByName("compileKotlin").dependsOn(tasks.getByName("runAntlr4"))
tasks.getByName("compileJava").dependsOn(tasks.getByName("runAntlr4"))
