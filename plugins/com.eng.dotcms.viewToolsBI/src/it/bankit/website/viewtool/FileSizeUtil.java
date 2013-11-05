package it.bankit.website.viewtool;

import java.io.File;

import org.apache.velocity.tools.generic.MathTool;

public class FileSizeUtil {

	final static int KILO_BYTE = 1024;
	final static int MEGA_BYTE = 1024 * 1024;
	final static int GIGA_BYTE = 1024 * 1024 * 1024;
	final static int TERA_BYTE = 1024 * 1024 * 1024 * 1024;

	public static String getsize(File fileName) {

		MathTool divider = new MathTool();

		String finalVal;
		long filesize = fileName.length();
		long size = filesize;
		int byteVal;
		Number changedByteVal = null;
		finalVal = "";
		if (filesize <= 0) {
			finalVal = "";
		} else if (filesize < 1000) {
			byteVal = 1;
			if (size > 0) {
				changedByteVal = divider.div(filesize, byteVal);
				finalVal = round(changedByteVal).toString() + " KB";
			}
		} else if (filesize < MEGA_BYTE) {

			byteVal = KILO_BYTE;
			if (size > 0) {
				changedByteVal = divider.div(filesize, byteVal);
				finalVal = round(changedByteVal).toString() + " KB";
			}
		} else if (filesize < GIGA_BYTE) {
			byteVal = MEGA_BYTE;
			if (size > 0) {
				changedByteVal = divider.div(filesize, byteVal);
				finalVal = round(changedByteVal).toString() + " MB";
			}
		} else if (filesize < TERA_BYTE) {
			byteVal = GIGA_BYTE;
			if (size > 0) {
				changedByteVal = divider.div(filesize, byteVal);
				finalVal = round(changedByteVal).toString() + " GB";
			}
		}

		return finalVal;
	}

	public static Number round(Object num) {

		if (num instanceof Number) {
			return Math.round(new Double(String.valueOf(num)));
		}
		try {
			return Math.round(new Double(String.valueOf(num)));
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

}
