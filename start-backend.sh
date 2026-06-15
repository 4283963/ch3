#!/bin/bash

echo "=========================================="
echo "  智能风幕柜系统 - 后端启动脚本"
echo "=========================================="

echo ""
echo "检查 MySQL 服务..."
if ! pgrep -x "mysqld" > /dev/null; then
    echo "MySQL 未运行，请先启动 MySQL 服务"
    echo "macOS: brew services start mysql"
    echo "Linux: sudo systemctl start mysql"
    exit 1
fi

echo ""
echo "检查 MQTT Broker..."
if ! pgrep -x "mosquitto" > /dev/null; then
    echo "警告: MQTT Broker 未运行"
    echo "请启动 MQTT Broker:"
    echo "  macOS: brew services start mosquitto"
    echo "  或下载安装: docker run -d -p 1883:1883 eclipse-mosquitto"
    echo ""
    echo "系统将以模拟模式运行..."
fi

echo ""
echo "进入后端目录..."
cd "$(dirname "$0")/backend" || exit 1

echo ""
echo "编译并启动后端服务..."
mvn spring-boot:run
