package com.aref.jarvis;

import android.content.*;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Store {
    private static final String PREF="jarvis_secure";
    private static final String ALIAS="JARVIS_AES";
    private final SharedPreferences p;
    public boolean away;
    public String key="", rules="", log="";

    public Store(Context c){
        p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        away=p.getBoolean("away",false);
        rules=p.getString("rules","همیشه فارسی، طبیعی، کوتاه و محترمانه پاسخ بده.");
        log=p.getString("log","");
        String encrypted=p.getString("keys_secure","");
        if(!encrypted.isEmpty()) key=decrypt(encrypted);
        else {
            String legacy=p.getString("keys","");
            if(!legacy.isEmpty()){key=legacy;save();}
        }
    }
    public synchronized void save(){
        SharedPreferences.Editor e=p.edit().putBoolean("away",away).putString("rules",rules).putString("log",trimLog(log));
        if(key!=null&&!key.isEmpty()) e.putString("keys_secure",encrypt(key));
        else e.remove("keys_secure");
        e.remove("keys").apply();
    }
    public synchronized String nextKey(){
        String[] a=key==null?new String[0]:key.split("\\|");
        java.util.ArrayList<String> valid=new java.util.ArrayList<>();
        for(String x:a)if(!x.trim().isEmpty())valid.add(x.trim());
        if(valid.isEmpty())return "";
        int i=p.getInt("ki",0)%valid.size();
        p.edit().putInt("ki",(i+1)%valid.size()).apply();
        return valid.get(i);
    }
    public synchronized void addLog(String line){log=(log==null?"":log)+"\n"+line;log=trimLog(log);save();}
    public synchronized boolean seen(String number,String body){String k="seen_"+Integer.toHexString((number+"|"+body).hashCode());if(p.getBoolean(k,false))return true;p.edit().putBoolean(k,true).apply();return false;}\n    public synchronized boolean canReply(String number){
        long last=p.getLong("last_"+safe(number),0);
        return System.currentTimeMillis()-last>=60000;
    }
    public synchronized void markReply(String number){p.edit().putLong("last_"+safe(number),System.currentTimeMillis()).apply();}
    private String safe(String s){return s==null?"unknown":s.replaceAll("[^0-9A-Za-z_]","_");}
    private String trimLog(String s){if(s==null)return "";return s.length()>16000?s.substring(s.length()-16000):s;}
    private SecretKey secret(){
        try{
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
            if(!ks.containsAlias(ALIAS)){
                KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
                kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
                kg.generateKey();
            }
            return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        }catch(Exception e){return null;}
    }
    private String encrypt(String plain){
        try{
            SecretKey k=secret();if(k==null)return "";
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,k);
            byte[] iv=c.getIV(), data=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(iv,2)+"."+Base64.encodeToString(data,2);
        }catch(Exception e){return "";}
    }
    private String decrypt(String value){
        try{
            String[] a=value.split("\\.",2);if(a.length!=2)return "";
            SecretKey k=secret();if(k==null)return "";
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,k,new GCMParameterSpec(128,Base64.decode(a[0],2)));
            return new String(c.doFinal(Base64.decode(a[1],2)),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
}