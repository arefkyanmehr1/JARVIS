package com.aref.jarvis;

import android.Manifest;import android.app.*;import android.app.role.RoleManager;import android.content.*;import android.content.pm.PackageManager;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.provider.Telephony;import android.view.*;import android.widget.*;import java.util.*;

public class MainActivity extends Activity{
 private final int RED=Color.rgb(255,57,68),CYAN=Color.rgb(0,220,255),BG=Color.rgb(5,8,13),CARD=Color.rgb(15,21,30),CARD2=Color.rgb(22,30,41),TEXT=Color.rgb(244,247,250),MUTED=Color.rgb(145,156,171),GREEN=Color.rgb(67,232,111);
 private Store store;private JarvisDb db;private FrameLayout body;private LinearLayout nav;private TextView title;
 @Override public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);db=new JarvisDb(this);show("home");}
 @Override protected void onDestroy(){db.close();super.onDestroy();}
 private GradientDrawable box(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
 private TextView t(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setTextDirection(View.TEXT_DIRECTION_RTL);v.setPadding(16,12,16,12);return v;}
 private Button b(String s){Button v=new Button(this);v.setText(s);v.setTextColor(TEXT);v.setTextSize(14);v.setAllCaps(false);v.setBackground(box(CARD2,24));v.setPadding(10,4,10,4);return v;}
 private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(10,10,10,10);l.setBackground(box(CARD,26));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,7,0,7);l.setLayoutParams(p);return l;}
 private void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
 private void base(String name){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(18,18,18,8);
  title=t(name,24,TEXT);title.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  TextView logo=t("◉",28,CYAN);logo.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);top.addView(logo,new LinearLayout.LayoutParams(48,60));root.addView(top);
  body=new FrameLayout(this);body.setPadding(14,0,14,0);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(8,8,8,10);nav.setBackgroundColor(Color.rgb(9,13,19));
  nav.addView(navBtn("خانه","home"));nav.addView(navBtn("پیامک","sms"));nav.addView(navBtn("چت AI","chat"));nav.addView(navBtn("قوانین","rules"));nav.addView(navBtn("تنظیمات","settings"));root.addView(nav,new LinearLayout.LayoutParams(-1,74));
  setContentView(root);
 }
 private Button navBtn(String s,String id){Button x=b(s);x.setTextSize(11);x.setOnClickListener(v->show(id));x.setBackground(box(Color.TRANSPARENT,0));x.setLayoutParams(new LinearLayout.LayoutParams(0,64,1));return x;}
 private ScrollView scroll(LinearLayout l){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.addView(l);return s;}
 private void put(View v){body.removeAllViews();body.addView(v,new FrameLayout.LayoutParams(-1,-1));}
 private void show(String page){
  String n=page.equals("home")?"JARVIS":page.equals("sms")?"پیامک‌های من":page.equals("chat")?"گفت‌وگو با JARVIS":page.equals("rules")?"قوانین هوشمند":"تنظیمات";
  base(n);if(page.equals("home"))home();else if(page.equals("sms"))sms();else if(page.equals("chat"))chat();else if(page.equals("rules"))rules();else settings();
 }
 private void home(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
  LinearLayout hero=card();TextView st=t(store.away?"●  JARVIS آنلاین و آماده است":"○  JARVIS آماده است",22,store.away?GREEN:RED);add(hero,st);
  add(hero,t("دستیار شخصی پیامک با Gemini 3.1 Flash-Lite",13,MUTED));
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button tog=b(store.away?"خاموش":"روشن کردن Auto Reply");tog.setOnClickListener(v->{store.away=!store.away;store.save();show("home");});row.addView(tog,new LinearLayout.LayoutParams(0,52,1));add(hero,row);add(l,hero);
  LinearLayout stats=card();add(stats,t("وضعیت سیستم",17,TEXT));add(stats,t("پیام‌های ذخیره‌شده: "+db.count()+"\nAPI Key: "+(store.key.isEmpty()?"تنظیم نشده":"تنظیم شده")+"\nحالت خودکار: "+(store.away?"فعال":"خاموش"),14,MUTED));add(l,stats);
  LinearLayout quick=card();add(quick,t("دسترسی سریع",17,TEXT));Button q1=b("📩  راه‌اندازی پیامک");q1.setOnClickListener(v->show("settings"));add(quick,q1);Button q2=b("🔑  مدیریت Gemini");q2.setOnClickListener(v->apiDialog());add(quick,q2);Button q3=b("🧠  قوانین پاسخ");q3.setOnClickListener(v->show("rules"));add(quick,q3);add(l,quick);
  LinearLayout log=card();add(log,t("آخرین فعالیت",17,TEXT));add(log,t(store.log==null||store.log.isEmpty()?"هنوز فعالیتی ثبت نشده است.":store.log,13,MUTED));add(l,log);put(scroll(l));
 }
 private void sms(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);add(l,t("تاریخچه کاملاً محلی",17,TEXT));add(l,t("پیام‌ها روی خود گوشی ذخیره می‌شوند و برای نمایش تاریخچه نیازی به سرور جداگانه نیست.",12,MUTED));
  LinearLayout c=card();String x=db.recentAll(80);add(c,t(x.isEmpty()?"هنوز پیامکی ثبت نشده است.":x,14,TEXT));add(l,c);put(scroll(l));
 }
 private void chat(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);add(l,t("چت خصوصی با Gemini",18,TEXT));add(l,t("این بخش مستقل از Auto Reply است.",12,MUTED));
  EditText e=new EditText(this);e.setHint("پیامت را بنویس…");e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setGravity(Gravity.RIGHT|Gravity.TOP);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setMinLines(4);e.setBackground(box(Color.rgb(10,15,22),20));add(l,e);
  Button send=b("ارسال به Gemini  →");add(l,send);TextView result=t("",15,TEXT);result.setGravity(Gravity.RIGHT|Gravity.TOP);add(l,result);
  send.setOnClickListener(v->{String q=e.getText().toString().trim();if(q.isEmpty())return;send.setEnabled(false);result.setText("در حال فکر کردن…");new Thread(()->{String a=GeminiClient.ask(store,q);runOnUiThread(()->{result.setText(a);send.setEnabled(true);db.event("chat",q);});}).start();});put(scroll(l));
 }
 private void rules(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);LinearLayout c=card();add(c,t("قوانین JARVIS",19,TEXT));add(c,t("قوانین در prompt ارسال می‌شوند و چند محدودیت مهم نیز قبل از ارسال SMS به‌صورت قطعی بررسی می‌شوند.",12,MUTED));
  EditText e=new EditText(this);e.setText(store.rules);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setGravity(Gravity.RIGHT|Gravity.TOP);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setMinLines(9);e.setBackground(box(Color.rgb(10,15,22),18));add(c,e);Button save=b("ذخیره قوانین");save.setOnClickListener(v->{store.rules=e.getText().toString().trim();store.save();Toast.makeText(this,"قوانین ذخیره شد",Toast.LENGTH_SHORT).show();});add(c,save);add(l,c);
  LinearLayout examples=card();add(examples,t("قواعد پیشنهادی",17,TEXT));add(examples,t("• همیشه فارسی و طبیعی\n• پاسخ کوتاه و محترمانه\n• اطلاعات حساس را بازگو نکن\n• بدون اجازه لینک ارسال نکن\n• در موارد نامطمئن پاسخ نده",14,MUTED));add(l,examples);put(scroll(l));
 }
 private void settings(){
  LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
  LinearLayout p=card();add(p,t("دسترسی‌ها",18,TEXT));Button sms=b("🔐  مجوز SMS و مخاطبین");sms.setOnClickListener(v->requestSmsPermissions());add(p,sms);Button role=b("📱  برنامه پیش‌فرض پیامک");role.setOnClickListener(v->requestSmsRole());add(p,role);add(l,p);
  LinearLayout ai=card();add(ai,t("هوش مصنوعی",18,TEXT));Button api=b("🔑  API Key های Gemini");api.setOnClickListener(v->apiDialog());add(ai,api);add(ai,t("مدل فعال: gemini-3.1-flash-lite",13,CYAN));add(l,ai);
  LinearLayout sec=card();add(sec,t("امنیت و داده",18,TEXT));add(sec,t("API Key با Android Keystore رمزنگاری می‌شود. تاریخچه و تنظیمات روی دستگاه نگهداری می‌شوند.",12,MUTED));Button clear=b("پاک‌کردن گزارش فعالیت");clear.setOnClickListener(v->{store.log="";store.save();show("settings");});add(sec,clear);add(l,sec);
  put(scroll(l));
 }
 private void requestSmsPermissions(){if(Build.VERSION.SDK_INT>=23)requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS,Manifest.permission.READ_CONTACTS},90);}
 private void requestSmsRole(){if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_SMS)&&!r.isRoleHeld(RoleManager.ROLE_SMS))startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_SMS),91);else Toast.makeText(this,"JARVIS در حال حاضر برنامه پیش‌فرض پیامک است.",Toast.LENGTH_LONG).show();}else{try{Intent i=new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,getPackageName());startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}}}
 private void apiDialog(){EditText e=new EditText(this);e.setText(store.key);e.setHint("key1|key2|key3");e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setInputType(1);new AlertDialog.Builder(this).setTitle("Gemini API Keys").setMessage("کلیدهای متعدد را با | جدا کن. در لاگ نمایش داده نمی‌شوند.").setView(e).setPositiveButton("ذخیره",(d,w)->{store.key=e.getText().toString().trim();store.save();}).setNegativeButton("لغو",null).show();}
}