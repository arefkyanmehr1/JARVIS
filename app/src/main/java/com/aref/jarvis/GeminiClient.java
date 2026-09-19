package com.aref.jarvis;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import org.json.*;
public class GeminiClient{
 private static final String MODEL="gemini-3.1-flash-lite";
 public static String ask(Store s,String q){
  String prompt="تو JARVIS هستی. قوانین کاربر را دقیق رعایت کن. پاسخ را فقط به شکل متن نهایی بده.\nقوانین:\n"+s.rules+"\n\nپیام:\n"+q;
  String last="خطای ناشناخته Gemini";
  for(int attempt=0;attempt<3;attempt++){String k=s.nextKey();if(k.isEmpty())return "API Key جمینی تنظیم نشده است.";
   try{
    URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent");
    HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(35000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("x-goog-api-key",k);c.setDoOutput(true);
    JSONObject body=new JSONObject();JSONArray contents=new JSONArray();JSONObject item=new JSONObject();JSONArray parts=new JSONArray();parts.put(new JSONObject().put("text",prompt));item.put("parts",parts);contents.put(item);body.put("contents",contents);
    JSONObject gen=new JSONObject();gen.put("maxOutputTokens",512);gen.put("thinkingConfig",new JSONObject().put("thinkingLevel","minimal"));body.put("generationConfig",gen);
    try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
    int code=c.getResponseCode();InputStream in=code<400?c.getInputStream():c.getErrorStream();String raw=new String(in.readAllBytes(),StandardCharsets.UTF_8);
    if(code>=400){last=prettyError(code,raw);if(code==401||code==403||code==429)continue;return last;}
    JSONObject root=new JSONObject(raw);JSONArray cand=root.optJSONArray("candidates");if(cand==null||cand.length()==0)return "Gemini پاسخی تولید نکرد.";
    JSONObject cc=cand.getJSONObject(0).optJSONObject("content");if(cc==null)return "Gemini پاسخ خالی برگرداند.";JSONArray ps=cc.optJSONArray("parts");StringBuilder out=new StringBuilder();
    if(ps!=null)for(int i=0;i<ps.length();i++)out.append(ps.getJSONObject(i).optString("text",""));
    String result=out.toString().trim();return result.isEmpty()?"Gemini پاسخ متنی نداشت.":result;
   }catch(Exception e){last="خطای اتصال به Gemini: "+e.getMessage();}
  }return last;
 }
 private static String prettyError(int code,String raw){try{JSONObject x=new JSONObject(raw);JSONObject e=x.optJSONObject("error");return "Gemini ("+code+"): "+(e==null?raw:e.optString("message","خطای API"));}catch(Exception e){return "Gemini ("+code+"): دسترسی یا تنظیمات API Key را بررسی کن.";}}
}