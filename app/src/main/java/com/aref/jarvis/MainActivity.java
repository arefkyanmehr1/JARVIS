package com.aref.jarvis;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.provider.Settings;import android.view.*;import android.widget.*;import java.util.*;
public class MainActivity extends Activity{
 LinearLayout root; TextView status; EditText input; Store store;
 int red=Color.rgb(227,27,35), cyan=Color.rgb(0,217,255);
 @Override public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);build();}
 TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(z);t.setPadding(18,14,18,14);return t;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackgroundColor(Color.rgb(28,34,44));return b;}
 void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,18);root.setBackgroundColor(Color.rgb(7,9,13));
 TextView h=tv("◉ JARVIS",28);h.setTextColor(cyan);root.addView(h);root.addView(tv("AI Messenger • Gemini 3.1 Flash-Lite",14));
 status=tv(store.away?"● Auto Reply فعال":"● Auto Reply خاموش",16);status.setTextColor(store.away?Color.GREEN:red);root.addView(status);
 Button access=btn("فعال‌سازی دسترسی پیام‌ها");access.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));root.addView(access);
 Button away=btn("حالت خواب / Auto Reply");away.setOnClickListener(v->{store.away=!store.away;store.save();build();});root.addView(away);
 Button keys=btn("مدیریت API های Gemini");keys.setOnClickListener(v->apiDialog());root.addView(keys);
 Button rules=btn("قوانین پاسخ‌دهی");rules.setOnClickListener(v->rulesDialog());root.addView(rules);
 root.addView(tv("چت با JARVIS",20));input=new EditText(this);input.setHint("پیام خودت را بنویس...");input.setTextColor(Color.WHITE);input.setHintTextColor(Color.GRAY);root.addView(input);
 Button send=btn("ارسال به Gemini");send.setOnClickListener(v->chat());root.addView(send);
 ScrollView sv=new ScrollView(this);TextView log=tv(store.log,14);sv.addView(log);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
 void apiDialog(){EditText e=new EditText(this);e.setHint("کلید Gemini API");e.setText(store.key);new AlertDialog.Builder(this).setTitle("Gemini API").setMessage("می‌توانی چند کلید را با | جدا کنی.").setView(e).setPositiveButton("ذخیره",(d,w)->{store.key=e.getText().toString().trim();store.save();}).setNegativeButton("لغو",null).show();}
 void rulesDialog(){EditText e=new EditText(this);e.setHint("مثلاً: همیشه فارسی و کوتاه جواب بده؛ به پیام‌های ناشناس پاسخ نده.");e.setMinLines(5);e.setText(store.rules);new AlertDialog.Builder(this).setTitle("قوانین JARVIS").setView(e).setPositiveButton("ذخیره",(d,w)->{store.rules=e.getText().toString();store.save();}).show();}
 void chat(){String q=input.getText().toString().trim();if(q.isEmpty())return;new Thread(()->{String a=GeminiClient.ask(store,q);runOnUiThread(()->{store.log+="\n\nشما: "+q+"\nJARVIS: "+a;store.save();build();});}).start();}
}