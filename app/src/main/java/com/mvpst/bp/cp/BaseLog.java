package com.mvpst.bp.cp;
import android.util.Log;

public class BaseLog {

	
   
		public static void a(int i, String str, String str2) {
			int length = str2.length();
			int i2 = length / 4000;
			if (i2 > 0) {
				int i3 = 0;
				for (int i4 = 0; i4 < i2; i4++) {
					b(i, str, str2.substring(i3, i3 + 4000));
					i3 += 4000;
				}
				b(i, str, str2.substring(i3, length));
				return;
			}
			b(i, str, str2);
		}

		public static void b(int i, String str, String str2) {
			switch (i) {
				case 1:
					Log.v(str, str2);
					return;
				case 2:
					Log.d(str, str2);
					return;
				case 3:
					Log.i(str, str2);
					return;
				case 4:
					Log.w(str, str2);
					return;
				case 5:
					Log.e(str, str2);
					return;
				case 6:
					Log.wtf(str, str2);
					return;
				default:
					return;
			}
		}
	}

