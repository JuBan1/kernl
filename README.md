# **Kernl**

**Kernl** is a Kotlin library built to simplify the majority of data management scenarios. **Kernl** provides a flexible and declarative 
approach to defining your data management strategies. **Kernl** gives you granular control over all aspects of caching, from 
in-memory network caching to offline backups in poor network conditions to database persistence to Key-Value storage.

## Features

1. **Declarative APIs:** Define the rules and **Kernl** generates the code to execute it.
2. **Performance:** **Kernl** uses KSP to generate code rather than relying on reflection to build functionality.
2. **Flexible Caching:** Use **Kernl**-provided policies or build your own custom policies to define your needs.
4. **Flexible Integration**: Integrates easily with all major dependency injection frameworks.
3. **Real-Time Data Sync:** Ensure your entire application stays in sync with **Kernl**'s managed streams.

## Quick Start

### 1. Add dependencies to your build.gradle.kts
You'll notice there are 3 distinct libraries. This is to keep your final `jar`/`aar` files as compact as possible by only
including the minimum runtime dependencies. Things like annotations are only needed at compile time.
```kotlin
dependencies {
    ksp("org.mattshoe.shoebox:Kernl.Processor:1.0.0")
    compileOnly("org.mattshoe.shoebox:Kernl.Annotations:1.0.0")
    implementation("org.mattshoe.shoebox:Kernl.Runtime:1.0.0")
}
```

### 2. Annotate

Annotate any method and give it a name to indicate that a `Kernl` should be generated for that method. This annotation 
will typically be on a Retrofit service method.

