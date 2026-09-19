package com.aref.jarvis;
import android.content.*;import android.database.sqlite.*;import android.database.Cursor;
public class JarvisDb extends SQLiteOpenHelper{
 public JarvisDb(Context c){super(c,"jarvis.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,address TEXT,name TEXT,body TEXT NOT NULL,direction INTEGER NOT NULL,ts INTEGER NOT NULL,status TEXT)");d.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,detail TEXT,ts INTEGER NOT NULL)");d.execSQL("CREATE INDEX idx_msg ON messages(address,ts)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 public synchronized void message(String a,String n,String b,int dir,String st){ContentValues v=new ContentValues();v.put("address",a);v.put("name",n);v.put("body",b);v.put("direction",dir);v.put("ts",System.currentTimeMillis());v.put("status",st);getWritableDatabase().insert("messages",null,v);}
 public synchronized void event(String t,String x){ContentValues v=new ContentValues();v.put("type",t);v.put("detail",x);v.put("ts",System.currentTimeMillis());getWritableDatabase().insert("events",null,v);}
 public String recent(String a,int limit){Cursor c=getReadableDatabase().query("messages",new String[]{"body","direction"},"address=?",new String[]{a},null,null,"ts DESC",String.valueOf(limit));StringBuilder s=new StringBuilder();try{while(c.moveToNext())s.insert(0,(c.getInt(1)==1?"JARVIS: ":"User: ")+c.getString(0)+"\n");}finally{c.close();}return s.toString();}
 public int count(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM messages",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
}