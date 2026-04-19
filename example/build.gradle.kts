val mainSourceSet = the<org.gradle.api.tasks.SourceSetContainer>()["main"]

dependencies {
    implementation(project(":chert-client"))
    implementation(project(":chert-spring-boot-starter"))
    implementation(project(":chert-micronaut-starter"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    annotationProcessor("io.micronaut:micronaut-inject-java:4.8.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.register<org.gradle.api.tasks.JavaExec>("runDirectClientExample") {
    group = "application"
    description = "Runs the direct client example against a running chert-server instance."
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("io.vacivor.chert.example.client.DirectClientExample")
}

tasks.register<org.gradle.api.tasks.JavaExec>("runStarterExample") {
    group = "application"
    description = "Runs the Spring Boot starter example."
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("io.vacivor.chert.example.starter.StarterExampleApplication")
}

tasks.register<org.gradle.api.tasks.JavaExec>("runMicronautExample") {
    group = "application"
    description = "Runs the Micronaut starter example."
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("io.vacivor.chert.example.micronaut.MicronautExampleApplication")
}
