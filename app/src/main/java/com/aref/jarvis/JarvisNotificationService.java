package com.aref.jarvis;
import android.service.notification.*;import android.app.*;import android.os.*;import android.content.*;import android.app.RemoteInput;import java.util.*;
public class JarvisNotificationService extends NotificationListenerService{
 Store store;
 @Override public void onCreate(){super.onCreate();store=new Store(this);}
 @Override public void onNotificationPosted(StatusBarNotification sbn){if(!store.away)return;Notification n=sbn.getNotification();if(n==null||n.extras==null)return;
 String text=n.extras.getCharSequence(Notification.EXTRA_TEXT,"").toString();String title=n.extras.getString(Notification.EXTRA_TITLE,"");if(text.isEmpty())return;
 new Thread(()->{String prompt="یک پیام جدید از مخاطب '"+title+"' دریافت شده. پیام: "+text+"\nقوانین: "+store.rules+"\nیک پاسخ کوتاه و طبیعی تولید کن. فقط متن پاسخ را بده.";
 String ans=GeminiClient.ask(store,prompt);sendReply(n,ans);store.log+="\n["+title+"] "+text+"\nJARVIS: "+ans;store.save();}).start();}
 void sendReply(Notification n,String answer){try{if(n.actions==null)return;for(Notification.Action a:n.actions){if(a.getRemoteInputs()!=null&&a.getRemoteInputs().length>0){Intent i=new Intent();Bundle b=new Bundle();b.putCharSequence(a.getRemoteInputs()[0].getResultKey(),answer);RemoteInput.addResultsToIntent(a.getRemoteInputs(),i,b);a.actionIntent.send(this,0,i);return;}}}catch(Exception ignored){}}
}