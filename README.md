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
### 抢购接口稳定在100ms，针对于已抢购过的ip进行拦截，同时进行简单的防止爬虫

更新:
### 使用springboot的maven插件打包成可执行jar
打包及启动流程:
### 1.直接在项目目录luckdraw下运行mvn package即可，建议加上 -X方便看打包流程
### 2.可以看到target目录下生成luck.draw-1.0-SNAPSHOT.jar
### 3.启动luckdraw,在target目录下面运行java -jar luck.draw-1.0-SNAPSHOT.jar即可启动

接下来更新:
### 利用redis来实现抢购
### 具体做法:
### 1.针对于写覆盖，利用redis的特性,watch以及事务来实现抢购
### 2.针对于抢空覆盖，则采用redis回调来实现
### 目前测试，抢购可达到5ms