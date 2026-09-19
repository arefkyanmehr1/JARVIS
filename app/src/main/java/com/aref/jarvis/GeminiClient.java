package com.aref.jarvis;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;
public class GeminiClient{
 static String esc(String s){return s.replace("\\\\","\\\\\\\\").replace("\"","\\\"").replace("\\n","\\\\n");}
 static String ask(Store s,String q){String k=s.nextKey();if(k.isEmpty())return "ابتدا API Key جمینی را در تنظیمات وارد کن.";try{
 String url="https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key="+URLEncoder.encode(k,"UTF-8");
 String prompt="تو JARVIS هستی. قوانین کاربر را دقیق رعایت کن. قوانین:\\n"+s.rules+"\\n\\nحافظه محلی/تاریخچه مرتبط:\\n"+s.log+"\\n\\nپیام کاربر:\\n"+q;
 String body="{\"contents\":[{\"parts\":[{\"text\":\""+esc(prompt)+"\"}]}]}";
 HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
 InputStream in=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();String r=new String(in.readAllBytes(),StandardCharsets.UTF_8);if(c.getResponseCode()>=400)return "Gemini API خطا داد: "+r;
 int x=r.indexOf("\"text\":\"");if(x<0)return r;String z=r.substring(x+8);int end=z.indexOf("\"");return z.substring(0,end).replace("\\n","\n").replace("\\\"","\"");}catch(Exception e){return "خطای اتصال: "+e.getMessage();}}
}