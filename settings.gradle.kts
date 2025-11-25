plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "Channels"


include("async-await-eager")
include("common")
include("common:launch-semaphore")
include("launch-semaphore")
include("channel")