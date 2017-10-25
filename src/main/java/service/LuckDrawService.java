package service;

import controller.RedisConst;
import controller.redis.PrizeResult;
import dao.mapper.DrawLogMapper;
import dao.mapper.PrizeMapper;
import dao.model.DrawLog;
import dao.model.DrawLogExample;
import dao.model.Prize;
import dao.model.PrizeExample;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
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

    @Autowired
    private JedisPool jedisPool;

    private Logger logger=Logger.getLogger(this.getClass());

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

    /**
     * 读取数据到redis
     */
    public void initRedis(){
        logger.info("init data from to redis");
        List<Prize> prizes=queryAll();
        long start=System.currentTimeMillis();
        Jedis jedis=null;
        jedis = jedisPool.getResource();
        //定义存放到redis规则
        for (int i=0;i<prizes.size();i++){
            String prize= RedisConst.PRIZEPREX+(i+1);   //i等奖
            int size=prizes.get(i).getPrizeSize();  //数量
            String result=jedis.set(prize,size+"");
            logger.info(result);
            String prizeVersion=RedisConst.VERSIONPREX+(i+1);   //i版本号
            String initVersion="0";
            jedis.set(prizeVersion,initVersion);
        }
        jedis.close();
        long end=System.currentTimeMillis();
        logger.info("初始化数据耗时:"+(end-start)+"ms");
    }

    /**
     * 统计当前的数量
     */
    public PrizeResult queryAllByRedis(){
        PrizeResult result=new PrizeResult();
        Jedis jedis=null;
        try {
            jedis=jedisPool.getResource();
            int firstSize=Integer.valueOf(jedis.get("prize_size_1"));
            result.setFirstSize(firstSize);
            int secondSize=Integer.valueOf(jedis.get("prize_size_2"));
            result.setSecondSize(secondSize);
            int thirdSize=Integer.valueOf(jedis.get("prize_size_3"));
            result.setThirdSize(thirdSize);
            int thankSize=Integer.valueOf(jedis.get("prize_size_4"));
            result.setThankSize(thankSize);
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        return result;
    }

    /**
     * 采用乐观锁
     * 1.
     */
    public void luckDrawByRedis(){
        Jedis jedis=null;
        try {
            jedis = jedisPool.getResource();
            PrizeResult result=queryAllByRedis();
            int sum=result.getFirstSize()+result.getSecondSize()+result.getThirdSize()+result.getThankSize();
            Random random=new Random();
            int luckNum=random.nextInt(sum+1);
            if (true){  //一等奖

                jedis.watch(RedisConst.FIRSTVERSION,RedisConst.FIRSTPRIZE);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.incr(RedisConst.FIRSTVERSION);    //版本号增加
                transac.decr(RedisConst.FIRSTPRIZE);    //减少first
                List<Object> list=transac.exec();
                if (list!=null) {
                    Long i = (Long) list.get(1);
                    if (i<0){   //利用回调防止多次抢空
                        throw new NullPointerException("一等奖已被抢空");
                    }
                }
            }else if (luckNum-(result.getFirstSize()+result.getSecondSize())<=0){
                if(result.getSecondSize()==0) {
                    throw new NullPointerException("二等奖已被抢购完");
                }
                jedis.watch(RedisConst.SECONDVERSION);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.incr(RedisConst.SECONDVERSION);    //版本号增加
                transac.decr(RedisConst.SECONDPRIZE);    //减少first
                List<Object> list=transac.exec();
            }else if (luckNum-(result.getFirstSize()+result.getSecondSize()+result.getThirdSize())<=0){
                if(result.getSecondSize()==0) {
                    throw new NullPointerException("三等奖已被抢购完");
                }
                jedis.watch(RedisConst.THIRDVERSION);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.incr(RedisConst.THIRDVERSION);    //版本号增加
                transac.decr(RedisConst.THIRDPRIZE);    //减少first
                List<Object> list=transac.exec();
            }else {
                if (result.getThankSize()==0){
                    throw new NullPointerException("感谢奖已被抢购完");
                }
                jedis.watch(RedisConst.THANKSVERSION);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.incr(RedisConst.THANKSVERSION);    //版本号增加
                transac.decr(RedisConst.THANKSPRIZE);    //减少first
                List<Object> list=transac.exec();
                logger.info(list);
            }
        }catch (Throwable e){
            logger.error(e.getMessage());
        }finally {
            if (jedis!=null){
            jedis.close();
            }
        }

    }



}
