# PacMan Java

This is a simple Pac-Man game implementation in Java.

## Build & Run Instructions

### Using Maven (Recommended)

This project uses Maven for dependency management and build automation.

#### Prerequisites
- Java JDK 17 or later
- Maven (or use the provided Maven wrapper)

#### Commands

```bash
# Compile the project
./mvnw compile

# Package the project into a JAR file
./mvnw package

# Run the game
./mvnw exec:java
```

### Using Nix (Alternative)

If you have Nix installed, you can use the provided flake.nix:

```bash
# Enter the development shell
nix develop

# Then you can use the following commands:
run           # Compile and run the game with Java
mvn-clean     # Clean Maven build artifacts
mvn-compile   # Compile the project with Maven
mvn-package   # Package the project into a JAR file with Maven
mvn-run       # Run the game with Maven
```

### Manual Build (Legacy)

```bash
# Compile
javac -d bin src/**/*.java

# Run main game
java -cp bin ui.RunGame

# Run tests
java -cp bin ui.SimpleTest
```

## Project Structure

- `src/api/` - Core game API interfaces and classes
- `src/hw4/` - Implementation of game actors (Pacman, ghosts)
- `src/ui/` - User interface classes and test runners