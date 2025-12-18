package com.example.reverseclock;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 天气工具类（和风天气V7 API）：
 * - 通过经纬度直接调用和风天气API获取实时天气信息
 * - 解析温度/天气描述/紫外线指数等信息
 * - 根据结果生成“是否带伞/是否防晒”的 AI 风格建议
 */
public class WeatherHelper {

    // 和风天气API Key
    private static final String API_KEY = "850ffe6f1c094b9d9ea5d082fe0be2c4";
    
    // API基础URL：使用你提供的专用域名
    private static final String API_BASE_URL = "https://nx7bygjncn.re.qweatherapi.com";

    // 使用地理坐标获取天气（和风天气V7 API）
    public static WeatherInfo fetchWeatherByLocation(Location location) {
        if (location == null) return null;
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        try {
            Log.d("WeatherHelper", "开始获取天气，经纬度: " + lon + "," + lat);
            
            // 和风天气V7：实时天气API（location格式为"经度,纬度"）
            String weatherUrl = API_BASE_URL + "/v7/weather/now?location="
                    + lon + "," + lat + "&key=" + API_KEY;
            
            Log.d("WeatherHelper", "开始调用和风天气API");
            String resp = doGet(weatherUrl);
            if (resp == null) {
                Log.e("WeatherHelper", "天气API返回为空");
                return null;
            }
            
            JSONObject json = new JSONObject(resp);
            
            // 检查是否有错误对象（403等错误会返回error对象）
            if (json.has("error")) {
                JSONObject error = json.optJSONObject("error");
                String errorTitle = error != null ? error.optString("title", "未知错误") : "未知错误";
                String errorDetail = error != null ? error.optString("detail", "") : "";
                int errorStatus = error != null ? error.optInt("status", 0) : 0;
                Log.e("WeatherHelper", "和风天气API返回错误 - status: " + errorStatus + ", title: " + errorTitle + ", detail: " + errorDetail);
                return null;
            }
            
            // 检查返回状态（和风天气：code为"200"表示成功）
            String code = json.optString("code", "");
            if (!"200".equals(code)) {
                String msg = json.optString("msg", "未知错误");
                Log.e("WeatherHelper", "和风天气API返回错误 - code: " + code + ", msg: " + msg);
                return null;
            }

            // 解析天气信息（now对象）
            JSONObject now = json.optJSONObject("now");
            if (now == null) {
                Log.e("WeatherHelper", "天气数据为空，now对象不存在");
                return null;
            }
            
            // 解析温度（和风天气返回的是字符串）
            String tempStr = now.optString("temp", "0");
            double temp = 0;
            try {
                temp = Double.parseDouble(tempStr);
            } catch (NumberFormatException e) {
                Log.e("WeatherHelper", "温度解析失败：" + tempStr);
            }

            String weather = now.optString("text", "未知"); // 天气状况文字描述
            String windDir = now.optString("windDir", ""); // 风向
            String windScale = now.optString("windScale", ""); // 风力等级
            String humidity = now.optString("humidity", ""); // 相对湿度

            // 获取紫外线指数（需要单独调用UV API）
            double uvIndex = getUvIndex(lon, lat);

            // 生成 AI 风格的建议文案
            String suggestion = buildSuggestion(temp, uvIndex, weather, windDir, windScale, humidity);

            WeatherInfo info = new WeatherInfo();
            info.tempC = temp;
            info.uvIndex = uvIndex;
            info.weatherMain = weather;
            info.weatherDesc = weather + (windDir.isEmpty() ? "" : "，风向" + windDir + " " + windScale + "级");
            info.suggestion = suggestion;
            return info;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            Log.e("WeatherHelper", "获取天气失败，异常信息：" + errorMsg);
            Log.e("WeatherHelper", "异常堆栈：", e);
            e.printStackTrace();
            return null;
        }
    }

    // 获取紫外线指数（和风天气V7 UV API）
    private static double getUvIndex(double longitude, double latitude) {
        try {
            String uvUrl = API_BASE_URL + "/v7/uv/now?location="
                    + longitude + "," + latitude + "&key=" + API_KEY;
            
            String resp = doGet(uvUrl);
            if (resp == null) {
                Log.w("WeatherHelper", "UV API返回为空，使用估算值");
                return 3.0;
            }
            
            JSONObject json = new JSONObject(resp);
            String code = json.optString("code", "");
            if (!"200".equals(code)) {
                Log.w("WeatherHelper", "UV API返回错误，使用估算值");
                return 3.0;
            }
            
            JSONObject now = json.optJSONObject("now");
            if (now == null) {
                Log.w("WeatherHelper", "UV数据为空，使用估算值");
                return 3.0;
            }
            
            String uvStr = now.optString("uvIndex", "3");
            try {
                double uv = Double.parseDouble(uvStr);
                Log.d("WeatherHelper", "成功获取紫外线指数: " + uv);
                return uv;
            } catch (NumberFormatException e) {
                Log.w("WeatherHelper", "UV指数解析失败，使用估算值");
                return 3.0;
            }
        } catch (Exception e) {
            Log.w("WeatherHelper", "获取UV指数失败，使用估算值: " + e.getMessage());
            return 3.0;
        }
    }

