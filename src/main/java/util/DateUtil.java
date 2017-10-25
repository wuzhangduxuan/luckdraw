package util;

import java.util.Date;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/24.
 */
public class DateUtil {


    public static String diffDate(Date start, Date now){
        long startTimes=start.getTime();
        long nowTimes=now.getTime();
        long diff=startTimes-nowTimes;
        long day=diff/(1000*60*60*24);
        long hour=diff%(1000*60*60*24)/(1000*60*60);
        long minu=diff%(1000*60*60*24)%(1000*60*60)/(1000*60);
        long mm=diff%(1000*60*60*24)%(1000*60*60)%(1000*60)/1000;
        return day+"天"+hour+"时"+minu+"分"+mm+"秒";
    }

    public static boolean isExpired(Date start, Date now){
        long startTimes=start.getTime();
        long nowTimes=now.getTime();
        if (startTimes>nowTimes){
            return false;
        }else{
            return true;
        }
    }

}
