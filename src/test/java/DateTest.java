import org.apache.log4j.Logger;
import org.junit.Test;
import util.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/24.
 */
public class DateTest {

    private Logger logger=Logger.getLogger(this.getClass());

    @Test
    public void test() throws ParseException {
        Date now=new Date();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date start=simpleDateFormat.parse("2017-10-24 10:30:00");
        String st=DateUtil.diffDate(start,now);
        logger.info(st);
    }

}
