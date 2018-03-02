package cn.com.zhjnc.cloudmusicxposed;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FileUtils {

    public static Context MOD_Context;

    public static void saveDailyList(String name, List list) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Gson gson = new Gson();
            List<String> jsonList = new ArrayList<>();
            for (Object obj : list) {
                String js = gson.toJson(obj);
                jsonList.add(js);
            }
            File file = new File(MOD_Context.getExternalFilesDir(null), name);
            FileOutputStream fos = null;
            ObjectOutputStream os = null;
            try {
                fos = new FileOutputStream(file);
                os = new ObjectOutputStream(fos);
                os.writeObject(jsonList);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    os.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<JSONObject> getDailyList(String name) {
        String state = Environment.getExternalStorageState();
        List<JSONObject> res = new ArrayList<>();

        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File file = new File(MOD_Context.getExternalFilesDir(null), name);
            if (file.exists()) {
                Log.wtf("TestCode", file.getPath());
                FileInputStream fis = null;
                ObjectInputStream is = null;
                try {
                    fis = new FileInputStream(file);
                    is = new ObjectInputStream(fis);
                    List<String> jsonList = (List) is.readObject();
                    for (String s : jsonList) {
                        JSONObject oldJson = new JSONObject(s);
                        JSONObject newJson = new JSONObject();
                        newJson.put("musicId", oldJson.getLong("id"));
                        newJson.put("mvId", oldJson.getLong("mvId"));
                        newJson.put("musicName", oldJson.getString("musicName"));
                        newJson.put("bitrate", oldJson.getInt("currentBitRate"));
                        newJson.put("albumId", oldJson.getJSONObject("album").getLong("id"));
                        newJson.put("album", oldJson.getJSONObject("album").getString("name"));
                        newJson.put("albumPicDocId", oldJson.getJSONObject("album").getLong("imageDocId"));
                        newJson.put("albumPic", oldJson.getJSONObject("album").getString("image"));
                        newJson.put("duration", oldJson.getInt("duration"));
                        JSONArray oldArtists = oldJson.getJSONArray("artists");
                        JSONArray artistArray = new JSONArray();
                        for (int i = 0; i < oldArtists.length(); i++) {
                            JSONArray singer = new JSONArray();
                            singer.put(0, oldArtists.getJSONObject(i).getString("name"));
                            singer.put(1, oldArtists.getJSONObject(i).getLong("id"));
                            artistArray.put(i, singer);
                        }
                        newJson.put("artist", artistArray);
                        res.add(newJson);
                    }
                } catch (FileNotFoundException e) {
//                e.printStackTrace();
                    Log.wtf("TestCode", "FileNotFoundException");
                } catch (IOException e) {
//                e.printStackTrace();
                    Log.wtf("TestCode", "IOException");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    Log.wtf("TestCode", "ClassNotFoundException");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.wtf("TestCode", "JSONException");
                } finally {
                    try {
                        is.close();
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return res;
        }
        return null;
    }

    public static String createFileNameByTime() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String result = "";
        if (hour >= 6) {
            result = year + "_" + month + "_" + day;
        } else {
            result = "0000_00_00";
        }
        return result;
    }
}
