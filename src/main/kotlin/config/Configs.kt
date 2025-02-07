package config

import burp.api.montoya.project.Project
import java.util.Properties

enum class Configs {
    INSTANCE;

    var version: String
    var extensionName: String

    init {
        // 加载属性文件
        val properties = Properties()
        val stream = javaClass.getResourceAsStream("/gradle.properties")
        properties.load(stream)
        // 读取属性
        version = properties.getProperty("projectVersion", "unknown")
        extensionName = properties.getProperty("extensionName", "unknown")
    }

    var startUP: Boolean = true
    var isInScope: Boolean = true
    var proxy: Boolean = true
    var repeater: Boolean = true

    var nullCheck: Boolean = true
    var neverScanRegex: String = "(delete|del)"

    var filterStatusButton: Boolean = true
    var randomCheckTimer: Long = 1000 // 随机扫描时间改为1秒
    var fixedIntervalTime: Long = 100 // 固定间隔改为100ms

    var urlFileExtension:MutableList<String> = mutableListOf(
        "js", "css", "jpg", "jpeg", "png", "gif", "ico",
        "woff", "woff2", "ttf", "eot", "mp4", "webm", "mp3",
        "wav", "pdf", "doc", "docx", "xls", "xlsx"
    )
    var payloads: MutableList<String> = mutableListOf<String>(
        "åååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååååå",
        "'\"%df",
        "'''",
        "''''",
        "#{xx}",
        "#xx}",
        "sb'='\"=\"",

//        "'",
//        "\"",
//        "')",
//        "''",
//        "'||'1",
//        "\"||\"1",
//        "sb'='\"=\"",
//        "'OR'1\"OR\"1",
//        "'OR'+1+\"OR\"+1=0",
//        "' sleep(3)",
//        "';WAITFOR DELAY '0:0:5'--" ,
//        "' AND (SELECT * FROM (SELECT(SLEEP(5)))a)--",
        "SLEEP(3) /*' or SLEEP(3) or'\" or SLEEP(3) or \""
    )
    val ERROR_SYNTAX: Array<String> = arrayOf(
        // 通用SQL语法错误
        "You have an error in your SQL syntax",                    // MySQL通用语法错误信息
        "Syntax error near",                                       // 通用语法错误信息，适用于多种DBMS
        "syntax error at or near",                                 // PostgreSQL及其他DBMS通用语法错误
        "Incorrect syntax near",                                   // SQL Server语法错误
        "nearby:",                                                 // 语法错误指示符
        "附近有语法错误",                                          // 中文语法错误提示
        "引号不完整",                                             // 中文中未闭合的引号
        "数据库出错",                                             // 中文数据库错误消息
        "Error SQL:",                                              // 通用SQL错误前缀
        "SQL error",                                               // 通用SQL错误信息
        "SQLSTATE=\\d+",                                          // SQLSTATE错误代码

        // 特定语法问题
        "the used select statements have different number of columns", // SELECT语句列数不同
        "An illegal character has been found in the statement",    // 语句中存在非法字符
        "MySQL server version for the right syntax to use",        // 不正确的MySQL语法版本
        "Unexpected end of command in statement",                  // 命令意外结束
        "A Parser Error \$syntax error\$",                       // 解析器错误（语法错误），转义括号
        "Syntax error in string in query expression",              // 查询表达式中的字符串语法错误

        // 表或列未找到错误
        "Table '\\S+' doesn't exist",                              // 表不存在
        "Unknown column '\\S+' in 'field list'",                   // 字段列表中列未知
        "column \"\\S*\" does not exist",                          // PostgreSQL中列不存在

        // 数据类型不匹配错误
        "Data type mismatch in criteria expression",               // 条件表达式中的数据类型不匹配

        // 操作符或子句使用错误
        "Invalid use of the '\\S+'",                               // 操作符或子句使用错误

        // 引号未闭合
        "Unclosed quotation mark (before|after) the character string", // 引号未闭合，出现在字符串前后

        // 各种驱动程序和提供者的错误
        "\$Microsoft\$\$ODBC .*? Driver\$",                    // ODBC驱动程序错误，转义方括号
        "java\\.sql\\.SQLException",                               // Java SQL异常
        "com\\.microsoft\\.sqlserver\\.jdbc",                      // Microsoft SQL Server JDBC驱动程序
        "com\\.informix\\.jdbc",                                   // Informix JDBC驱动程序
        "org\\.postgresql\\.jdbc",                                 // PostgreSQL JDBC驱动程序
        "com\\.jnetdirect\\.jsql",                                 // jSQLConnect JDBC驱动程序
        "macromedia\\.jdbc\\.sqlserver",                           // ColdFusion SQL Server JDBC驱动程序
        "System\\.Exception: SQL Execution Error",                 // SQL执行期间的系统异常
        "System\\.Data\\.SqlClient\\.SqlException",                // .NET SqlClient Data Provider异常
        "System\\.Data\\.OleDb\\.OleDbException",                  // OleDb异常

        // 特定数据库系统的错误
        "ORA-\\d{5}:",                                             // Oracle特定错误代码
        "PLS-\\d{4}:",                                             // PL/SQL特定错误代码
        "DB2 SQL error:",                                          // DB2 SQL错误
        "SQLite error",                                            // SQLite通用错误
        "Microsoft JET Database Engine",                           // Microsoft JET Database引擎错误

        // 缺少元素的错误
        "Missing FROM clause",                                     // 缺少FROM子句
        "Missing parentheses in the query",                       // 查询中缺少括号
        "Missing join condition",                                  // 缺少JOIN条件
        "Empty query is not allowed",                              // 空查询不允许

        // 其他常见的SQL相关错误
        "Ambiguous column name",                                   // 列名模糊
        "Null value not allowed in column",                       // 列中不允许空值
        "Subquery returns more than one row",                     // 子查询返回多行
        "Invalid order by clause",                                // ORDER BY子句无效
        "Cannot drop table that is currently in use",             // 正在使用的表不能被删除
        "Cannot alter system table",                              // 不能修改系统表
        "Procedure '[^']*' requires parameter",                   // 过程需要参数
        "supplied argument is not a valid \\w+ result",           // 提供的参数不是有效的结果
        "internal error \$IBM\$\$CLI Driver\$\$DB2",         // 内部DB2错误，转义方括号
        "PostgreSQL query failed:",                               // PostgreSQL查询失败
        "Dynamic Page Generation Error:",                         // 动态页面生成错误
        "ADODB\\.Field 0x800A0BCD",                               // ADODB字段错误，移除美元符号
        "Sintaxis incorrecta cerca de",                           // 西班牙语语法错误
        "PG::SyntaxError:",                                       // PostgreSQL语法错误
        "where clause",                                           // WHERE子句错误
        "Oracle error",                                           // Oracle错误
        "Sybase message:",                                        // Sybase错误消息
        "Database error",                                         // 通用数据库错误
        "INSERT INTO",                                            // SQL INSERT语句错误
        "SQL syntax",                                             // 通用SQL语法错误
        "PSQLException",                                          // PostgreSQL异常
        "mysql_fetch_array\$\$",                                // MySQL fetch array函数错误，转义括号
        "pg_query\$\$ \\[:",                                    // PostgreSQL查询函数错误，转义括号
        "pg_exec\$\$ \\[:",                                     // PostgreSQL exec函数错误，转义括号
        "mssql_query\$\$",                                      // MSSQL查询函数错误，转义括号

        // 更具体的错误信息
        "Column count doesn't match value count at row",          // 行中的列数与值数不匹配
        "your MySQL server version",                              // MySQL服务器版本相关错误
        "valid \\w+ result",                                      // 有效结果验证
        "Procedure '[^']*' requires parameter",                   // 需要参数的过程
        "Microsoft OLE DB Provider for ODBC Drivers",             // OLE DB提供者错误
        "Microsoft OLE DB Provider for SQL Server",               // SQL Server的OLE DB提供者错误
        "Microsoft\\[ODBC Microsoft Access Driver\$"            // Microsoft Access ODBC驱动程序错误，转义方括号
    )
    var maxAllowedParameterCount: Int = 30 // max allowed parameters
    var allowedMimeTypeMimeType: MutableList<String> =  mutableListOf<String>("NONE", "HTML", "PLAIN_TEXT", "JSON", "XML", "YAML", "APPLICATION_UNKNOWN", "LEGACY_SER_AMF")
    var  uninterestingType = mutableListOf("js", "js.map", "css", "css.map", "swf", "zip", "gz", "7zip", "war", "jar", "doc", "docx", "xls", "xlsx", "pdf", "exe", "dll", "png", "jpeg", "jpg", "bmp", "tif", "tiff", "gif", "webp", "svg", "ico", "m3u", "mp4", "m4a", "ogg", "aac", "flac", "mp3", "wav", "avi", "mov", "mpeg", "wmv", "webm", "woff", "woff2", "ttf")
    var heuristicWordsError: MutableList<String> = mutableListOf<String>(
        "无法解析请求参数",
        "无效的请求",
        "\"code\":999",
        "\"code\":400",
        "系统异常，请稍后再试！",
        "网页访问错误",
        "33333"
    )
    var boringWordsError: MutableList<String> = mutableListOf<String>(
        "Failed to convert value of type ['\"]([^'\"]+)['\"]",
        "\"error\": \"Bad Request\""
    )

}


