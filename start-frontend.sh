#!/bin/bash

echo "=========================================="
echo "  智能风幕柜系统 - 前端启动脚本"
echo "=========================================="

echo ""
echo "进入前端目录..."
cd "$(dirname "$0")/frontend" || exit 1

echo ""
echo "检查依赖..."
if [ ! -d "node_modules" ]; then
    echo "首次启动，正在安装依赖..."
    npm install
fi

echo ""
echo "启动前端开发服务器..."
npm start
