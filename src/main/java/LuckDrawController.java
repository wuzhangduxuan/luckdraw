import com.google.common.util.concurrent.RateLimiter;
import controller.ResultConst;
import controller.SystemConst;
import controller.redis.PrizeResult;
import controller.result.LuckMessage;
import controller.result.ResultBean;
import dao.model.DrawLog;
import dao.model.Prize;
import org.apache.log4j.Logger;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import service.LuckDrawService;
import util.DateUtil;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/18.
 */

@EnableTransactionManagement
@MapperScan("dao.mapper")
@ComponentScan({"service","config"})
@Controller
@SpringBootApplication
public class LuckDrawController {

    @Autowired
    private DataSource dataSource;

    private Logger logger=Logger.getLogger(this.getClass());

    RateLimiter rateLimiter=RateLimiter.create(10);     //限流处理

    @Autowired
    private LuckDrawService luckDrawService;

    @ResponseBody
    @ExceptionHandler(value = Throwable.class)
    public ResultBean<String> handler() throws SQLException {
        ResultBean<String> resultBean=new ResultBean<String>();
        resultBean.setCode(-1);
        resultBean.setMessage(ResultConst.ERRORMESSAGE);
        return resultBean;
    }

    @ResponseBody
    @RequestMapping(value = "/setCountDown")
    public ResultBean<String> setCountDown(String time){
        SystemConst.startLuckDraw=DateUtil.getDate(time);
        ResultBean<String> resultBean=new ResultBean<>();
        resultBean.setCode(1);
        resultBean.setMessage("设置开抢时间为"+SystemConst.startLuckDraw.getTime());
        return resultBean;
    }

    @ResponseBody
    @RequestMapping(value = "/getSystemTime")
    public ResultBean<String> getSystemTime(){
        ResultBean<String> resultBean=new ResultBean<>();
        resultBean.setCode(1);
        String time=DateUtil.getTime(new Date());
        resultBean.setMessage(time);
        return resultBean;
    }

    @ResponseBody
    @RequestMapping(value = "/queryCountDown")
    //查询倒计时
    public ResultBean<String> queryCountDown(){
        ResultBean<String> resultBean=new ResultBean<>();
        Date now=new Date();
        Date start= SystemConst.startLuckDraw;
        if (DateUtil.isExpired(start,now)){
            resultBean.setCode(-1);
            resultBean.setMessage(0+"");
        }else{
            resultBean.setCode(1);
            long diff=DateUtil.diffMS(start,now);
            resultBean.setMessage(diff+"");
        }
        return resultBean;
    }

    @ResponseBody
    @RequestMapping(value = "/queryResult")
    //查询抽奖结果
    public ResultBean<String> queryResult(HttpServletRequest request){
        long start=System.currentTimeMillis();
        List<DrawLog> drawLogs=luckDrawService.getResultByIp(request);
        long end=System.currentTimeMillis();
        logger.info("查询抽奖结果耗时:  "+(end-start)+"ms");
        ResultBean<String> resultBean=new ResultBean<>();
        if (drawLogs.size()==0){
            resultBean.setCode(1);
            resultBean.setMessage(ResultConst.HavedNotLuckDraw);
            return resultBean;
        }else {
            resultBean.setCode(1);
            DrawLog drawLog=drawLogs.get(0);
            if(drawLog.getDrawContent()==1) {
                resultBean.setMessage(ResultConst.HaveGetOne);
            }
            else if (drawLog.getDrawContent()==2){
                resultBean.setMessage(ResultConst.HaveGetSecond);
            }else if (drawLog.getDrawContent()==3){
                resultBean.setMessage(ResultConst.HaveGetThird);
            }else if (drawLog.getDrawContent()==4){
                resultBean.setMessage(ResultConst.HaveGetThank);
            }
            return resultBean;
        }
    }

    //查询奖池接口
    @ResponseBody
    @RequestMapping(value = "/select")
    ResultBean<List<Prize>> home() throws Exception {
        long start=System.currentTimeMillis();
        List<Prize> prizes=luckDrawService.queryAll();
        long end=System.currentTimeMillis();
        logger.info("查询耗时:  "+(end-start)+"ms");
        ResultBean<List<Prize>> resultBean=new ResultBean<>();
        resultBean.setCode(1);
        resultBean.setMessage(prizes);
        return resultBean;
    }

