# 天气获取问题排查指南

## 快速检查清单

### ✅ 1. 网络连接
- [ ] 设备已连接 Wi-Fi 或移动数据
- [ ] 可以正常访问网页
- [ ] 模拟器网络已开启

### ✅ 2. 定位权限
- [ ] 系统设置中已授予位置权限
- [ ] APP 中已允许定位权限
- [ ] 定位功能已成功获取经纬度

### ✅ 3. 高德地图 API Key
- [ ] 已登录高德开放平台：https://console.amap.com/
- [ ] API Key 状态为"正常"
- [ ] 已勾选"Web服务 API"权限
- [ ] 已勾选"逆地理编码"服务
- [ ] 已勾选"天气查询"服务
- [ ] API Key 未过期
- [ ] 未超出每日调用配额

### ✅ 4. 查看日志
在 Android Studio Logcat 中过滤 `WeatherHelper`，查看详细错误信息

## 常见错误及解决方案

### 错误：API Key 无效 (infocode: 10001)
**解决方案：**
1. 检查 API Key 是否正确复制（无多余空格）
2. 确认 API Key 在高德平台中状态为"正常"
3. 重新生成一个新的 API Key 并替换

### 错误：API Key 权限不足 (infocode: 10002)
**解决方案：**
1. 登录高德开放平台
2. 进入"应用管理" → 选择你的应用
3. 在"服务平台"中勾选：
   - ✅ Web服务 API
   - ✅ 逆地理编码
   - ✅ 天气查询
4. 保存后等待几分钟生效

### 错误：网络连接问题
**解决方案：**
1. 检查设备网络连接
2. 检查防火墙是否拦截
3. 尝试使用移动数据而非 Wi-Fi
4. 检查代理设置

### 错误：无法获取城市编码
**解决方案：**
1. 确认定位已成功获取经纬度
2. 检查逆地理编码 API 是否正常
3. 查看 Logcat 中逆地理编码 API 的返回结果

## 手动测试 API

### 测试逆地理编码 API
在浏览器中访问（替换为你的 API Key）：
```
https://restapi.amap.com/v3/geocode/regeo?location=116.397428,39.90923&key=你的API_KEY
```

预期返回：
```json
{
  "status": "1",
  "regeocode": {
    "addressComponent": {
      "adcode": "110000"
    }
  }
}
```

### 测试天气 API
在浏览器中访问（替换为你的 API Key 和 adcode）：
```
https://restapi.amap.com/v3/weather/weatherInfo?city=110000&key=你的API_KEY
```

预期返回：
```json
{
  "status": "1",
  "lives": [
    {
      "temperature": "20",
      "weather": "晴",
      "winddirection": "北",
      "windpower": "≤3",
      "humidity": "45"
    }
  ]
}
```

## 联系支持
如果以上方法都无法解决问题，请提供：
1. Logcat 中的完整错误日志
2. API Key 的前几位（用于验证格式）
3. 使用的设备类型（真机/模拟器）
4. Android 版本

