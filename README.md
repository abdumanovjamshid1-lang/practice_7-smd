# CoroutinesLab - Practice 10

This repository contains my solutions for the Coroutines practice assignment (Part A theory questions and Part B coding tasks).

---

## Demo Videos (Tasks 1 - 5)

<details>
<summary><b>Task 1: First coroutine with runBlocking, launch and delay (Click to expand)</b></summary>
<br>

<!-- Replace the link below with your actual uploaded video link or drag-and-drop video on GitHub -->
https://github.com/user-attachments/assets/placeholder-task-1.mp4

*Shows: Normal execution order (`Start`, `B done`, `A done`) with `delay()`, and how changing to `Thread.sleep()` forces sequential blocking order.*
</details>

<details>
<summary><b>Task 2: suspend function with n tries (Click to expand)</b></summary>
<br>

https://github.com/user-attachments/assets/placeholder-task-2.mp4

*Shows: Failure path when `n = 2` (exhausting tries and showing cached user), recovery/happy path when `n = 3` (failing twice and getting `User: Ada` on 3rd try), and the compiler error when calling `fetchName()` from a regular function.*
</details>

<details>
<summary><b>Task 3: Control coroutine with Job (Click to expand)</b></summary>
<br>

https://github.com/user-attachments/assets/placeholder-task-3.mp4

*Shows: Waiting 250ms and cancelling via `cancelAndJoin()` (`isCancelled=true` and `cleanup` printed), plus the optional run with `join()` printing all 5 ticks.*
</details>

<details>
<summary><b>Task 4: Load name and age with async (Click to expand)</b></summary>
<br>

https://github.com/user-attachments/assets/placeholder-task-4.mp4

*Shows: Sequential call taking ~200ms, concurrent `async` taking ~100ms, and calling `await()` immediately after each `async` going back to ~200ms.*
</details>

<details>
<summary><b>Task 5: Find thread of each dispatcher (Click to expand)</b></summary>
<br>

https://github.com/user-attachments/assets/placeholder-task-5.mp4

*Shows: Thread names printed for `Default`, `IO`, `Unconfined`, `newSingleThreadContext("MyThread")` before and after `delay()`, and inherited `main` dispatcher.*
</details>

---

## Part A. Theory Questions (Q1 – Q10)

### Q1. Explain difference between `newSingleThreadContext` and `Dispatchers.Unconfined`. Show code example.
`newSingleThreadContext` creates a brand new, dedicated OS thread just for our coroutine. Because it is a real thread that takes up memory, we have to manually call `.close()` on it when we are done, otherwise it stays alive and causes a memory/thread leak. If we put a `delay()` inside it, the coroutine runs on this custom thread before `delay()`, and wakes up on the exact same custom thread after `delay()`.

`Dispatchers.Unconfined` is different because it doesn't lock the coroutine to any specific thread. When it starts, it just runs on whatever thread called it (like `main`). However, after it hits a suspension point like `delay()`, it resumes on the thread that the suspending function used internally (which for `delay()` is usually `kotlinx.coroutines.DefaultExecutor`).

**Code Example:**
```kotlin
val myThread = newSingleThreadContext("MyThread")
runBlocking {
    launch(myThread) {
        println("Single before delay: ${Thread.currentThread().name}") // MyThread
        delay(100)
        println("Single after delay: ${Thread.currentThread().name}")  // MyThread
    }

    launch(Dispatchers.Unconfined) {
        println("Unconfined before delay: ${Thread.currentThread().name}") // main
        delay(100)
        println("Unconfined after delay: ${Thread.currentThread().name}")  // DefaultExecutor
    }
}
myThread.close() // Must close when finished!
```

### Q2. What is difference between `async` and `launch`?
The main difference is whether we need a return value back from the coroutine:
- **`launch`** is used when we just want to start a job and don't need any result back ("fire and forget"). It returns a `Job` object. We use `job.join()` if we want to wait for it to finish, which doesn't return any data.
- **`async`** is used when the coroutine calculates or fetches something and we need that result. It returns a `Deferred<T>` (which is a child of `Job` that holds a future result). To get the value out, we call `deferred.await()`, which suspends until the value is ready and returns it.

