package com.aref.jarvis;
import android.content.*;import android.database.Cursor;import android.provider.ContactsContract;
public final class ContactHelper{
 private ContactHelper(){}
 public static String name(Context c,String n){try{Cursor x=c.getContentResolver().query(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},ContactsContract.PhoneLookup.NUMBER+" LIKE ?",new String[]{"%"+n.replaceAll("[^0-9+]","")+"%"},null);if(x!=null)try{return x.moveToFirst()?x.getString(0):"";}finally{x.close();}}catch(Exception ignored){}return "";}
}