package example.contextmenu

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import config.ExecutorManager
import processor.http.HttpInterceptor
import java.awt.Component
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JMenuItem
import javax.swing.SwingUtilities

/**
 *
 * 支持单选、多选扫描，以及按 Sitemap 全量扫描
 */
class SiteMapContextMenuItemsProvider(
    private val api: MontoyaApi,
    private val httpInterceptor: HttpInterceptor
) : ContextMenuItemsProvider {

    companion object {
        private const val MENU_CHECK_SELECTED = " Check selected"
        private const val MENU_CHECK_HOST = "Check all for host: "
    }

    override fun provideMenuItems(event: ContextMenuEvent): MutableList<Component>? {
        if (!event.isFromTool(ToolType.PROXY, ToolType.TARGET, ToolType.LOGGER, ToolType.INTRUDER)) return null

        val selectedItems = event.selectedRequestResponses()
        if (selectedItems.isEmpty()) return null
        val menuList = mutableListOf<Component>()
        val scanLabel = if (selectedItems.size == 1) {
            "$MENU_CHECK_SELECTED request"
        } else {
            "$MENU_CHECK_SELECTED ${selectedItems.size} requests"
        }

        val scanItem = JMenuItem(scanLabel).apply {
            addActionListener {
                performScan(selectedItems)
            }
        }
        menuList.add(scanItem)

        // Check Host
        val firstItem = selectedItems.first()
        val host = firstItem.httpService()?.host()

        if (host != null) {
            val hostItem = JMenuItem("$MENU_CHECK_HOST$host").apply {
                addActionListener {
                    performScanByHost(host)
                }
            }
            menuList.add(hostItem)
        }

        return menuList
    }

    /**
     * 处理选中的请求列表 (多选/单选)
     */
    private fun performScan(items: List<HttpRequestResponse>) {
        val total = items.size
        val startTime = System.currentTimeMillis()
        val counter = AtomicInteger(0) // 线程安全的计数器

        api.logging().logToOutput("[+] Batch scan started: $total requests queued.")

        for (item in items) {
            val request = item.request()
            ExecutorManager.get().executorService.submit {
                try {
                    val response = api.http().sendRequest(request)
                    httpInterceptor.processHttpHandler(response)
                } catch (e: Exception) {
                    api.logging().logToError("Scan failed for ${request.url()}: ${e.message}")
                } finally {
                    val finishedCount = counter.incrementAndGet()
                    if (finishedCount == total) {
                        logCompletion(total, "Selection", startTime)
                    }
                }
            }
        }
    }

    /**
     * 扫描指定 Host 下的所有请求 (SiteMap)
     */
    private fun performScanByHost(host: String) {
        // 获取 SiteMap 可能会很慢
        ExecutorManager.get().executorService.submit {
            val startTime = System.currentTimeMillis()
            api.logging().logToOutput("[*] Fetching sitemap for host: $host ...")

            // 过滤 SiteMap
            val targetRequests = api.siteMap().requestResponses()
                .filter { it.httpService()?.host()?.equals(host, ignoreCase = true) == true }

            if (targetRequests.isEmpty()) {
                api.logging().logToOutput("[-] No requests found for host: $host")
                return@submit
            }
            api.logging().logToOutput("[+] Found ${targetRequests.size} requests for $host. Starting scan...")
            performScan(targetRequests)
        }
    }


    private fun logCompletion(total: Int, target: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        api.logging().logToOutput("[=] Completed scanning $total requests for [$target] in ${duration}ms")
    }
}