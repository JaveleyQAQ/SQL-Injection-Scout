import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
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
            30, // corePoolSize
            ThreadPoolExecutor.CallerRunsPolicy()
        ).apply {
            maximumPoolSize = 150
            setKeepAliveTime(7L, TimeUnit.SECONDS)
            allowCoreThreadTimeOut(true)
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