## LittleMaster

**版本: v1.5.4**

感谢您使用LittleMaster 副本插件使用本插件时请阅读如下规则
1. 本插件作者为若水仅发布在付费群内 如果您在其他渠道下载到了本插件请尽快删除

2. 本插件是免费插件 但请不要对外出售也不要提供给其他人

3. 每当本插件加载是 本文件都会生成，本文件会提供插件的使用方法以及使用规则

4. 使用本插件时，所有的帮助信息均在本文件，其他功能请自行探索

5. 本插件不适合小白使用且不提供任何的帮助，仅接收bug的反馈，若有其他求助信息均不理会

   

**<u>指令/lt help</u>** 

#### 配置文件

```yaml
头部显示:  "§e{名称} §c❤§7[§a{血量}§e/§b{最大血量}§7]"
皮肤: "粉蓝双瞳猫耳少女"
是否可移动: true
攻击力: 1
血量: 50
防御: 0
大小: 1
攻击距离: 1.3
击退距离: 0.8
视觉距离: 30
是否攻击友好生物: false
是否攻击敌对生物: true
是否被动回击生物: true
是否主动攻击生物: true
攻击速度: 10
阵营: "光明"
是否攻击相同阵营: false
攻击阵营: []
攻击方式: 0
群体攻击范围: 5
恢复血量: 5
恢复间隔: 20
是否仅脱战恢复: true
主动锁定玩家: true
移动速度: 1.6
无敌时间: 30
是否可击退: false
装饰:
  手持: "267:0"
  副手: "267:0"
  帽子: "0:0"
  胸甲: "0:0"
  护腿: "0:0"
  靴子: "0:0"

药水效果:
  - "20:1:5"

未锁定时是否移动: true

显示伤害榜: false
技能:
  "20":
    - 技能名: "@体型"
      效果: 2.0
    - 技能名: "@伤害"
      效果: 7
    - 技能名: "@生成"
      效果: ["a1"]
    - 技能名: "@信息"
      信息: "触发了"
      效果: 1
    - 技能名: "@药水"
      效果: [ "20:1:5"]

死亡掉落:
 # 若为nbt物品则 id: "保存的名称:数量@nbt"
 # 数量可不填 id: "保存的名称@nbt"
  item:
    - id: "264:0:1@item"
      round: 20
  cmd:
    - cmd: ''
      round: 0

公告:
  击杀:
    是否提示: true
    信息: "&e[ &bBOSS提醒 &e] &d{name} 被 {player} 击杀"
  是否公告: true
  死亡:
    是否提示: true
    信息: "&e[ &bBOSS &e] {name} 在坐标: x: {x} y: {y} z: {z} 处死亡"


```
#### 坐标文件

```yaml
刷新怪物: a1
刷怪点:
  x: 0.0
  y: 0.0
  z: 0.0
  level: "world"
是否刷怪: true
刷怪间隔: 10
刷怪数量: 1
刷怪上限: 1
怪物最大位移距离: -1
刷怪距离: 18
是否显示刷怪点: true
# -1时间为永久 单位为秒
怪物存在时间: -1
公告:
  是否提示: true
  时间:
    - 3600
    - 1800
    - 600
    - 300
  信息: "&e[ &bBOSS提醒 &e] &a{name} 将在 {time} 后复活"
  复活提醒: "&e[ &bBOSS提醒 &e] &a{name} 已复活"
刷怪点标题: "§a>>§l§o LittleMonster §r§a<< "
刷怪点浮空字: |-
      §e>>§a-----------------§e<<
      §e>>§f{名称} §d的刷怪点       §e<<
      §e>>§f数量: §a{数量} §e/§c {上限} §e<<
      §e>>§f刷新时间: §b{time} 秒   §e<<
      §e>> {name} 努力挑战吧<<
      §e>>§a-----------------§e<<

```
#### 配置文件
```yaml
npcs:
  # 自定生成的时间 20为1秒
  autospawn-tick: 100
  # 自动生成的地图名称
  worlds-spawning: []

# 格式
# autospawn:
# npc名称:
#    maxCount: 20
#    liveTime: 30
autospawn: {}
```

#### 攻击方式

| 攻击方式 | 介绍                                                  |
| -------- | ----------------------------------------------------- |
| 0        | NPC对玩家使用近战                                     |
| 1        | NPC对范围内玩家群体攻击                               |
| 2        | NPC向目标发射弓箭                                     |
| 3        | NPC触发EntityInteractEvent事件 (可用于开发者接口调用) |

#### 技能列表

| 变量      | 描述                                 | 数据类型(效果)              |
| --------- | ------------------------------------ | --------------------------- |
| @药水     | 给予目标玩家/生物 药水效果           | [] 示例["药水id:等级:时间"] |
| @群体药水 | 给予群体攻击范围内玩家/生物 药水效果 | [] 示例["药水id:等级:时间"] |
| @体型     | 改变NPC体型大小                      | 数值 示例: 1.0              |
| @伤害     | 设置NPC的伤害                        | 数值 示例: 1                |
| @攻速     | 设置NPC的攻击速度 20为1秒            | 数值 示例: 10               |
| @皮肤     | 设置NPC的皮肤                        | 名称 示例  "皮肤名称"       |
| @群体引燃 | 给予目标玩家/生物 燃烧效果           | 数值 示例: 10 引燃10秒      |
| @引燃     | 给予群体攻击范围内玩家/生物 燃烧效果 | 数值 示例: 10 引燃10秒      |
| @群体冰冻 | 给予群体攻击范围内玩家/生物冻结效果  | 数值 示例: 10 冻结10秒      |
| @冰冻     | 给予目标玩家/生物 冻结效果           | 数值 示例: 10 冻结10秒      |
| @范围击退 | 击退群体攻击范围内玩家/生物          | 数值 示例: 0.2              |
| @击退     | 击退目标玩家/生物                    | 数值 示例: 0.2              |
| @信息     | 给攻击过NPC的玩家发送信息            | 内容 示例: "这是一段话"     |
| @生成     | 召唤NPC怪物                          | [] 示例: ["NPC的文件名称"]  |

#### 指令信息

```
/lt clear 清除所有实体  
/lt create <名字>  创建怪物  
/lt del <名字> 删除怪物  
/lt pos <名字> <怪物> 创建怪物点  
/lt dp <名字> 删除怪物点  
/lt spawn <名字> <x> <y> <z> <level> <time(存活时间(秒))> 生成一个怪物  
指令可用 /lt spawn 名称 存活时间 指令代替 存活时间可以不写
也可以/lt spawn 名称 ~ ~ ~ 地图名称 存活时间代替 存活时间可以不写
/lt set 修改怪物数据  
/lt reload 重新加载配置文件  
/lt save <名字> 将手持物品保存在nbtitem.yml
```


#### 死亡执行指令的玩家变量

| 变量       | 介绍                           |
| ---------- | ------------------------------ |
| @target    | NPC锁定的目标玩家/最后一击玩家 |
| @targetAll | 攻击过NPC的所有在线玩家        |
| @damage    | 伤害最高的玩家                 |