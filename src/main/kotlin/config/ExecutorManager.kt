package config
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ExecutorManager private constructor() {

    companion object {
        private val instance: ExecutorManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            ExecutorManager()
        }
        fun get(): ExecutorManager = instance
    }

    // 动态计算核心线程数（I/O 密集型任务优化）
    private val corePoolSize = (Runtime.getRuntime().availableProcessors() * 1.5).toInt().coerceAtLeast(4)

    // 原子计数器保证线程名唯一
    private val threadCounter = AtomicInteger(0)

    val executorService: ScheduledExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        object : ScheduledThreadPoolExecutor(
            corePoolSize,
            ThreadFactory { runnable ->
                Thread(runnable).apply {
                    name = "Request-Worker-${threadCounter.getAndIncrement()}"
                    isDaemon = true
                    priority = Thread.NORM_PRIORITY - 1 // 降低优先级避免抢占CPU
                }
            },
            RejectedExecutionHandler { runnable, executor ->
                // 丢弃最旧任务 + 记录日志
                if (executor.queue.size >= corePoolSize * 2) {
                    executor.queue.poll()
//                    api.logging().logToWarn("Task rejected, dropping oldest task: ${runnable.javaClass.simpleName}")
                }
                executor.execute(runnable)
            }
        ) {
            // 覆盖 afterExecute 捕获未处理异常
            override fun afterExecute(r: Runnable, t: Throwable?) {
                super.afterExecute(r, t)
                if (t != null) {
                   println("Uncaught exception in task: ${t}")
                }
            }
        }.apply {
            setKeepAliveTime(30, TimeUnit.SECONDS)
            allowCoreThreadTimeOut(true) // 允许核心线程超时
        }
    }

    // 安全关闭逻辑优化
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}