    // 通过城市名称获取天气（优先使用）
    public static WeatherInfo fetchWeatherByCityName(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) return null;
        
        try {
            Log.d("WeatherHelper", "开始通过城市名称获取天气: " + cityName);
            
            // 第一步：通过城市名称搜索获取location ID和经纬度
            // 使用正确的地理API路径：/geo/v2/city/lookup
            String cityUrl = API_BASE_URL + "/geo/v2/city/lookup?location=" 
                    + java.net.URLEncoder.encode(cityName, "UTF-8") + "&key=" + API_KEY;
            
            String resp = doGet(cityUrl);
            if (resp == null) {
                Log.e("WeatherHelper", "城市搜索API返回为空");
                return null;
            }
            
            JSONObject json = new JSONObject(resp);
            String code = json.optString("code", "");
            if (!"200".equals(code)) {
                String msg = json.optString("msg", "未知错误");
                Log.e("WeatherHelper", "城市搜索API返回错误 - code: " + code + ", msg: " + msg);
                return null;
            }
            
            JSONArray locationArray = json.optJSONArray("location");
            if (locationArray == null || locationArray.length() == 0) {
                Log.e("WeatherHelper", "未找到该城市");
                return null;
            }
            
            // 取第一个结果
            JSONObject location = locationArray.getJSONObject(0);
            String lonStr = location.optString("lon", "");
            String latStr = location.optString("lat", "");
            
            if (lonStr.isEmpty() || latStr.isEmpty()) {
                Log.e("WeatherHelper", "城市坐标为空");
                return null;
            }
            
            double lon = Double.parseDouble(lonStr);
            double lat = Double.parseDouble(latStr);
            
            Log.d("WeatherHelper", "城市坐标: " + lon + "," + lat);
            
            // 第二步：使用经纬度获取天气
            String weatherUrl = API_BASE_URL + "/v7/weather/now?location="
                    + lon + "," + lat + "&key=" + API_KEY;
            
            String weatherResp = doGet(weatherUrl);
            if (weatherResp == null) {
                Log.e("WeatherHelper", "天气API返回为空");
                return null;
            }
            
            JSONObject weatherJson = new JSONObject(weatherResp);
            
            // 检查是否有错误
            if (weatherJson.has("error")) {
                JSONObject error = weatherJson.optJSONObject("error");
                String errorTitle = error != null ? error.optString("title", "未知错误") : "未知错误";
                Log.e("WeatherHelper", "和风天气API返回错误: " + errorTitle);
                return null;
            }
            
            // 检查返回状态
            String weatherCode = weatherJson.optString("code", "");
            if (!"200".equals(weatherCode)) {
                String msg = weatherJson.optString("msg", "未知错误");
                Log.e("WeatherHelper", "和风天气API返回错误 - code: " + weatherCode + ", msg: " + msg);
                return null;
            }

            // 解析天气信息
            JSONObject now = weatherJson.optJSONObject("now");
            if (now == null) {
                Log.e("WeatherHelper", "天气数据为空，now对象不存在");
                return null;
            }
            
            String tempStr = now.optString("temp", "0");
            double temp = 0;
            try {
                temp = Double.parseDouble(tempStr);
            } catch (NumberFormatException e) {
                Log.e("WeatherHelper", "温度解析失败：" + tempStr);
            }

            String weather = now.optString("text", "未知");
            String windDir = now.optString("windDir", "");
            String windScale = now.optString("windScale", "");
            String humidity = now.optString("humidity", "");

            // 获取紫外线指数
            double uvIndex = getUvIndex(lon, lat);

            // 生成建议文案
            String suggestion = buildSuggestion(temp, uvIndex, weather, windDir, windScale, humidity);

            WeatherInfo info = new WeatherInfo();
            info.tempC = temp;
            info.uvIndex = uvIndex;
            info.weatherMain = weather;
            info.weatherDesc = weather + (windDir.isEmpty() ? "" : "，风向" + windDir + " " + windScale + "级");
            info.suggestion = suggestion;
            return info;
        } catch (Exception e) {
            Log.e("WeatherHelper", "通过城市名称获取天气失败：" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    static String doGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            Log.d("WeatherHelper", "请求URL: " + urlStr);
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000); // 增加超时时间
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("User-Agent", "ReverseClock-Android");
            conn.setDoInput(true);
            
            Log.d("WeatherHelper", "开始连接...");
            conn.connect();
            
            int code = conn.getResponseCode();
            Log.d("WeatherHelper", "HTTP响应码: " + code);
            
            if (code < 200 || code >= 300) {
                // HTTP错误，读取错误流
                is = conn.getErrorStream();
                if (is != null) {
                    br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder errorSb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorSb.append(line);
                    }
                    String errorResponse = errorSb.toString();
                    Log.e("WeatherHelper", "HTTP错误响应: " + errorResponse);
                    throw new Exception("HTTP错误 " + code + ": " + errorResponse);
                } else {
                    throw new Exception("HTTP错误 " + code + ": 无法读取错误响应");
                }
            }
            
