# Build Fix Notes

## Issue: RestTemplateBuilder not found

### Error Message
```
[ERROR] /app/src/main/java/com/java6/springboot/internflow/scheduler/KeepAliveScheduler.java:[5,43] 
package org.springframework.boot.web.client does not exist

[ERROR] /app/src/main/java/com/java6/springboot/internflow/scheduler/KeepAliveScheduler.java:[43,31] 
cannot find symbol
  symbol:   class RestTemplateBuilder
  location: class com.java6.springboot.internflow.scheduler.KeepAliveScheduler
```

### Root Cause
`RestTemplateBuilder` is part of `spring-boot-starter-web` which may not be available in the classpath during Docker build.

### Solution
Replace `RestTemplateBuilder` with direct `RestTemplate` instantiation using `SimpleClientHttpRequestFactory`.

### Changes Made

**Before:**
```java
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;

public KeepAliveScheduler(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .build();
}
```

**After:**
```java
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public KeepAliveScheduler() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(10000); // 10 seconds
    factory.setReadTimeout(10000); // 10 seconds
    this.restTemplate = new RestTemplate(factory);
}
```

### Benefits
1. ✅ No dependency on `spring-boot-starter-web`
2. ✅ Works with `spring-web` only
3. ✅ Same timeout configuration
4. ✅ Simpler code

### Testing
```bash
# Build locally
mvn clean package -DskipTests

# Build with Docker
docker build -t internflow .
```

### Related Files
- `src/main/java/com/java6/springboot/internflow/scheduler/KeepAliveScheduler.java`

### Date
2026-05-18
