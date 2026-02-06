package config

import burp.api.montoya.MontoyaApi
import burp.api.montoya.persistence.Preferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class DataPersistence(val api: MontoyaApi) {
    private val preferences: Preferences = api.persistence().preferences() ?: throw IllegalStateException("Persistence not available")
    val config = Configs.INSTANCE
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private companion object {
        const val KEY_FLAG = "JAVELEY_GLOBAL_FLAG"
    }

    init {
        if (preferences.getString(KEY_FLAG) == null) {
            preferences.setString(KEY_FLAG, "INIT_DONE")
            saveData()
        } else {
            loadData()
        }
    }

    fun loadData() {
        with(preferences) {
            getBoolean("startUP")?.let { config.startUP = it }
            getBoolean("isInScope")?.let { config.isInScope = it }
            getBoolean("proxy")?.let { config.proxy = it }
            getBoolean("repeater")?.let { config.repeater = it }
            getBoolean("nullCheck")?.let { config.nullCheck = it }

            getInteger("maxAllowedParameterCount")?.let { config.maxAllowedParameterCount = it }
            getLong("randomCheckTimer")?.let { config.randomCheckTimer = it }
            getLong("fixedIntervalTime")?.let { config.fixedIntervalTime = it }
            getString("neverScanRegex")?.let { config.neverScanRegex = it }
            getString("nestedJsonParams")?.let { config.nestedJsonParams = it }

            // 使用 JSON 反序列化加载列表
            loadJsonList("payloads", config.payloads)
            loadJsonList("boringWords", config.boringWords)
            loadJsonList("uninterestingType", config.uninterestingType)
            loadJsonList("allowedMimeTypeMimeType", config.allowedMimeTypeMimeType)
            loadJsonList("hiddenParams", config.hiddenParams)
            loadJsonList("ignoreParams", config.ignoreParams)
        }
    }

    fun saveData() {
        with(preferences) {
            setBoolean("startUP", config.startUP)
            setBoolean("isInScope", config.isInScope)
            setBoolean("proxy", config.proxy)
            setBoolean("repeater", config.repeater)
            setBoolean("nullCheck", config.nullCheck)

            setInteger("maxAllowedParameterCount", config.maxAllowedParameterCount)
            setLong("randomCheckTimer", config.randomCheckTimer)
            setLong("fixedIntervalTime", config.fixedIntervalTime)
            setString("neverScanRegex", config.neverScanRegex ?: "")
            setString("nestedJsonParams", config.nestedJsonParams ?: "")

            saveJsonList("payloads", config.payloads)
            saveJsonList("boringWords", config.boringWords)
            saveJsonList("uninterestingType", config.uninterestingType)
            saveJsonList("allowedMimeTypeMimeType", config.allowedMimeTypeMimeType)
            saveJsonList("hiddenParams", config.hiddenParams)
            saveJsonList("ignoreParams", config.ignoreParams)
        }
    }

    fun updateConfig() = saveData()

    /**
     * 将 List 转为 JSON 字符串并存入 Preferences
     */
    private fun saveJsonList(key: String, list: List<String>) {
        val json = gson.toJson(list)
        preferences.setString(key, json)
    }

    /**
     * 从 Preferences 读取 JSON 字符串并还原为 List
     */
    private fun loadJsonList(key: String, targetList: MutableList<String>) {
        val json = preferences.getString(key)
        if (!json.isNullOrBlank()) {
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val decodedList: List<String> = gson.fromJson(json, listType)
                targetList.clear()
                targetList.addAll(decodedList)
            } catch (e: Exception) {
                System.err.println("Failed to parse config list for key: $key")
            }
        }
    }
}