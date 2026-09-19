package com.aref.jarvis;
public final class RuleEngine{
 private RuleEngine(){}
 public static String clean(String s){return s==null?"":s.trim().replaceAll("\\s+"," ");}
 public static boolean allow(String number,String incoming,String answer,Store s){if(!s.away||number==null||incoming==null)return false;String a=clean(answer);if(a.isEmpty()||a.length()>1000||!s.canReply(number))return false;String r=s.rules==null?"":s.rules.toLowerCase(java.util.Locale.ROOT);if(r.contains("لینک نده")&&(a.contains("http://")||a.contains("https://")))return false;return true;}
}