    @ResponseBody
    @RequestMapping(value = "/panic")
    public ResultBean<LuckMessage> luckDraw(HttpServletRequest request){
        long start=System.currentTimeMillis();
        ResultBean<LuckMessage> resultBean=new ResultBean<LuckMessage>();
        LuckMessage luckMessage=new LuckMessage();
        rateLimiter.acquire();
        int luckId=luckDrawService.luckDraw(request);
        if (luckId==ResultConst.NOTGETLEVEL){
            luckMessage.setPrizeLevel(ResultConst.NOTGETLEVEL);
            luckMessage.setLuckMessage(ResultConst.NOTGETLUCK);
        }else if (luckId==ResultConst.FIRSTLEVEL){
            luckMessage.setPrizeLevel(ResultConst.FIRSTLEVEL);
            luckMessage.setLuckMessage(ResultConst.FIRSTLUCK);
        }else if (luckId==ResultConst.SECONDLEVEL){
            luckMessage.setPrizeLevel(ResultConst.SECONDLEVEL);
            luckMessage.setLuckMessage(ResultConst.SECONDLUCK);
        }else if (luckId==ResultConst.THIRDLEVEL){
            luckMessage.setPrizeLevel(ResultConst.THIRDLEVEL);
            luckMessage.setLuckMessage(ResultConst.THIRDLUCK);
        }else if (luckId==ResultConst.THANKLEVEL){
            luckMessage.setPrizeLevel(ResultConst.THANKLEVEL);
            luckMessage.setLuckMessage(ResultConst.THANKLUCK);
        }
        resultBean.setMessage(luckMessage);
        long end=System.currentTimeMillis();
        logger.info("抽奖耗时:  "+(end-start)+"ms");
        return resultBean;
    }

    @ResponseBody
    @RequestMapping("/luckDraw")
    public ResultBean<LuckMessage> LuckDrawByRedis(HttpServletRequest request){

        ResultBean<LuckMessage> resultBean=new ResultBean<LuckMessage>();
        long start=System.currentTimeMillis();
        int luckId=luckDrawService.luckDrawByRedis(request);
        LuckMessage luckMessage=new LuckMessage();
        if (luckId==ResultConst.NOTGETLEVEL){
            luckMessage.setPrizeLevel(ResultConst.NOTGETLEVEL);
            luckMessage.setLuckMessage(ResultConst.NOTGETLUCK);
        }else if (luckId==ResultConst.FIRSTLEVEL){
            luckMessage.setPrizeLevel(ResultConst.FIRSTLEVEL);
            luckMessage.setLuckMessage(ResultConst.FIRSTLUCK);
        }else if (luckId==ResultConst.SECONDLEVEL){
            luckMessage.setPrizeLevel(ResultConst.SECONDLEVEL);
            luckMessage.setLuckMessage(ResultConst.SECONDLUCK);
        }else if (luckId==ResultConst.THIRDLEVEL){
            luckMessage.setPrizeLevel(ResultConst.THIRDLEVEL);
            luckMessage.setLuckMessage(ResultConst.THIRDLUCK);
        }else if (luckId==ResultConst.THANKLEVEL){
            luckMessage.setPrizeLevel(ResultConst.THANKLEVEL);
            luckMessage.setLuckMessage(ResultConst.THANKLUCK);
        }else if (luckId==ResultConst.JACKPOTISNULL){
            luckMessage.setPrizeLevel(ResultConst.JACKPOTISNULL);
            luckMessage.setLuckMessage(ResultConst.JACKPOTLUCK);
        }
        resultBean.setMessage(luckMessage);
        long end=System.currentTimeMillis();
        logger.info("采用redis的抢购耗时:"+(end-start)+"ms");
        return resultBean;
    }


    @ResponseBody
    @RequestMapping("/selectJackpot")
    public ResultBean<PrizeResult> selectJackpot(){
        PrizeResult result=luckDrawService.queryAllByRedis();
        if (result.getFirstSize()<0){
            result.setFirstSize(0);
        }
        if (result.getSecondSize()<0){
            result.setSecondSize(0);
        }
        if (result.getThirdSize()<0){
            result.setThirdSize(0);
        }
        if (result.getThankSize()<0){
            result.setThankSize(0);
        }
        ResultBean<PrizeResult> resultResultBean=new ResultBean<>();
        resultResultBean.setCode(1);
        resultResultBean.setMessage(result);
        return resultResultBean;
    }

    public static void main(String[] args) {
        SpringApplication.run(LuckDrawController.class/*,"--server.port=8081"*/);
    }
}
