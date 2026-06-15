# 智能风幕柜多区化温度联动与自动除霜微调系统

## 项目简介

为某生鲜连锁超市开发的智能风幕柜（开放式冷藏柜）多区化温度联动与自动除霜微调系统。

一个大风幕柜分为三个独立控温区：
- **上层（酸奶区）**：2°C ~ 8°C，默认目标温度 4°C
- **中层（熟食区）**：0°C ~ 6°C，默认目标温度 3°C
- **下层（鲜肉区）**：-2°C ~ 4°C，默认目标温度 0°C

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      店面运维看板 (React)                │
│  ┌──────────┬──────────┬──────────┬──────────────────┐  │
│  │ 三区监控 │ 温度趋势 │ 操作日志 │ 温度调整/联动控制 │  │
│  └──────────┴──────────┴──────────┴──────────────────┘  │
└─────────────────────────────┬───────────────────────────┘
                              │ REST API
┌─────────────────────────────▼───────────────────────────┐
│              后端服务 (Spring Boot + MySQL)             │
│  ┌──────────┬──────────┬──────────┬──────────────────┐  │
│  │ 定时任务 │ MQTT通信 │ 联动逻辑 │ 数据持久化/日志  │  │
│  └──────────┴──────────┴──────────┴──────────────────┘  │
└─────────────────────────────┬───────────────────────────┘
                              │ MQTT
┌─────────────────────────────▼───────────────────────────┐
│               风幕柜控制器 (门店设备)                   │
│  ┌──────────┬──────────┬──────────┬──────────────────┐  │
│  │ 温度采集 │ 结霜检测 │ 风机控制 │ 除霜丝控制       │  │
│  └──────────┴──────────┴──────────┴──────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 核心功能

### 1. 实时温度监控
- 定时（每10秒）通过MQTT读取三层温区的实时温度
- 定时（每30秒）读取蒸发器结霜厚度
- 实时展示各温区的当前温度、目标温度、设备状态

### 2. 目标温度调整
- 调度员可在React界面上微调任意温区的目标温度
- 后端通过MQTT下发温控参数到风幕柜控制器
- 数据库记录完整的调整日志（操作员、时间、原因、MQTT下发状态）

### 3. 智能联动控制
**核心联动逻辑：**
> 一旦中层熟食区的风机增大风量，上层酸奶区的电热除霜丝输出功率就要按比例自动往下微调，防止串温。

- 联动比例：风机转速变化量 × 30% = 除霜功率调整量
- 最小除霜功率保护：不低于20%
- 自动记录所有联动操作日志

### 4. 数据可视化
- 温度趋势图表（近1小时历史数据）
- 操作日志查询（温度调整日志、联动控制日志）
- 结霜厚度进度条显示与告警

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA
- **消息协议**: MQTT (Eclipse Paho + Spring Integration)
- **构建工具**: Maven
- **Java版本**: JDK 17

### 前端
- **框架**: React 18
- **UI组件**: Ant Design 5.x
- **图表**: Recharts
- **HTTP客户端**: Axios
- **日期处理**: Day.js

## API 接口

### 温度控制接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/temperature/zones` | 获取所有温区状态 |
| GET | `/api/temperature/zones/{zone}` | 获取指定温区状态 |
| GET | `/api/temperature/history/{zone}` | 获取温度历史数据 |
| POST | `/api/temperature/adjust` | 调整目标温度 |

### 联动控制接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/linkage/status` | 获取联动状态 |
| POST | `/api/linkage/simulate-fan` | 模拟风机转速变化 |

### 日志查询接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/logs/temperature-control` | 获取温度调整日志 |
| GET | `/api/logs/linkage` | 获取联动控制日志 |

## MQTT 主题定义

### 上报主题 (设备 → 后端)
- `freezer/upper/temperature/report` - 上层温度上报
- `freezer/middle/temperature/report` - 中层温度上报
- `freezer/lower/temperature/report` - 下层温度上报
- `freezer/upper/frost/report` - 上层结霜厚度上报
- `freezer/middle/frost/report` - 中层结霜厚度上报
- `freezer/lower/frost/report` - 下层结霜厚度上报
- `freezer/middle/fan/status` - 中层风机状态上报

### 下发主题 (后端 → 设备)
- `freezer/{zone}/temperature/set` - 设置目标温度
- `freezer/upper/defrost/set` - 设置上层除霜功率

### 消息格式示例

**温度上报:**
```json
{
  "zone": "upper",
  "currentTemp": 4.2,
  "targetTemp": 4.0,
  "timestamp": 1703721600000
}
```

**结霜上报:**
```json
{
  "zone": "upper",
  "frostThickness": 2.5,
  "evaporatorTemp": -8.5,
  "timestamp": 1703721600000
}
```

**风机状态:**
```json
{
  "fanSpeed": 75,
  "defrostPower": 65,
  "compressorStatus": true,
  "timestamp": 1703721600000
}
```

## 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| `temperature_reading` | 温度读数记录表 |
| `frost_reading` | 结霜厚度读数记录表 |
| `temperature_control_log` | 温度调整日志表 |
| `linkage_log` | 联动控制日志表 |
| `device_status` | 设备状态记录表 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- MQTT Broker (Mosquitto 或 EMQX)

