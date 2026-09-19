package com.aref.jarvis;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private final int RED=Color.rgb(255,58,68), CYAN=Color.rgb(0,220,255), BG=Color.rgb(5,8,13);
    private final int CARD=Color.rgb(16,22,31), CARD2=Color.rgb(21,29,40), TEXT=Color.rgb(242,246,250), MUTED=Color.rgb(150,160,174);
    private Store store; private LinearLayout content; private TextView status; private EditText input;

    @Override public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);showHome();}

    private GradientDrawable bg(int color,float radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;
    }
    private TextView text(String s,float size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);t.setPadding(18,12,18,12);return t;
    }
    private Button action(String label){
        Button b=new Button(this);b.setText(label);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setGravity(Gravity.CENTER);
        b.setPadding(12,4,12,4);b.setBackground(bg(CARD2,22));return b;
    }
    private LinearLayout card(){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(8,8,8,8);l.setBackground(bg(CARD,28));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,8,0,8);l.setLayoutParams(p);return l;
    }
    private void add(View v){content.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private void showHome(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(18,18,18,28);content.setTextDirection(View.TEXT_DIRECTION_RTL);
        scroll.addView(content);

        TextView brand=text("◉  JARVIS",31,CYAN);brand.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);brand.setPadding(8,8,8,4);add(brand);
        TextView sub=text("دستیار شخصی هوشمند • SMS • Gemini 3.1 Flash-Lite",14,MUTED);sub.setGravity(Gravity.LEFT);add(sub);

        LinearLayout hero=card();
        TextView h=text(store.away?"●  حالت خودکار فعال است":"○  حالت خودکار خاموش است",20,store.away?Color.rgb(70,235,110):RED);
        hero.addView(h);
        TextView hs=text(store.away?"JARVIS در زمان دریافت پیامک می‌تواند پاسخ را تولید و ارسال کند.":"برای شروع، دسترسی پیامک را فعال کن و سپس حالت خودکار را روشن کن.",13,MUTED);
        hero.addView(hs);
        Button toggle=action(store.away?"خاموش کردن Auto Reply":"روشن کردن Auto Reply");
        toggle.setOnClickListener(v->{store.away=!store.away;store.save();showHome();});hero.addView(toggle);
        add(hero);

        LinearLayout setup=card();setup.addView(text("راه‌اندازی SMS",18,TEXT));
        Button perm=action("🔐  دسترسی پیامک و مخاطبین");
        perm.setOnClickListener(v->requestSmsPermissions());setup.addView(perm);
        Button role=action("📱  تنظیم JARVIS به‌عنوان برنامه پیامک");
        role.setOnClickListener(v->requestSmsRole());setup.addView(role);add(setup);

        LinearLayout tools=card();tools.addView(text("تنظیمات هوش مصنوعی",18,TEXT));
        Button keys=action("🔑  مدیریت API Key های Gemini");keys.setOnClickListener(v->apiDialog());tools.addView(keys);
        Button rules=action("⚙️  قوانین پاسخ‌دهی");rules.setOnClickListener(v->rulesDialog());tools.addView(rules);add(tools);

        LinearLayout chat=card();chat.addView(text("گفت‌وگو با JARVIS",18,TEXT));
        input=new EditText(this);input.setHint("پیامت را بنویس…");input.setHintTextColor(MUTED);input.setTextColor(TEXT);input.setTextSize(15);input.setGravity(Gravity.RIGHT|Gravity.TOP);input.setTextDirection(View.TEXT_DIRECTION_RTL);input.setMinLines(3);input.setPadding(18,14,18,14);input.setBackground(bg(Color.rgb(10,15,22),20));chat.addView(input,new LinearLayout.LayoutParams(-1,-2));
        Button send=action("ارسال به Gemini  →");send.setOnClickListener(v->chat());chat.addView(send);add(chat);

        LinearLayout logs=card();logs.addView(text("گزارش فعالیت",18,TEXT));
        TextView log=text(store.log==null||store.log.isEmpty()?"هنوز فعالیتی ثبت نشده است.":store.log,13,MUTED);log.setGravity(Gravity.RIGHT|Gravity.TOP);logs.addView(log);add(logs);

        setContentView(scroll);
    }

    private void requestSmsPermissions(){
        if(Build.VERSION.SDK_INT>=23) requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS,Manifest.permission.READ_CONTACTS},90);
    }
    private void requestSmsRole(){
        if(Build.VERSION.SDK_INT>=29){
            RoleManager rm=getSystemService(RoleManager.class);
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS))
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS),91);
            else Toast.makeText(this,"JARVIS همین حالا برنامه پیش‌فرض پیامک است.",Toast.LENGTH_LONG).show();
        }else{
            try{Intent i=new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,getPackageName());startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName())));}
        }
    }
    private void apiDialog(){
        EditText e=new EditText(this);e.setText(store.key);e.setHint("key1|key2|key3");e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setInputType(1);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("API Key های Gemini").setMessage("چند کلید را با | جدا کن. کلیدها فقط برای استفاده داخل برنامه ذخیره می‌شوند.").setView(e).setPositiveButton("ذخیره",(x,w)->{store.key=e.getText().toString().trim();store.save();}).setNegativeButton("لغو",null).create();d.show();
    }
    private void rulesDialog(){
        EditText e=new EditText(this);e.setText(store.rules);e.setHint("مثلاً: همیشه فارسی، کوتاه و محترمانه جواب بده.");e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setGravity(Gravity.RIGHT|Gravity.TOP);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setMinLines(6);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("قوانین JARVIS").setView(e).setPositiveButton("ذخیره",(x,w)->{store.rules=e.getText().toString().trim();store.save();}).setNegativeButton("لغو",null).create();d.show();
    }
    private void chat(){
        String q=input==null?"":input.getText().toString().trim();if(q.isEmpty())return;
        input.setEnabled(false);new Thread(()->{String a=GeminiClient.ask(store,q);runOnUiThread(()->{store.log+="\n\nشما: "+q+"\nJARVIS: "+a;store.save();showHome();});}).start();
    }
}