### Q3. What is coroutine? What is difference between suspend and block?
A coroutine is basically a lightweight thread managed by the Kotlin runtime instead of the OS. We can run thousands of them on a single thread without running out of memory.
- **Suspend (`delay`)**: Pauses the current coroutine without freezing the thread. The thread is released so other coroutines can use it in the meantime.
- **Block (`Thread.sleep`)**: Completely freezes the physical OS thread. No other coroutine on that thread can do anything until the sleep timer finishes.

If we have two coroutines on one thread: with `delay(100)`, Coroutine 1 pauses and immediately lets Coroutine 2 start running on that same thread, so both finish in about ~100ms total. With `Thread.sleep(100)`, Coroutine 1 locks the thread for 100ms, forcing Coroutine 2 to wait its turn, taking ~200ms total.

### Q4. What does `suspend` keyword do?
Adding `suspend` tells the Kotlin compiler that this function can pause its execution at suspension points and resume later without blocking the thread.
- **Does it create a new thread?** No, marking a function as `suspend` does not create any thread.
- **Who can call a suspend function?** It can only be called from another `suspend` function or inside a coroutine builder (like `launch`, `async`, or `runBlocking`).
- **What hidden parameter does the compiler add?** Under the hood, the compiler adds a hidden parameter called `Continuation<T>` at the end of the function signature. This object acts like a callback and state machine that remembers local variables and where the function paused so it can resume from that exact line later.

### Q5. Explain concurrency vs parallelism.
- **Concurrency** is about dealing with multiple tasks at the same time by switching between them. Even with one CPU core and one thread, a program can start Task A, pause it while waiting, switch to Task B, and switch back.
- **Parallelism** is about actually executing multiple tasks at the exact same physical instant.

Coroutines running on a **single thread** give us **concurrency** (interleaving tasks when one suspends). True **parallelism** requires **several CPU cores** running multiple threads at the exact same moment.

### Q6. What is structured concurrency?
Structured concurrency means coroutines are always launched inside a specific `CoroutineScope`, forming a strict parent-child tree hierarchy so background tasks don't leak or get lost.
- **When parent finishes:** The parent coroutine will automatically wait and cannot complete until all of its child coroutines have finished running.
- **When parent is cancelled:** Cancelling the parent automatically cancels all of its child coroutines down the tree.
- **When a child fails:** If a child throws an unhandled exception, it propagates up to the parent, which cancels the parent itself and all other sibling coroutines.

### Q7. Compare builders: `launch`, `async`, `runBlocking`, `withContext`, `coroutineScope`.
- **`launch`**: Starts a new coroutine, returns a `Job`, does **not** block the thread.
- **`async`**: Starts a new coroutine, returns `Deferred<T>`, does **not** block the thread.
- **`runBlocking`**: Starts a coroutine and **blocks** the current thread until everything inside it finishes. Returns the result `T` of the block.
- **`withContext`**: A suspending function that switches the coroutine's context/dispatcher, waits for the block to finish without blocking the thread, and returns the result `T`.
- **`coroutineScope`**: A suspending function that creates a child scope, waits for all children inside it to finish without blocking the thread, and returns the result `T`.

**Why never use `runBlocking` on Android UI thread?** Because the Android UI thread is responsible for drawing the screen and handling user touches (~60-120 frames per second). Blocking it with `runBlocking` freezes the screen and causes an **ANR (Application Not Responding)** crash.

### Q8. What is `CoroutineScope` and what is `CoroutineContext`?
- **`CoroutineScope`** controls **when** coroutines run—it tracks their lifecycle and lets us cancel all coroutines started inside it when they are no longer needed.
- **`CoroutineContext`** controls **where and how** coroutines run—it is a set of elements like the `Job`, `CoroutineName`, and the `CoroutineDispatcher` (which decides the thread).
- **If you specify nothing:** If we call `launch { }` without passing a dispatcher, it inherits the context/dispatcher from its parent scope (for example, inside `runBlocking` on `main`, it inherits the `main` thread). If a scope has no dispatcher set at all, it falls back to `Dispatchers.Default`.

