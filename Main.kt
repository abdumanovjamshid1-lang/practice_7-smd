package coroutineslab

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

// ==========================================
// Task 1: runBlocking, launch, delay
// ==========================================
fun runTask1() = runBlocking {
    println("--- Task 1 ---")
    // Coroutine A: 200 ms
    launch {
        delay(200)
        println("A done")
    }
    // Coroutine B: 100 ms
    launch {
        delay(100)
        println("B done")
    }
    // Prints right after launching
    println("Start")
}

// ==========================================
// Task 2: Suspend function with n tries
// ==========================================
var callCounter = 0

suspend fun fetchName(): String? {
    delay(100)
    callCounter++
    return if (callCounter >= 3) "Ada" else null
}

fun showUser(maxTries: Int) = runBlocking {
    var succeeded = false
    for (attempt in 1..maxTries) {
        val result = fetchName()
        if (result != null) {
            println("User: $result")
            succeeded = true
            break
        } else {
            println("try $attempt failed")
        }
    }
    if (!succeeded) {
        println("Server is down, show cached user")
    }
}

fun runTask2() {
    println("\n--- Task 2 (Failure Path: n = 2) ---")
    callCounter = 0
    showUser(2)

    println("\n--- Task 2 (Happy Path: n = 3) ---")
    callCounter = 0
    showUser(3)
}

// ==========================================
// Task 3: Control coroutine with Job
// ==========================================
fun runTask3() = runBlocking {
    println("\n--- Task 3 (Cancellation) ---")
    val job = launch {
        try {
            for (i in 0..4) {
                println("tick $i")
                delay(100)
            }
        } finally {
            println("cleanup")
        }
    }

    delay(250)
    job.cancelAndJoin()
    println("isCancelled=${job.isCancelled}")

    println("\n--- Task 3 (Optional: Complete with join) ---")
    val completeJob = launch {
        try {
            for (i in 0..4) {
                println("tick $i")
                delay(100)
            }
        } finally {
            println("cleanup")
        }
    }
    completeJob.join()
}

// ==========================================
// Task 4: Load name & age with async
// ==========================================
suspend fun loadName(): String {
    delay(100)
    return "Ada"
}

suspend fun loadAge(): Int {
    delay(100)
    return 36
}

fun runTask4() = runBlocking {
    println("\n--- Task 4 ---")

    // 1. Sequential: one by one (~200 ms)
    val seqTime = measureTimeMillis {
        val name = loadName()
        val age = loadAge()
    }
    println("Sequential time: ~$seqTime ms")

    // 2. Parallel async: await() at the end (~100 ms)
    val parTime = measureTimeMillis {
        val nameDeferred = async { loadName() }
        val ageDeferred = async { loadAge() }
        val name = nameDeferred.await()
        val age = ageDeferred.await()
    }
    println("Async (parallel) time: ~$parTime ms")

    // 3. Immediate await: await right after async (~200 ms)
    val immTime = measureTimeMillis {
        val name = async { loadName() }.await()
        val age = async { loadAge() }.await()
    }
    println("Async (immediate await) time: ~$immTime ms")
}

// ==========================================
// Task 5: Find thread of each dispatcher
// ==========================================
@OptIn(DelicateCoroutinesApi::class)
fun runTask5() = runBlocking {
    println("\n--- Task 5 ---")

    // 1. Default Dispatcher
    launch(Dispatchers.Default) {
        println("Default: ${Thread.currentThread().name}")
    }.join()

    // 2. IO Dispatcher
    launch(Dispatchers.IO) {
        println("IO: ${Thread.currentThread().name}")
    }.join()

    // 3. Unconfined Dispatcher
    launch(Dispatchers.Unconfined) {
        println("Unconfined: ${Thread.currentThread().name}")
    }.join()

    // 4. newSingleThreadContext
    val customContext = newSingleThreadContext("MyThread")
    launch(customContext) {
        println("newSingleThreadContext before: ${Thread.currentThread().name}")
        delay(50)
        println("newSingleThreadContext after: ${Thread.currentThread().name}")
    }.join()
    customContext.close() // release thread resource

    // 5. Inherited Dispatcher (no dispatcher argument)
    launch {
        println("Inherited: ${Thread.currentThread().name}")
    }.join()
}

// ==========================================
// Main Function
// ==========================================
fun main() {
    runTask1()
    runTask2()
    runTask3()
    runTask4()
    runTask5()
}
