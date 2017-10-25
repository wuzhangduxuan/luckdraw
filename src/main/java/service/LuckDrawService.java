package service;

import dao.mapper.DrawLogMapper;
import dao.mapper.PrizeMapper;
import dao.model.DrawLog;
import dao.model.DrawLogExample;
import dao.model.Prize;
import dao.model.PrizeExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import util.IPUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Random;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/18.
 * luck to draw
 */
@SpringBootApplication

public class LuckDrawService {

    @Autowired
    private DrawLogMapper drawLogMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    //抽奖逻辑设置

    //查询奖池情况
    /**
     * 读使用写锁，防止误读
     */
    public List<Prize> queryAll(){
        PrizeExample example=new PrizeExample();
        List<Prize> prizes=prizeMapper.selectByExample(example);
        return prizes;
    }

    //查询抽奖结果
    public List<DrawLog> getResultByIp(HttpServletRequest request){
        String ip=IPUtil.getIpAddr(request);
        DrawLogExample drawLogExample=new DrawLogExample();
        drawLogExample.createCriteria().andDrawIpEqualTo(ip);
        List<DrawLog> drawLogs=drawLogMapper.selectByExample(drawLogExample);
        return drawLogs;
    }

    //抢购
    //更新采用乐观锁
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = {Throwable.class})
    public int luckDraw(HttpServletRequest request){
        List<Prize> prizes=queryAll();
        int sum=0;
        //统计当前的抢购数量
        Prize firstPrize=prizes.get(0);
        int first=firstPrize.getPrizeSize();
        Prize secondPrize=prizes.get(1);
        int second=secondPrize.getPrizeSize();
        Prize thirdPrize=prizes.get(2);
        int third=thirdPrize.getPrizeSize();
        Prize thankPrize=prizes.get(3);
        int thanks=thankPrize.getPrizeSize();
        sum=first+second+third+thanks;
        Random random=new Random();
        int luckNum=random.nextInt(sum)+1;
        int result=0;
        //扣除库存
        if (luckNum-first<=0){  //一等奖
            if (firstPrize.getPrizeSize()==0){
                throw new NullPointerException("一等奖已被抢购完");
            }
            firstPrize.setPrizeSize(firstPrize.getPrizeSize()-1);
            int oldversion=firstPrize.getVersion();
            firstPrize.setVersion(oldversion+1);
            result=prizeMapper.update(firstPrize,oldversion);
            result=result==1?1:0;
        }else if (luckNum-(first+second)<=0){   //二等奖
            if(secondPrize.getPrizeSize()==0) {
                throw new NullPointerException("二等奖已被抢购完");
            }
            secondPrize.setPrizeSize(secondPrize.getPrizeSize()-1);
            int oldversion=secondPrize.getVersion();
            secondPrize.setVersion(oldversion+1);
            result=prizeMapper.update(secondPrize,oldversion);
            result=result==1?2:0;
        }else if (luckNum-(first+second+third)<=0){ //三等奖
            if (thirdPrize.getPrizeSize()==0){
                throw new NullPointerException("三等奖已被抢购完");
            }
            int oldversion=thirdPrize.getVersion();
            thirdPrize.setPrizeSize(thirdPrize.getPrizeSize()-1);
            thirdPrize.setVersion(oldversion+1);
            result=prizeMapper.update(thirdPrize,oldversion);
            result=result==1?3:0;
        }else{  //感谢奖
            if (thankPrize.getPrizeSize()==0){
                result=0;
            }
            int oldversion=thankPrize.getVersion();
            thankPrize.setPrizeSize(thankPrize.getPrizeSize()-1);
            thankPrize.setVersion(oldversion+1);
            result=prizeMapper.update(thankPrize,oldversion);
            result=result==1?4:0;
        }
        //落盘到日志
        String ip=IPUtil.getIpAddr(request);
        DrawLog drawLog=new DrawLog();
        drawLog.setDrawIp(ip);
        drawLog.setDrawContent(result);
        drawLogMapper.insert(drawLog);
        return result;
    }

    /**
     * it is to judge the ip have draw
     */
    public boolean isIpDrawed(String ip){
        DrawLogExample ipExample=new DrawLogExample();
        ipExample.createCriteria().andDrawIpEqualTo(ip);
        List<DrawLog> ipLists=drawLogMapper.selectByExample(ipExample);
        if (ipLists.size()!=0){
            return false;
        }
        return true;
    }
    

}
