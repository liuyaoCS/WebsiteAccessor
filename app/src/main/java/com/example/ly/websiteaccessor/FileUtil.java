package com.example.ly.websiteaccessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.widget.Toast;


/**
 * 文件的缓存与读取
 */
public class FileUtil {



	/**
	 * 获取缓存路径
	 * @param context
	 * @return
	 */
	public static String getCachePath(Context context) {
		String cachePath = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {

				cachePath =context.getExternalCacheDir().getPath() ;

		} else{
			cachePath = context.getCacheDir().getPath();
		}
		return cachePath;
	}


	/**
	 * 保存bitmap到文件
	 * @param bmp
	 * @param filename
	 * @return
	 */
	public static boolean saveBitmap2File(Bitmap bmp, String filename) {
		CompressFormat format = CompressFormat.JPEG;
		int quality = 100;
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bmp.compress(format, quality, stream);
	}
}