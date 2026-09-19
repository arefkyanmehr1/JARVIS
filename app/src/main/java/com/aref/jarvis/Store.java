package com.aref.jarvis;
import android.content.*;import java.util.*;
public class Store{
 SharedPreferences p; boolean away;String key,rules,log;
 Store(Context c){p=c.getSharedPreferences("jarvis",0);away=p.getBoolean("away",false);key=p.getString("keys","");rules=p.getString("rules","همیشه فارسی، طبیعی، کوتاه و محترمانه پاسخ بده.");log=p.getString("log","");}
 void save(){p.edit().putBoolean("away",away).putString("keys",key).putString("rules",rules).putString("log",log).apply();}
 String nextKey(){String[] a=key.split("\\|");if(a.length==0)return "";int i=p.getInt("ki",0)%a.length;p.edit().putInt("ki",(i+1)%a.length).apply();return a[i].trim();}
}