See [Annotations](#annotations)
for a list of annotations to use.

```kotlin
interface UserDataService {
    @Kernl.SingleCache.InMemory("UserData")
    suspend fun getUserData(id: String, someParam: Int, otherParam: Boolean): UserData
}
```

### 3. (Optional) Create Your Own [`KernlPolicy`](docs/kernl/KERNL_POLICY.md)
```kotlin
class UserDataKernlPolicy: KernlPolicy, Disposable {
    // You could use this to expose public triggers to affect Kernls en-masse
    override val events = MutableSharedFlow<KernlEvenet>()
    // Data is only valid for 25 minutes
    override val timeToLive = 25.minutes
    // Load data from disk first, then fall back to network
    override val cacheStrategy = CacheStrategy.DiskFirst
    // Pre-emptively refresh data when it is about to expire
    override val invalidationStrategy = InvalidationStrategy.Preemptive(leadTime = 30.seconds, retries = 3)
}
```

### 4. Bind Your **Kernl**

**Kernl** was designed with flexibility in mind, so it is trivial to create an instance of the generated repository 
via its associated Factory. This allows you to use **Kernl** with any dependency injection framework. Examples included below.

#### With DefaultKernlPolicy
```kotlin
// Option 1: Pass function pointer to the factory
val useDataKernl = UserDataKernl.Factory(serviceImpl::getUserData)

// Option 2: Pass lambda to the factory
val useDataKernl = UserDataKernl.Factory { id, someParam, otherParam ->
    service.getUserData(id, someParam, otherParam)
}
```

#### With Custom KernlPolicy
```kotlin
// Option 1: Pass function pointer to the factory
val useDataKernl = UserDataKernl.Factory(kernlPolicy, serviceImpl::getUserData)

// Option 2: Pass lambda to the factory
val useDataKernl = UserDataKernl.Factory(kernlPolicy) { id, someParam, otherParam ->
    service.getUserData(id, someParam, otherParam)
}
```

#### Dagger Sample with DefaultKernlPolicy
```kotlin
@Module
interface UserDataModule {
    companion object {
        @Provides
        fun provideUserDataKernl(
            service: UserDataService
        ): UserDataKernl {
            return UserDataKernl.Factory(service::getUserData)
        }
    }
}
```

#### Dagger Sample with Custom KernlPolicy
```kotlin
@Module
interface UserDataModule {
    companion object {
        @Provides
        fun provideUserDataKernl(
            service: UserDataService,
            kernlPolicy: UserDataKernlPolicy
        ): UserDataKernl {
            return UserDataKernl.Factory(kernlPolicy, service::getUserData)
        }
    }
}
```

<details>
    <summary><b>Other Popular Dependency Injection Examples</b></summary>

#### Hilt Sample
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UserDateModule {
    
    @Singleton
    @Provides
    fun provideUserDataKernl(
        service: UserDataService
    ): UserDataKernl {
        return UserDataKernl.Factory(service::getUserData)
    }
}
```

#### Koin Sample
```kotlin
val userDataKernl = module {
    single<UserDataKernl> {
        UserDataKernl.Factory(service::getUserData)
    }
}
```

#### Spring Sample
```kotlin
@Configuration
class UserDataConfiguration {

    @Bean
    fun userDataKernl(service: UserDataService): UserDataKernl {
        return UserDataKernl.Factory(service::getUserData)
    }
}
```

</details>


### 5. Use Your **Kernl**!

Now you can just inject your **Kernl** and use it wherever you see fit. This will typically be injected into your repository
layer to interface with the rest of your app, but every architecture is unique, so use it where it suits your architecture
best.

```kotlin
class UserRepository(
    private val userDataKernl: UserDataKernl
) {
    private val _userData = MutableSharedFlow<UserData>()
    private val _errors = MutableSharedFlow<Error>()
    
    val userData: Flow<UserData> = _userData
    val errors: Flow<Error> = _errors
    
    init {
        userDataKernl.data
            .onSuccess {
                _userData.emit(it)
            }.onInvalidation {
                handleInvalidation()
            }.onError {
                _errors.emit(it.error)
            }.launchIn(viewModelScope)
    }
    
    suspend fun loadUser(id: String, someParam: Int, otherParam: Boolean) {
        userDataKernl.fetch(
            UserDataKernl.Params(id, someParam, otherParam)
        )
    }
    
    private suspend fun handleInvalidation() {
        userDataKernl.refresb(
            UserDataKernl.Params(id, someParam, otherParam)
        )
    }
}
```



# API Documentation 

### Annotations
- [`@Kernl.NoCache`](docs/annotations/NO_CACHE.md)
- [`@Kernl.SingleCache.InMemory`](docs/annotations/SINGLE_MEMORY_CACHE.md)
- [`@Kernl.AssociativeCache.InMemory`](docs/annotations/ASSOCIATIVE_MEMORY_CACHE.md)

### Kernl
- [`NoCacheKernl`](docs/**Kernl**/NO_CACHE_KERNL.md)
- [`SingleCacheKernl`](docs/**Kernl**/SINGLE_CACHE_KERNL.md)
- [`AssociativeCacheKernl`](docs/**Kernl**/ASSOCIATIVE_MEMORY_CACHE_KERNLmd)
- [`DataResult`](docs/DATA_RESULT.md)
- [`ValidDataResult`](docs/VALID_DATA_RESULT.md)
- [`ErrorDataResult`](docs/ERROR_DATA_RESULT.md)
- [`Kernl`](docs/kernl/KERNL.md)
- [`DefaultKernlPolicy`](docs/kernl/DEFAULT_KERNL_POLICY.md)
- [`KernlPolicy`](docs/kernl/KERNL_POLICY.md)
- [`CacheStrategy`](docs/kernl/CACHE_STRATEGY.md)
- [`InvalidationStrategy`](docs/kernl/INVALIDATION_STRATEGY.md)
- [`KernlEvent`](docs/kernl/KERNL_EVENT.md)

### Extensions
- [`valueOrNull()`](docs/extensions/VALUE_OR_NULL.md)
- [`unwrap()`](docs/extensions/UNWRAP.md)
- [`unwrap { ... }`](docs/extensions/UNWRAP_WITH_ERROR_HANDLING.md)
- [`orElse { ... }`](docs/extensions/OR_ELSE.md)
- [`asDataResult()`](docs/extensions/AS_DATA_RESULT)
- [`onSuccess { ... }`](docs/extensions/ON_SUCCESS.md)
- [`onError { ... }`](docs/extensions/ON_ERROR.md)
- [`onInvalidation { ... }`](docs/extensions/ON_INVALIDATION.md)
- [`cacheDataResult { ... }`](docs/extensions/CATCH_DATA_RESULT.md)