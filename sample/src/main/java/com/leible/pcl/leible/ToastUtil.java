package com.leible.pcl.leible;



import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;


/***
 * 吐司工具
 */
public class ToastUtil {
    private ToastUtil()
    {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }
    /**
     * 中间显示
     * @param context
     * @param title
     */
    public static void toastInfo(Context context, String title) {
        Toast toast = Toast.makeText(context, title, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    /**
     * 下面显示的Toast
     * @param context
     * @param title
     */
    public static void toastInfobelow(Context context, String title) {
        Toast toast = Toast.makeText(context, title, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
}
