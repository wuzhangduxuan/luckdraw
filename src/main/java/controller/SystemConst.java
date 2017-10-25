package controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/24.
 */
public class SystemConst {



    public static Date startLuckDraw;

    static {
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            startLuckDraw=simpleDateFormat.parse("2017-10-25 19:06:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