### 1. 启动依赖服务

```bash
# 启动 MySQL
brew services start mysql

# 启动 MQTT Broker (可选，系统支持模拟模式)
brew services start mosquitto
# 或使用 Docker
docker run -d -p 1883:1883 eclipse-mosquitto
```

### 2. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS smart_freezer
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_freezer?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: your_username
    password: your_password
```

### 3. 启动后端服务

```bash
# 方式1：使用启动脚本
./start-backend.sh

# 方式2：手动编译启动
cd backend
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 启动前端服务

```bash
# 方式1：使用启动脚本
./start-frontend.sh

# 方式2：手动安装依赖启动
cd frontend
npm install
npm start
```

前端服务将在 `http://localhost:3000` 启动。

## 项目目录结构

```
ch3/
├── backend/                                    # 后端 Spring Boot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/smartfreezer/
│       │   ├── SmartFreezerApplication.java    # 主启动类
│       │   ├── config/                         # 配置类
│       │   │   ├── MqttConfig.java            # MQTT配置
│       │   │   └── FreezerConfig.java         # 温区配置
│       │   ├── controller/                     # REST控制器
│       │   │   ├── TemperatureController.java
│       │   │   ├── LinkageController.java
│       │   │   └── LogController.java
│       │   ├── service/                        # 业务服务层
│       │   │   ├── MqttService.java           # MQTT通信服务
│       │   │   ├── ScheduledTaskService.java  # 定时任务服务
│       │   │   ├── TemperatureLinkageService.java  # 联动逻辑服务
│       │   │   └── TemperatureControlService.java  # 温控服务
│       │   ├── entity/                         # 数据实体
│       │   ├── repository/                     # 数据访问层
│       │   └── dto/                            # 数据传输对象
│       └── resources/
│           ├── application.yml                 # 应用配置
│           └── schema.sql                      # 数据库初始化
├── frontend/                                   # 前端 React 项目
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── App.js                              # 主应用组件
│       ├── index.js                            # 入口文件
│       ├── components/                         # UI组件
│       │   ├── ZoneCard.jsx                   # 温区卡片
│       │   ├── LinkagePanel.jsx                # 联动控制面板
│       │   ├── TemperatureAdjustModal.jsx      # 温度调整弹窗
│       │   ├── TemperatureChart.jsx            # 温度趋势图表
│       │   └── LogTable.jsx                    # 日志表格
│       ├── services/
│       │   └── api.js                          # API服务
│       └── styles/
│           └── index.css                       # 全局样式
├── start-backend.sh                            # 后端启动脚本
├── start-frontend.sh                           # 前端启动脚本
└── README.md                                   # 项目说明
```

## 核心联动逻辑说明

### 温度联动算法

```
当检测到中层风机转速变化时：
  1. 计算风机转速变化量 ΔFan = 新转速 - 旧转速

  2. 如果 ΔFan > 0 (风量增大)：
     - 计算除霜功率下调量 ΔPower = ΔFan × 联动比例(0.3)
     - 新除霜功率 = 旧功率 - ΔPower
     - 确保不低于最小除霜功率(20%)
     - 记录联动日志

  3. 如果 ΔFan < 0 (风量减小)：
     - 计算除霜功率上调量 ΔPower = |ΔFan| × 联动比例(0.3)
     - 新除霜功率 = 旧功率 + ΔPower
     - 确保不超过100%
     - 记录联动日志

  4. 通过MQTT下发新的除霜功率指令
```

### 业务场景

夏季高温时段，顾客频繁开启风幕柜门，中层熟食区需要增大风机风量以维持温度。但风机风量增大会导致冷气向上流动，造成上层酸奶区温度过低，影响酸奶品质。

系统通过自动下调上层除霜丝功率，减少加热量，防止上层温度过低，实现节能与品质保证的双重目标。

## 模拟功能

为便于开发和演示，系统内置了模拟功能：
- 定时任务模拟温度和结霜数据的上报
- 前端可通过滑块模拟风机转速变化，观察联动效果
- 即使没有真实的MQTT Broker和设备，系统也能完整运行

## 开发说明

### 后端开发

```bash
cd backend
mvn clean compile
mvn test
mvn spring-boot:run
```

### 前端开发

```bash
cd frontend
npm install
npm start          # 开发模式
npm run build      # 生产构建
```

## 注意事项

1. **MQTT连接失败**：系统会记录警告日志，但不影响核心功能运行，定时任务会继续模拟数据
2. **数据库连接**：首次启动会自动创建数据表，请确保MySQL服务已启动
3. **时区配置**：系统默认使用 Asia/Shanghai 时区
4. **温度范围校验**：所有温度调整都会经过范围校验，防止误操作

## 扩展建议

1. 增加告警规则配置，当温度偏离目标过远时发送短信/邮件告警
2. 接入更多门店和风幕柜设备，实现多门店集中管理
3. 增加数据统计分析功能，生成月度能耗报告
4. 增加用户权限管理，区分操作员和管理员角色
5. 接入视频监控，可视化展示风幕柜现场状态
