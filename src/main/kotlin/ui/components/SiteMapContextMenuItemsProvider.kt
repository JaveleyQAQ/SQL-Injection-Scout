package example.contextmenu

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import processor.http.HttpInterceptor
import java.awt.Component
import javax.swing.JMenuItem
import javax.swing.SwingUtilities

/**
 * SiteMap menu
 */
class SiteMapContextMenuItemsProvider(
    private val api: MontoyaApi,
    private val httpInterceptor: HttpInterceptor
) : ContextMenuItemsProvider {

    companion object {
        private const val MENU_CHECK_REQUEST = "Check request"
        private const val MENU_CHECK_ALL_REQUESTS = "Check all requests for host: "
    }

    override fun provideMenuItems(event: ContextMenuEvent): MutableList<Component>? {
        if (!event.isFromTool(ToolType.PROXY, ToolType.TARGET,ToolType.LOGGER)) return null
        val requestResponse =  event.selectedRequestResponses().firstOrNull()  ?: return null
        return mutableListOf<Component>().apply {
            add(createCheckRequestMenuItem(requestResponse.request()))
            createCheckAllRequestsMenuItem(requestResponse.httpService()?.host())?.let { add(it) }
        }
    }

    private fun createCheckRequestMenuItem(httpRequest: HttpRequest): JMenuItem =
        JMenuItem(MENU_CHECK_REQUEST).apply {
            addActionListener {
                val startTime = System.currentTimeMillis()
                ExecutorManager.get().executorService.submit {
                    val response = api.http().sendRequest(httpRequest)
                    httpInterceptor.processHttpHandler(response)
                    updateProgress(1,1,"")
                }
                logCompletion(1,"", startTime )
            }
        }

    /**
     * check all requests for selected host
     */
    private fun createCheckAllRequestsMenuItem(host: String?): JMenuItem? =
        host?.let { host ->
            JMenuItem("$MENU_CHECK_ALL_REQUESTS$host").apply {
                addActionListener { processAllRequests(host) }
            }
        }

    private fun processAllRequests(host: String) {
        ExecutorManager.get().executorService.submit {
            val startTime = System.currentTimeMillis()
            val filteredRequests = api.siteMap().requestResponses()
                .filter { it.request().httpService()?.host()?.equals(host, true) == true }
            filteredRequests.forEachIndexed { index, entry ->
                try {
                    val response = api.http().sendRequest(entry.request())
                    httpInterceptor.processHttpHandler(response)
                    updateProgress(index + 1, filteredRequests.size, host)
                } catch (e: Exception) {
                    api.logging().logToError("Request failed: ${e.message}")
                }
            }
            logCompletion(filteredRequests.size, host, startTime)
        }
    }

    private fun updateProgress(current: Int, total: Int, host: String) {
        SwingUtilities.invokeLater {
            api.logging().logToOutput("Processing $current/$total")
        }
    }

    private fun logCompletion(total: Int, host: String, startTime: Long) {
        SwingUtilities.invokeLater {
            val duration = System.currentTimeMillis() - startTime
            api.logging().logToOutput("Completed $total requests for $host in ${duration}ms")
        }
    }
}