package util;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/24.
 */
public class DateUtil {

    private static Logger logger=Logger.getLogger(DateUtil.class);


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

    public static long diffMS(Date start, Date now){
        long startTimes=start.getTime();
        long nowTimes=now.getTime();
        return startTimes-nowTimes;
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

    public static String getTime(Date date){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return simpleDateFormat.format(date);
    }

    public static Date getDate(String times){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date=new Date();
        try {
            date=simpleDateFormat.parse(times);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return date;
    }

}
