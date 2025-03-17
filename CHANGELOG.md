# 更新日志
## 1.0.5
- 添加日志表格排序
- 简化`Flag`提示内容，有趣使用`√`否则为`×`
- 支持用户在`Fuzz Params List`列表中 ( 一行一个参数) 自定义原请求参数，用于`Fuzz`隐藏参数。

## 1.0.4
- 添加`Boring Words`配置，支持用户自定义过滤无趣的响应内容
- 结果排序更改，优先按照参数名称进行排序
- 对于内置的`'''`和`''''`，`#{}`和`#}`单独计算颜色
- 删除了大部分`payload`
- 

## 1.0.3
- 将大于`Max Param Count`的请求加入日志列表，并使用`ExcessParams`标记
- 对`null`扫描添加逻辑，忽略`302`状态码时依然为`interesting`
- 对`302`进行扫描
- 修改了内置一些`payload`



## 1.0.2 2025年 1月16日
- 修复一直处于`scanning`扫描状态[#3 ](https://github.com/JaveleyQAQ/SQL-Injection-Scout/issues/5)

## 1.0.1 2025年 1月15日 
* 配置持久化
* 添加新的`payload`，顺便检测越权
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
