## 前言
如今android开发基本上从之前的Java语言转而使用Kotlin语言，MMVM模式中用于保存UI状态的工具`LiveData`也逐渐被`Flow`代替。下面将逐步介绍Kotlin的Flow相关知识，以及如何与`Coroutine`配合使用，写出漂亮的声明式，响应式代码，当然最重要的是性能强大，可读性强，易于维护！

## Flow介绍
Flow 是 Kotlin 协程库中的一个概念和类，用于处理异步数据流。它提供了一种声明式的方式来处理连续的、异步的数据序列，并且与协程无缝集成。

### 以下是 Flow 的一些关键特性和优势

1. 异步数据流：Flow 允许以异步的方式处理连续的数据流。它可以处理大量的数据或长时间运行的操作，而无需阻塞主线程。
2. 声明式编程：Flow 提供了一种声明式的编程模型，通过操作符（operators）链式调用来处理数据流。这使得代码更简洁、易读和易于维护。
3. 可组合性：Flow 的操作符可以组合在一起，构建复杂的数据转换和处理逻辑。您可以使用 `map`、`filter`、`flatMap`、`zip` 等操作符来转换、过滤、合并和组合数据流。
4. 挂起函数：Flow 的操作可以在挂起函数中执行，使其适用于与协程一起使用。这样可以方便地进行异步操作和并发编程，避免了回调地狱和复杂的线程管理。
5. 取消支持：Flow 具有与协程一样的取消支持。可以使用 `cancel`、`collect` 中的 `cancellable` 参数或 `withTimeout` 等函数来取消数据流的收集和处理。

### 在介绍flow具体用法之前，先说明下flow的冷流，热流

在 Kotlin 的协程中，"冷流"（Cold Flow）和"热流"（Hot Flow）是用来描述 Flow 和 SharedFlow 两种不同的数据流的特性，还有一种特别的热流，StateFlow，它继承自SharedFlow
```kotlin
public interface StateFlow<out T> : SharedFlow<T> {
    /**
     * The current value of this state flow.
     */
    public val value: T
}
```

### cold flow & hot flow区别
1. 冷流（Cold Flow）：
    - 冷流是指每次订阅都会重新开始并独立运行的数据流。
    - 当每个订阅者开始收集数据时，冷流会从头开始发射数据，每个订阅者都会独立地接收到完整的数据流。
    - 例如，通过调用 Flow 的 `collect` 或 `collectLatest` 函数，可以订阅冷流并收集数据。

1. 热流（Hot Flow）：
    - 热流是指已经开始发射数据并在订阅之前运行的数据流。
    - 热流在启动时就开始发射数据，无论是否有订阅者。
    - 如果订阅者在流已经开始发射数据后加入，它们可能会错过一些数据。
    - 例如，通过调用 SharedFlow 的 `asSharedFlow` 函数，可以创建热流，并可以通过 `collect` 函数订阅。

## Flow使用

```Kotlin
class SecondFragment : Fragment() {

    //……省略无关代码

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFlow.setOnClickListener {
            lifecycleScope.launch {
                val value = createFlow().first()
                Log.d("flow", "flow.first() = $value")

                val acc = createFlow().fold(0) { acc, item ->
                    acc + item
                }
                Log.d("flow", "flow.fold() = $acc")

                try {
                    val value = createFlow().single()
                    Log.d("flow", "flow.single() = $value")
                } catch (e: Exception) {
                    Log.d("flow", e.toString())
                }
            }
        }

        binding.collectLastBtn.setOnClickListener {
            lifecycleScope.launch {
                createFlow().collectLatest { value ->
                    println("Collecting $value")
                    delay(1000) // Emulate work
                    println("$value collected")
                }
            }
        }

    }

    private fun createFlow(): Flow<Int> {
        return flow {
            emit(100)
            delay(500)
            emit(200)
            emit(300)
        }
    }
}
```

### flow创建
创建一个普通flow很简单，直接如上所述方法`createFlow()`,直接调用`flow{}`,代码块中使用`emit(value)`发射数据；另外还有一些其他方式创建flow，例如`T.asFlow()`和`flowOf(value: T)`等方法，本质都是调用了`flow{}`，具体使用细节看后续Demo；

```kotlin
public fun <T> Iterable<T>.asFlow(): Flow<T> = flow {
    forEach { value ->
        emit(value)
    }
}
```

