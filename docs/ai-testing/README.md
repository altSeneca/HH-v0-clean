# ONNX Gemma AI Testing Framework

A comprehensive testing framework for the ONNX Gemma local AI implementation in HazardHawk, ensuring reliable construction safety analysis capabilities.

## 🎯 Overview

This testing framework provides complete validation of the ONNX Gemma local AI model integration, covering:

- **Unit Tests** - Core functionality, model initialization, error handling
- **Integration Tests** - End-to-end pipeline, cross-platform compatibility  
- **Model Validation** - Accuracy testing against construction safety ground truth
- **Performance Testing** - Benchmarking, memory profiling, resource management
- **CI/CD Integration** - Automated testing and reporting

## 📁 Test Structure

```
shared/src/commonTest/kotlin/com/hazardhawk/ai/
├── ONNXGemmaAnalyzerTest.kt          # Unit tests for core analyzer
├── ONNXGemmaIntegrationTest.kt       # End-to-end integration tests  
├── ModelValidationTest.kt            # Model accuracy validation
├── PerformanceBenchmarkTest.kt       # Performance and memory tests
└── test/
    ├── ONNXTestDataModels.kt         # Test data structures
    ├── ConstructionSafetyDataset.kt  # Ground truth datasets
    └── EnhancedTestDataFactory.kt    # Test data generation

shared/src/commonTest/resources/test-data/
├── construction-safety/
│   ├── annotations/                  # Ground truth annotations
│   │   ├── scaffold-multi-hazards-1.json
│   │   ├── ppe-violations-1.json
│   │   └── ...
│   └── README.md                     # Dataset documentation
└── models/
    └── test_models_info.json         # Test model configurations

.github/workflows/
└── onnx-gemma-ai-tests.yml          # CI/CD pipeline configuration

scripts/
└── run-ai-tests.sh                  # Test automation script
```

## 🧪 Test Categories

### 1. Unit Tests (`ONNXGemmaAnalyzerTest.kt`)

**Purpose**: Validate core ONNX analyzer functionality

**Test Coverage**:
- ✅ Model initialization with valid/invalid paths
- ✅ Memory limit enforcement
- ✅ Image preprocessing and validation
- ✅ Error handling and graceful degradation
- ✅ Resource cleanup verification
- ✅ Concurrent processing safety
- ✅ Fallback mechanism validation

**Key Features**:
- Mock ONNX model testing
- Memory leak detection
- Thread safety validation
- Error boundary testing

### 2. Integration Tests (`ONNXGemmaIntegrationTest.kt`)

**Purpose**: Validate end-to-end AI processing pipeline

**Test Coverage**:
- ✅ Complete image-to-analysis workflow
- ✅ Batch processing efficiency
- ✅ Cross-platform compatibility
- ✅ Fallback integration with cloud services
- ✅ Different image formats and sizes
- ✅ Concurrent request handling
- ✅ Performance under load

**Key Features**:
- Real workflow simulation
- Multi-platform testing
- Load testing capabilities
- Integration with existing systems

### 3. Model Validation (`ModelValidationTest.kt`)

**Purpose**: Validate AI model accuracy against ground truth data

**Test Coverage**:
- ✅ Construction safety detection accuracy (>75% target)
- ✅ PPE violation detection precision
- ✅ OSHA compliance code mapping
- ✅ Confidence score calibration
- ✅ Challenging lighting conditions
- ✅ Partial occlusion handling
- ✅ Edge case scenario validation

**Ground Truth Dataset**:
- 12+ annotated construction site images
- Multiple hazard types and scenarios
- PPE compliance variations
- OSHA code mappings
- Performance benchmarks per image size

### 4. Performance Tests (`PerformanceBenchmarkTest.kt`)

**Purpose**: Ensure AI processing meets performance requirements

**Test Coverage**:
- ✅ Inference time benchmarking
  - HD (1920x1080): < 8 seconds
  - 4K (3840x2160): < 15 seconds  
  - Small (640x480): < 3 seconds