### Q9. When to use `Dispatchers.Default`, `IO` and `Main`?
- **`Dispatchers.Default`**: Best for heavy CPU work (e.g., sorting a huge list or filtering image pixels). **Avoid:** Blocking network or disk calls, because Default only has as many threads as CPU cores, so blocking them starves the CPU pool.
- **`Dispatchers.IO`**: Best for waiting on disk or network operations (e.g., reading a file, Room database queries, or Retrofit calls). **Avoid:** Heavy CPU math loops, because IO can spin up to 64+ threads, which causes slow thread-switching overhead if they are all doing heavy CPU work.
- **`Dispatchers.Main`**: Best for interacting with the UI (e.g., setting text on a TextView or updating Compose state). **Avoid:** Anything slow like file reading, network calls, or heavy sorting, as it will freeze the app UI.

### Q10. Explain line by line: `viewModelScope.launch { withContext(Dispatchers.IO) { ... } }`
1. `viewModelScope.launch {`: Starts a coroutine tied to the Android `ViewModel`. By default, `viewModelScope` uses `Dispatchers.Main.immediate`, so code right inside `launch` starts on the **Main (UI) thread**.
2. `withContext(Dispatchers.IO) { ... }`: Suspends the Main coroutine (leaving the UI thread free) and shifts execution of the inner block to a background thread from the **IO thread pool**. Once the block finishes, it switches back to the **Main thread** for any remaining lines inside `launch`.
3. **When ViewModel is cleared:** When the user leaves the screen and `onCleared()` is called on the `ViewModel`, `viewModelScope` automatically cancels its job, which immediately cancels the running `withContext(Dispatchers.IO)` work so we don't leak memory or waste battery.

---

## Part B. Notes & Explanations for Coding Tasks

### Task 1 (Optional): Why does replacing `delay()` with `Thread.sleep()` change the output order?
When we use `delay()`, Coroutine A and B suspend without blocking the `main` thread, allowing `println("Start")` to run first while A and B wait in the background. When we replace `delay()` with `Thread.sleep()`, the thread gets blocked inside the coroutine as soon as it runs, forcing everything on the single `main` thread to wait sequentially instead of running concurrently.

### Task 2: Compile error when calling `fetchName()` from a normal function
If we try to call `fetchName()` inside a regular non-suspend function without a coroutine builder, the Kotlin compiler gives this exact error:
```text
Suspend function 'fetchName' should be called only from a coroutine or another suspend function
```

### Task 4: Why does calling `await()` right after each `async` make the time go back to ~200 ms?
Because `await()` is a suspending function that pauses the coroutine right on that line until the result is ready. If we write `async { loadName() }.await()`, the program waits the full 100 ms for `loadName()` to finish before it even reaches the next line to start `async { loadAge() }`. To run them in parallel (~100 ms), we must start both `async` blocks first, and only call `.await()` on both of them afterwards.

---

## How Structured Concurrency is Handled in this Repo

In this project (`Main.kt`), every coroutine is started inside a `runBlocking` scope within each task function, meaning there are no "runaway" global coroutines (we never use `GlobalScope`). 

Here is an example from our code showing how parent-child scoping and cancellation work together:

```kotlin
fun runTask3() = runBlocking {
    // 'runBlocking' is the parent scope.
    // 'launch' creates a child Job tied to this runBlocking parent.
    val job = launch {
        try {
            for (i in 0..4) {
                println("tick $i")
                delay(100) // checks for cancellation at every suspension point
            }
        } finally {
            // Structured concurrency ensures that when the job is cancelled,
            // it throws CancellationException at delay() and lets us clean up safely.
            println("cleanup")
        }
    }

    delay(250)
    // Cancels the child job and waits for its 'finally' block to finish
    // before the parent scope moves on.
    job.cancelAndJoin()
    println("isCancelled=${job.isCancelled}")
}
```

**Why this follows structured concurrency:**
1. **Parent waits for children:** In `runTask1()`, even though we don't manually call `.join()` on Coroutines A and B, `runBlocking` automatically waits for both child `launch` blocks to finish after 100ms and 200ms before exiting the function.
2. **Scoped Parallelism:** In `runTask4()`, both `async` calls are children of `runBlocking`. If one of them threw an exception, the parent scope would automatically cancel the other `async` task immediately instead of letting it run in the background for nothing.
3. **Clean Cancellation & Resource Release:** In `runTask3()` and `runTask5()`, jobs are tracked by their parent scope, `finally` blocks are guaranteed to run before `cancelAndJoin()` returns, and `newSingleThreadContext` is explicitly closed via `.close()` once its child coroutine completes.