### flow的常用操作符
#### first
顾名思义获取到flow数据流中的第一个元素，与之对应的是`last()`
#### fold
这个方法源码如下：需要一个参数初始值，用于后续`(acc: R, value: T) -> R`函数的入参`acc`，通过`collect`得到flow发射的每一个值，调用`operation`,返回最终得到的计算结果；
```kotlin
public suspend inline fun <T, R> Flow<T>.fold(
    initial: R,
    crossinline operation: suspend (acc: R, value: T) -> R
): R {
    var accumulator = initial
    collect { value ->
        accumulator = operation(accumulator, value)
    }
    return accumulator
}
```
例如：得到的计算结果就是100+200+300 = 600,最终打印`flow.fold() = 600`
```kotlin
val acc = createFlow().fold(0) { acc, item ->
                    acc + item
                }
Log.d("flow", "flow.fold() = $acc")
```
#### single
上述例子中有这样一段code：
```
try {
    val value = createFlow().single()
    Log.d("flow", "flow.single() = $value")
} catch (e: Exception) {
    Log.d("flow", e.toString())
}
```
这里的`single()`操作符作用如下：
1.  **获取单个元素**：`single()` 操作符用于获取 Flow 中的单个元素。如果 Flow 中只包含一个元素，它将返回该元素；如果 Flow 中包含多个元素或没有元素，它将抛出 `IllegalArgumentException` 异常。
1.  **用于确保 Flow 只包含一个元素**：`single()` 可以用作 Flow 的检查机制，确保 Flow 中只包含一个元素。如果 Flow 中的元素数量不符合预期，`single()` 将抛出异常，提供了一种简单的验证和安全性检查。
1.  **简化处理单个元素的情况**：当你只关心 Flow 中的单个元素，并希望在处理该元素时终止流的收集时，可以使用 `single()`。它能够简化对单个元素的处理逻辑。

## StateFlow

## SharedFlow


## 总结
Flow 提供了一种简洁、强大且可组合的方式来处理异步数据流。它可以与 Kotlin 协程一起使用，为异步编程提供了更优雅的解决方案，并提供了更好的可读性和维护性。Flow 的设计使得处理数据流变得更加直观和简单，同时具备高效和可扩展的特性。

Flow、StateFlow和SharedFlow是Kotlin协程库中用于处理异步数据流的不同类型。它们适用于不同的使用场景：

1.  Flow：
    -   Flow适用于一次性的、连续的异步数据流。
    -   使用Flow可以处理潜在的无限数据流，并在每次订阅时重新开始。
    -   Flow是冷流，每个订阅者都会独立地接收到完整的数据流。
    -   适合处理单个值、集合、网络请求、数据库查询等异步操作的结果。
    -   操作符链式调用的声明式编程风格使代码易于理解和组合。
2.  StateFlow：
    -   StateFlow适用于具有状态的异步数据流。
    -   它是`SharedFlow`的一个特化版本，用于表示具有可变状态的数据流。
    -   StateFlow维护当前的状态值，并将状态变化通过Flow的方式进行广播。
    -   适合在UI层面中使用，可以实现简单的状态管理，例如表示UI组件的可见性、文本内容等。
3.  SharedFlow：
    -   SharedFlow适用于多个订阅者共享的异步数据流。
    -   它是一种热流，即在开始发射数据后，无论是否有订阅者，都会持续发射数据。
    -   SharedFlow允许多个订阅者同时收到相同的数据流，而不是每个订阅者都重新开始数据流。
    -   适合实现事件总线、实时更新、广播消息等场景，可以让多个订阅者观察和响应相同的数据。

4.  `StateFlow`在遇到数据倒灌的情况下，数据倒灌不是问题，在某些场景下我们不需要数据倒灌，可以采用`SharedFlow`代替；

根据您的使用需求，可以选择适合的数据流类型。如果只需要一次性的连续数据流，可以使用Flow。如果需要具有可变状态的数据流，可以使用StateFlow。如果需要多个订阅者共享相同的数据流，可以使用SharedFlow。

注意，Flow、StateFlow和SharedFlow都需要在协程作用域内进行收集和处理，以确保正确的协程上下文和取消支持。


## 参考
官方文档[StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)&[SharedFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/)