- ✅ Memory usage profiling (< 512MB peak)
- ✅ Memory leak detection
- ✅ Throughput testing
- ✅ Resource constraint handling
- ✅ Concurrent processing performance

**Performance Metrics**:
- Processing time per image size
- Memory usage patterns
- Throughput (images/second)
- Resource utilization
- Stability under load

## 🚀 Running Tests

### Quick Start

```bash
# Run all AI tests
./scripts/run-ai-tests.sh --level full

# Run specific test categories
./scripts/run-ai-tests.sh --level unit        # Fast unit tests only
./scripts/run-ai-tests.sh --level integration # Integration tests
./scripts/run-ai-tests.sh --level validation  # Model validation
./scripts/run-ai-tests.sh --level performance # Performance benchmarks
```

### Gradle Tasks

```bash
# Individual test categories
./gradlew testONNXGemmaUnit          # Unit tests
./gradlew testONNXGemmaIntegration   # Integration tests  
./gradlew testONNXGemmaValidation    # Model validation
./gradlew testONNXGemmaPerformance   # Performance tests

# Complete test suite
./gradlew testONNXGemmaAll           # All tests + report generation

# Generate test report
./gradlew generateAITestReport       # Comprehensive HTML report
```

### Advanced Options

```bash
# Enable memory monitoring
./scripts/run-ai-tests.sh --level performance --memory-monitoring

# Enable performance profiling
./scripts/run-ai-tests.sh --level validation --performance-profile

# Parallel execution
./scripts/run-ai-tests.sh --level full --parallel

# Verbose output
./scripts/run-ai-tests.sh --level unit --verbose
```

## 📊 Test Reports

The framework generates comprehensive HTML reports including:

- **Test Summary** - Pass/fail rates, execution times
- **Performance Metrics** - Inference times, memory usage
- **Model Accuracy** - Validation results against ground truth
- **Memory Analysis** - Usage patterns, leak detection
- **Recommendations** - Performance optimization suggestions

**Report Locations**:
- Main Report: `build/reports/ai-tests/ai-test-summary.html`
- Individual Reports: `build/reports/tests/*/index.html`
- Raw Results: `build/test-results/ai-tests/`

## 🔧 CI/CD Integration

### GitHub Actions

The framework includes a comprehensive CI/CD pipeline (`.github/workflows/onnx-gemma-ai-tests.yml`) that:

- ✅ Runs automatically on push/PR to main branches
- ✅ Supports manual triggering with configurable test levels
- ✅ Caches ONNX models for faster execution
- ✅ Monitors memory usage during tests
- ✅ Generates and publishes test results
- ✅ Creates performance analysis reports
- ✅ Provides failure notifications

### Trigger Conditions

- **Push to main**: Full validation suite
- **Push to develop**: Integration tests
- **Feature branches**: Unit tests only
- **Scheduled**: Nightly full test suite
- **Manual**: Configurable test level

### Environment Configuration

```yaml
# Environment variables
ONNX_MODEL_CACHE_PATH: ~/.cache/onnx-models
AI_TEST_RESULTS_PATH: ./build/test-results/ai-tests
GRADLE_OPTS: -Dorg.gradle.daemon=false
```

## 📋 Test Data Management

### Mock Models

The framework uses mock ONNX models for testing:

- **test_gemma_model.onnx** - Standard test model (125MB)
- **corrupted_model.onnx** - Corrupted model for error testing
- **large_test_model.onnx** - Large model for memory testing (512MB)

### Ground Truth Dataset

Comprehensive construction safety dataset with:

- **12+ Test Scenarios** - Various construction site situations
- **Detailed Annotations** - Hazard locations, OSHA codes, confidence levels
- **Performance Benchmarks** - Expected processing times per scenario
- **Multiple Categories** - Fall protection, PPE, electrical, housekeeping

### Test Data Generation

Programmatic test data generation for:
- Various image sizes and formats
- Construction site scenarios
- PPE compliance variations
- Hazard detection ground truth
- Performance benchmark data

