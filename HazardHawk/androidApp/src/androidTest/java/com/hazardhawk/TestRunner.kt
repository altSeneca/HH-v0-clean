package com.hazardhawk

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test Suite Runner for HazardHawk AI Workflow Testing
 * 
 * Executes all AI workflow tests in the correct order:
 * 1. Unit tests (state management, core logic)
 * 2. UI integration tests (dialog workflow)  
 * 3. End-to-end tests (complete workflows)
 * 4. Performance tests (construction site requirements)
 * 5. Usability tests (construction worker needs)
 * 
 * Run with: ./gradlew :androidApp:connectedAndroidTest
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Unit Tests - Core AI state management
    CameraScreenAITests::class,
    
    // UI Integration Tests - Dialog and component integration
    AIWorkflowIntegrationTests::class,
    
    // End-to-End Tests - Complete user workflows
    AIWorkflowEndToEndTests::class,
    
    // Performance Tests - Construction site performance requirements
    AIPerformanceReliabilityTests::class,
    
    // Usability Tests - Construction worker field conditions
    ConstructionWorkerUsabilityTests::class
)
class HazardHawkAIWorkflowTestSuite

/**
 * Quick test runner for development - runs critical tests only
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CameraScreenAITests::class,
    AIWorkflowIntegrationTests::class
)
class QuickAIWorkflowTestSuite

/**
 * Performance test runner - runs performance and reliability tests only
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AIPerformanceReliabilityTests::class,
    ConstructionWorkerUsabilityTests::class
)
class PerformanceTestSuite