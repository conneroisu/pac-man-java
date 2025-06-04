# Pac-Man Java Refactoring Plan

## Overview

This document outlines the major refactoring efforts needed to address the remaining 130 PMD violations in the Pac-Man Java codebase. The violations fall into several categories requiring different levels of effort.

## Current Status

✅ **Fixed (23 violations addressed):**
- Empty catch blocks
- Constructor calling overridable methods
- SystemPrintln violations (suppressed appropriately)
- Serialization issues
- GuardLogStatement violations
- Generic exception catching
- Boolean return simplification
- Unused variables
- Duplicate literals

🔴 **Remaining (130 violations):**
- Design complexity issues (God classes, excessive methods/complexity)
- Test class structure violations
- Architectural design patterns

## High Priority Refactoring (Critical Design Issues)

### 1. ActorImpl.java - God Class Refactoring
**Violations:** GodClass, TooManyMethods, TooManyFields, CyclomaticComplexity, ExcessiveMethodLength

**Current Issues:**
- WMC (Weighted Method Count): 147
- 20+ methods in single class
- Multiple responsibilities: movement, pathfinding, logging, state management

**Refactoring Strategy:**
```
ActorImpl (Base Class)
├── MovementController
│   ├── calculateNextCellLocation()
│   ├── getNextLocation()
│   └── handleDirectionChange()
├── PathfindingEngine
│   ├── calculatePathToTarget()
│   ├── findBestDirection()
│   └── evaluateDirections()
├── StateManager
│   ├── handleModeChange()
│   ├── handleFrightenedMode()
│   └── updatePosition()
└── GhostLogger
    ├── logMovement()
    ├── initializeLogging()
    └── closeLogging()
```

**Implementation Steps:**
1. Extract PathfindingEngine interface with implementations
2. Create MovementController for position/direction logic
3. Extract StateManager for mode and state transitions
4. Create GhostLogger utility class
5. Refactor ActorImpl to use composition pattern
6. Update all ghost subclasses (Blinky, Pinky, Inky, Clyde)

**Estimated Effort:** 3-4 days

### 2. PacmanGame.java - God Class Refactoring
**Violations:** GodClass, TooManyMethods, NcssCount, CognitiveComplexity

**Current Issues:**
- WMC: 78, ATFD: 35
- Handles game state, collision detection, scoring, enemy management

**Refactoring Strategy:**
```
PacmanGame (Game Controller)
├── GameStateManager
│   ├── resetGame()
│   ├── updateGameState()
│   └── checkWinConditions()
├── CollisionDetector
│   ├── checkCollisions()
│   ├── handleGhostCollision()
│   └── handlePelletCollection()
├── ScoreManager
│   ├── updateScore()
│   ├── calculateBonus()
│   └── trackStatistics()
└── EnemyManager
    ├── spawnEnemies()
    ├── updateEnemies()
    └── manageGhostStates()
```

**Implementation Steps:**
1. Extract collision detection logic
2. Create separate score management system
3. Extract enemy management responsibilities
4. Implement game state pattern
5. Reduce constructor complexity through builder pattern

**Estimated Effort:** 2-3 days

### 3. Pacman.java Player Class Refactoring
**Violations:** GodClass, CyclomaticComplexity, NPathComplexity

**Current Issues:**
- Complex tryTurn() method (cyclomatic complexity: 30)
- Mixed responsibilities for input handling and movement

**Refactoring Strategy:**
```
Pacman (Player Entity)
├── InputHandler
│   ├── processInput()
│   ├── validateDirection()
│   └── queueDirection()
├── PlayerMovement
│   ├── tryTurn()
│   ├── handleTurn()
│   └── updatePosition()
└── PlayerState
    ├── updateAnimation()
    ├── handlePowerUp()
    └── resetPlayer()
```

**Implementation Steps:**
1. Extract input handling logic
2. Simplify tryTurn() method using strategy pattern
3. Create separate animation handler
4. Implement command pattern for direction changes

**Estimated Effort:** 1-2 days

## Medium Priority Refactoring (Test Framework Issues)

### 4. Test Class Structure Violations
**Affected Files:** 
- GhostActivationTest.java
- GhostBehaviorTest.java  
- GhostContinuousMovementTest.java
- GhostMovementTest.java
- GhostPositionTest.java
- GhostSpawnTest.java
- GhostTargetingTest.java
- GhostWallCollisionTest.java
- MazeNavigationTest.java
- SimpleTest.java

**Issues:**
- TestClassWithoutTestCases violations
- ExcessiveMethodLength in main() methods
- High cognitive complexity in test logic

**Refactoring Strategy:**
1. **Convert to JUnit Framework:**
   ```java
   public class GhostBehaviorTest {
       @Test
       public void testBlinkyTargeting() { /* ... */ }
       
       @Test
       public void testPinkyAmbush() { /* ... */ }
       
       @Test
       public void testInkyFlanking() { /* ... */ }
       
       @Test
       public void testClydeDistance() { /* ... */ }
   }
   ```

2. **Extract Test Utilities:**
   ```java
   public class TestUtils {
       public static PacmanGame createTestGame(String[] maze, int frameRate) { /* ... */ }
       public static void assertGhostPosition(Actor ghost, int row, int col) { /* ... */ }
       public static void runFrames(PacmanGame game, int count) { /* ... */ }
   }
   ```

3. **Break Down Complex Tests:**
   - Split large main() methods into focused test methods
   - Extract setup/teardown logic
   - Create parameterized tests for multiple scenarios

**Implementation Steps:**
1. Add JUnit 5 dependency to pom.xml
2. Create TestUtils class for common functionality
3. Convert each test class systematically
4. Refactor complex test logic into smaller methods
5. Add proper assertions and test documentation

