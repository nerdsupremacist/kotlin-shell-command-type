# kotlin-shell-command-type-provider
Type Provider for shell commands inside of Kotlin Scripting

## Example

Use `@Command` to import a command that you can call from the [Shell DSL](https://github.com/jakubriegel/kotlin-shell).

### Move files

```kotlin
@file:Command("mv")

runBlocking {
  shell {
    mv {
      // Do not prompt for confirmation before overwriting
      +f

      +"sourceFile.kts"
      +"destination/folder/"
    }()
  }
}
```

### Compiling with Clang

```kotlin
@file:Command("clang")

val file = File("project/file.m")

runBlocking {
  shell {
    clang {
      // Treat input as Objective C
      +objC
      
      // Support Posix Threads
      +pthread

      +file
    }()
  }
}
```

### Build Docker

It even supports subcommands. For example with Docker:

```kotlin
@file:Command("docker")

runBlocking {
  shell {
    // Build Image
    docker.build {
      +noCache
      +compress
      
      +tag("MyImage")

      +"path/to/Dockerfile"
    }()
    
    // Run Image
    docker.run {
      +publish("8080:8080")
      
      +"MyImage"
    }
  }
}
```

## Usage

coming soon