            // 成功响应，读取输入流
            is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String response = sb.toString();
            Log.d("WeatherHelper", "API响应长度: " + response.length() + " 字符");
            Log.d("WeatherHelper", "API响应内容: " + response);
            return response;
        } catch (java.net.SocketTimeoutException e) {
            Log.e("WeatherHelper", "网络请求超时: " + e.getMessage());
            throw new Exception("网络请求超时，请检查网络连接");
        } catch (java.net.UnknownHostException e) {
            Log.e("WeatherHelper", "无法解析主机: " + e.getMessage());
            throw new Exception("无法连接到服务器，请检查网络连接");
        } catch (java.io.IOException e) {
            Log.e("WeatherHelper", "IO异常: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("网络IO错误: " + e.getMessage());
        } catch (Exception e) {
            Log.e("WeatherHelper", "网络请求异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            // 确保资源释放
            try {
                if (br != null) br.close();
                if (is != null) is.close();
            } catch (Exception e) {
                Log.w("WeatherHelper", "关闭流时出错: " + e.getMessage());
            }
            if (conn != null) conn.disconnect();
        }
    }

    // 简单的“AI 风格”提示逻辑（适配和风天气数据）
    private static String buildSuggestion(double temp, double uv, String weather, 
                                         String windDir, String windScale, String humidity) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前天气：").append(weather);
        if (!windDir.isEmpty()) {
            sb.append("，").append(windDir).append("风");
            if (!windScale.isEmpty()) {
                sb.append(" ").append(windScale).append("级");
            }
        }
        sb.append("，气温 ").append(Math.round(temp)).append("℃");
        if (!humidity.isEmpty()) {
            sb.append("，湿度 ").append(humidity).append("%");
        }
        sb.append("。\n");
        sb.append("紫外线指数：").append(String.format("%.1f", uv)).append("（0-11+）。\n");

        // 是否带伞（根据天气描述判断）
        boolean mayRain = false;
        if (weather != null) {
            String lower = weather.toLowerCase();
            // 和风天气的天气描述：雨、小雨、中雨、大雨、暴雨、雷阵雨等
            if (lower.contains("雨") || lower.contains("雷") || lower.contains("暴雨") 
                || lower.contains("小雨") || lower.contains("中雨") || lower.contains("大雨")
                || lower.contains("阵雨") || lower.contains("雷阵雨")) {
                mayRain = true;
            }
        }
        if (mayRain) {
            sb.append("建议：今天可能有降水，出门最好带把伞，以防万一。\n");
        } else {
            sb.append("建议：今天降水概率不高，可以不用特意带伞（如果担心天气多变可以带折叠伞）。\n");
        }

        // 是否防晒（根据实际UV指数）
        if (uv >= 6) {
            sb.append("防晒：紫外线偏强，建议出门前涂防晒霜，戴好帽子和太阳镜。");
        } else if (uv >= 3) {
            sb.append("防晒：紫外线中等，户外时间较长时建议简单防晒。");
        } else {
            sb.append("防晒：紫外线较弱，日常通勤可以适当放松，但皮肤敏感仍可轻度防晒。");
        }
        return sb.toString();
    }

    // 简单数据模型
    public static class WeatherInfo {
        public double tempC;
        public double uvIndex;
        public String weatherMain;
        public String weatherDesc;
        public String suggestion;
    }
}


