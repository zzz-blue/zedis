
Java版本的Redis
# 项目功能

* 数据库存储功能
    * [x] 键值存储
    * [x] 键值过期清除
        * [x] 惰性清除
        * [ ] 定期清除
* 基本的客户端服务端，基于RESP协议的网络通信,基于Reactor模式的事件分发与处理
    * [x] 事件循环
    * [x] IO事件
    * [ ] 定时事件
    * [x] RESP协议
    * [x] 基于命令行的客户端服务端
* 命令
	* [x] append
	* [x] del
	* [x] echo
	* [x] exists
	* [x] expire
	* [x] expireat
	* [x] get
	* [x] persist
	* [x] pexpire
	* [x] pexpireat
	* [x] ping
	* [x] psetex
	* [x] psubscribe
	* [x] pttl
	* [x] publish
	* [x] punsubscribe
	* [x] set
	* [x] setex
	* [x] setnx
	* [x] strlen
	* [x] subscribe
	* [x] ttl
	* [x] unsubscribe

* 进阶功能
    * [x] 发布订阅相关功能
  
