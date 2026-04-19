dependencies {
    api(project(":chert-client"))
    annotationProcessor("io.micronaut:micronaut-inject-java:4.8.2")
    api("io.micronaut:micronaut-inject:4.8.2")
    api("io.micronaut:micronaut-runtime:4.8.2")
    api("io.micronaut.discovery:micronaut-discovery-client:4.5.1")
    api("jakarta.inject:jakarta.inject-api:2.0.1")
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")
    api("org.slf4j:slf4j-api:2.0.12")
    
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:4.8.2")
    testImplementation("io.micronaut.test:micronaut-test-junit5:4.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.25.3")
}
