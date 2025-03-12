import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ExecutorManager private constructor() {


    companion object {
        // 使用 Kotlin 的 lazy 属性委托来延迟初始化单例
        private val instance: ExecutorManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            ExecutorManager()
        }

        fun get(): ExecutorManager = instance
    }

    val executorService: ScheduledExecutorService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ScheduledThreadPoolExecutor(
            150, // 直接设置最大线程数为核心线程数
            { runnable ->
                Thread(runnable).apply {
                    name = "Request-Worker-${Thread.currentThread().id}"
                    isDaemon = true // 守护线程，避免阻塞程序关闭
                }
            },
            { runnable, executor ->
                // 记录拒绝任务并重试（避免丢失请求）
                executor.execute(runnable)
            }
        ).apply {
            setKeepAliveTime(60, TimeUnit.SECONDS) // 延长空闲线程存活时间
        }
    }



    fun shutdown() {
        executorService.shutdownNow()
        // 清除静态引用以便垃圾回收
        synchronized(this) {
            //TODO:
        }
    }

}