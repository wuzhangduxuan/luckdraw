# luckdraw

#### 采用springboot搭建的抽奖接口

启动方式:
##### 1.创建db luckdraw，并运行script里面的sql代码
##### 2.点击LuckDrawController直接启动

```
1.防止读污染，采用mysql自带写锁 for update
2.防止抽奖覆盖，采用乐观锁，在表中增加version，修改表时将版本+1
```

更新:
### 采用alibaba的druid配置，提高响应速度,初始化后查询可达到4ms左右,
### 抢购接口稳定在100ms