**Estimated Effort:** 2-3 days

### 5. SimulationTestFramework.java Refactoring
**Violations:** CognitiveComplexity, CyclomaticComplexity, NPathComplexity

**Current Issues:**
- Complex simulation logic in single methods
- Mixed responsibilities for simulation and validation

**Refactoring Strategy:**
```java
public class SimulationTestFramework {
    private final SimulationRunner runner;
    private final AssertionEngine assertionEngine;
    private final TestReporter reporter;
    
    public SimulationResult runSimulation(String testName, SimulationConfig config) {
        // Delegated to SimulationRunner
    }
    
    public void validateResults(SimulationResult result) {
        // Delegated to AssertionEngine
    }
}

public class AssertionEngine {
    public void assertNoInappropriateStopping(List<GhostFrame> trace) { /* ... */ }
    public void assertNoWallIntersection(List<GhostFrame> trace) { /* ... */ }
    public void assertWithinMazeBounds(List<GhostFrame> trace) { /* ... */ }
    public void assertConsistentMovementSpeed(List<GhostFrame> trace) { /* ... */ }
    public void assertDeadGhostsReturnHome(List<GhostFrame> trace) { /* ... */ }
    public void assertNoInfiniteLoops(List<GhostFrame> trace) { /* ... */ }
}
```

**Implementation Steps:**
1. Extract simulation execution logic
2. Create dedicated assertion classes
3. Implement result reporting system
4. Add configuration validation
5. Create fluent assertion API

**Estimated Effort:** 1-2 days

## Low Priority Refactoring (Minor Complexity Issues)

### 6. Individual Ghost Classes
**Files:** Blinky.java, Pinky.java, Inky.java, Clyde.java
**Issues:** CognitiveComplexity in getTargetLocation() methods

**Refactoring Strategy:**
1. Extract targeting algorithms into strategy classes
2. Simplify conditional logic using lookup tables
3. Create shared targeting utilities

**Estimated Effort:** 1 day

### 7. UI Test Classes Complexity
**Files:** ComprehensiveGhostTesting.java, GhostStuckAnalysis.java
**Issues:** God class violations, excessive method length

**Refactoring Strategy:**
1. Extract test scenarios into separate classes
2. Create test data builders
3. Implement page object pattern for UI interactions

**Estimated Effort:** 1-2 days

## Implementation Priority

### Phase 1: Core Architecture (Week 1-2)
1. ActorImpl.java refactoring (highest impact)
2. PacmanGame.java refactoring
3. Pacman.java refactoring

### Phase 2: Testing Framework (Week 3)
1. Convert test classes to JUnit
2. Extract test utilities
3. SimulationTestFramework refactoring

### Phase 3: Polish (Week 4)
1. Individual ghost class improvements
2. UI test class refactoring
3. Code review and integration testing

## Benefits of Refactoring

### Code Quality Improvements:
- **Maintainability:** Smaller, focused classes easier to understand and modify
- **Testability:** Separated concerns enable better unit testing
- **Extensibility:** New ghost types or game features easier to add
- **Debugging:** Isolated responsibilities make bug tracking simpler

### Performance Benefits:
- **Memory Usage:** Reduced object coupling and better lifecycle management
- **CPU Usage:** More efficient algorithms through specialized classes
- **Garbage Collection:** Better object lifecycle management

### Development Benefits:
- **Team Collaboration:** Multiple developers can work on different components
- **Code Reviews:** Smaller, focused changes easier to review
- **Feature Development:** New features can be added without modifying core logic
- **Bug Fixes:** Issues isolated to specific components

## Architectural Patterns to Implement

### 1. Strategy Pattern
- Ghost targeting algorithms
- Movement strategies
- Collision handling

### 2. Observer Pattern
- Game state changes
- Score updates
- Ghost mode transitions

### 3. Command Pattern
- Player input handling
- Undo/redo functionality
- Game actions

### 4. Factory Pattern
- Ghost creation
- Test scenario generation
- Game configuration

### 5. State Pattern
- Ghost modes (Chase, Scatter, Frightened, Dead)
- Game states (Playing, Paused, GameOver)

## Testing Strategy

### Unit Testing:
- Test each extracted class independently
- Mock dependencies using Mockito
- Achieve >90% code coverage

### Integration Testing:
- Test component interactions
- Validate game logic end-to-end
- Performance regression testing

### Acceptance Testing:
- Verify game behavior matches original
- Test all ghost AI scenarios
- Execute all tests successfully
- Validate user interface interactions

## Risk Mitigation

### Regression Risk:
- Maintain comprehensive test suite
- Use feature flags for gradual rollout
- Keep original implementation as reference

### Performance Risk:
- Benchmark before and after refactoring
- Profile memory usage and CPU performance
- Optimize hot paths identified during refactoring

### Timeline Risk:
- Implement in phases with working code at each stage
- Prioritize high-impact, low-risk changes first
- Plan for additional testing time

## Success Metrics

### Quantitative Goals:
- Reduce PMD violations by 80% (from 130 to <26)
- Achieve cyclomatic complexity <10 for all methods
- Maintain <100ms frame update time
- Achieve >90% test coverage

### Qualitative Goals:
- Improved code readability and documentation
- Easier feature development and bug fixes
- Better separation of concerns
- More maintainable test suite

## Conclusion

This refactoring plan addresses the core architectural issues in the Pac-Man Java codebase. The phased approach ensures minimal disruption while achieving significant improvements in code quality, maintainability, and extensibility. The estimated total effort is 3-4 weeks for a complete implementation, with immediate benefits visible after Phase 1 completion.