## 🎛️ Configuration

### System Properties

```bash
# Model configuration
-Donnx.model.path=/path/to/models
-Dai.test.timeout=300000
-Dai.test.memory.limit=2048

# Test type selection
-Dtest.type=unit|integration|validation|performance|full
```

### Memory Configuration

```bash
# JVM settings for AI tests
-Xmx2g                          # Maximum heap size
-XX:+UseG1GC                    # G1 garbage collector
-XX:MaxGCPauseMillis=200        # GC pause time limit
```

### Performance Thresholds

```kotlin
// Configurable performance targets
val PERFORMANCE_TARGETS = mapOf(
    "hd_inference_time" to 8000L,      // 8 seconds max
    "4k_inference_time" to 15000L,     // 15 seconds max
    "memory_usage_peak" to 512,        // 512MB max
    "model_accuracy_min" to 0.75f,     // 75% minimum accuracy
    "throughput_target" to 0.2f        // 0.2 images/second
)
```

## 🔍 Debugging

### Test Failures

1. **Check Logs**: `build/test-results/ai-tests/ai-test-execution.log`
2. **Memory Issues**: Enable memory monitoring with `-m` flag
3. **Performance Issues**: Use performance profiling with `-f` flag
4. **Model Issues**: Verify mock models in `~/.cache/onnx-models/`

### Common Issues

- **Memory Exhaustion**: Increase heap size or reduce concurrent tests
- **Timeout Errors**: Increase test timeout or optimize test data
- **Model Loading Failures**: Check ONNX model cache and permissions
- **Platform Issues**: Verify cross-platform dependencies

## 🚀 Performance Optimization

### Test Execution

- Use parallel execution for independent tests
- Cache ONNX models to avoid repeated downloads
- Optimize test data size for faster execution
- Use filtered test execution for development

### Memory Management

- Monitor memory usage with built-in profiling
- Implement proper resource cleanup
- Use memory-efficient test data generation
- Configure appropriate JVM settings

## 📚 Best Practices

### Test Design

- ✅ **Isolate Tests** - Each test should be independent
- ✅ **Mock External Dependencies** - Use mock ONNX models
- ✅ **Validate Edge Cases** - Test error conditions and boundaries
- ✅ **Performance Aware** - Include timing and memory assertions
- ✅ **Cross-Platform** - Ensure tests work on all target platforms

### Maintenance

- ✅ **Update Ground Truth** - Keep test datasets current
- ✅ **Monitor Performance** - Track performance regression
- ✅ **Review Test Coverage** - Ensure comprehensive validation
- ✅ **Documentation** - Keep test documentation updated

### Development Workflow

1. **TDD Approach** - Write tests before implementing features
2. **Incremental Testing** - Add tests for new functionality
3. **Regression Prevention** - Include tests for bug fixes
4. **Performance Monitoring** - Continuous performance validation

## 🤝 Contributing

### Adding New Tests

1. **Unit Tests** - Add to `ONNXGemmaAnalyzerTest.kt`
2. **Integration Tests** - Add to `ONNXGemmaIntegrationTest.kt`
3. **Validation Tests** - Update ground truth datasets
4. **Performance Tests** - Add benchmarks to `PerformanceBenchmarkTest.kt`

### Test Data

1. **New Scenarios** - Add to `ConstructionSafetyDataset.kt`
2. **Ground Truth** - Create annotation files in JSON format
3. **Mock Models** - Update model configurations as needed

### Documentation

- Update this README for new features
- Add inline documentation for complex test logic
- Update CI/CD documentation for workflow changes

## 📞 Support

For technical support with the AI testing framework:

- **Issues**: Create GitHub issues for bugs or feature requests
- **Documentation**: Refer to inline code documentation
- **Performance**: Check performance analysis reports
- **Integration**: Review CI/CD pipeline configurations

---

**Framework Version**: 1.0.0  
**Last Updated**: January 2025  
**Maintained By**: HazardHawk AI Development Team