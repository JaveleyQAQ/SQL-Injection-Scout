package example.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

// 配置数据类
data class ServerConfig(
    val host: String = "localhost",
    val port: Int = 8080,
    val endpoints: List<EndpointConfig> = emptyList()
) {
    // 添加扩展属性，直接获取所有path
    val paths: List<String>
        get() = endpoints.map { it.path }
}

data class EndpointConfig(
    val path: String,
    val method: String,
    val headers: Map<String, String> = emptyMap()
)

// YAML配置解析器
class YamlConfigParser {
    private val mapper = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
    }

    fun parseConfig(filePath: String): ServerConfig {
        return try {
            val file = this.javaClass.getResourceAsStream(filePath)
            mapper.readValue(file)
        } catch (e: Exception) {
            println("解析配置文件失败: ${e.message}")
            ServerConfig() // 返回默认配置
        }
    }
}

fun main() {
    val parser = YamlConfigParser()
    val config = parser.parseConfig("/config/config.yml")

    // 现在可以直接访问paths
    println("所有路径: ${config.paths}")

    // 如果需要访问单个path，可以通过索引
    if (config.paths.isNotEmpty()) {
        println("第一个路径: ${config.paths[0]}")
    }
}