# 更新日志
## 1.0.2 2025年 1月16日
- 修复一直处于scanning扫描状态[#3 ](https://github.com/JaveleyQAQ/SQL-Injection-Scout/issues/5)

## 1.0.1 2025年 1月15日 
* 配置持久化
* 添加新的payload，顺便检测越权
  * 单个参数设置为`null`
  * 将所有参数同时设置为`null`，参数名包含`page`、`size`、`limit`、`count`、`num`除外
  * JSON 设置为`{ }`
* 针对值为`int`类型且不被引号包裹做处理，仅使用`-1`与`null`扫描
* 对每个设置标签添加鼠标停留提示文本
![img_3.png](src/main/resources/img_3.png)
  * 添加新的规则标识
    * 黄色
    * diff 字段 为 `same`，但是文本细节不同时的处理。
    * 例子:
  >   原始
    >   >   {"count": 0, "data":{ }}
    > 
    >   修改后
    > 
    >   >   {"count": 1, "data":{ }}  

## 1.0.0 发布
* 🔥 芜湖～ 1️⃣ giao我里giao giao！ 呀呼～
  ![img.png](src/main/resources/img.png)
