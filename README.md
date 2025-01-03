


---- 
## 0.1
- 暂时只支持对**GET/POST**的参数做**FUZZ**
- 支持xml/json/form
- 最小化探测`payload` 
- 对`response`做`diff`判断，灰色为无趣的（同`same`），不一定准确。
  - `Interesting` 说明值得一看
  - `Boring`  可跳过
  - 判断原理为：假设**页面的参数都为反射类型，对`payload`和`diff`长度比较，相同认为无趣（没有对`response`中是否包含做判断，没那么严谨，考虑到宽字节会匹配不到）** 
    - 其他情况都默认打上绿色标记
    - 将绿色标记的分组，继续判断 diff 内容， 默认是出现9次以上重复的diff设置为无趣的😑
    - 对最终结果根据颜色排序展示
- 自动在扫描页面的`resposne`中匹配`diff`结果（当存在多处修改时默认取第一处）
- 支持内置`scope`范围

## todo
* `null`
* 颜色自定义？
* 可查看多处diff内容
* 可选二次确认存在注入的条目


// 多种规则判断 Boring
* diff.all == same as color.gray   // All diff String are "same"  as color gray
* diff.+length == payload.length  &&  color == null   as color.gray  
* diff.+/-length != payload.length && color == null as color.Green 
* color.all == green  && count >=9  && diff.len.all == same  as color.gray
* color.all == gray as Flag Boring
