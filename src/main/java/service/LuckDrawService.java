package service;

import controller.RedisConst;
import controller.ResultConst;
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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    @PostConstruct
    public void initRedis(){
        logger.info("init data from to redis");
        List<Prize> prizes=queryAll();
        long start=System.currentTimeMillis();
        Jedis jedis=null;
        jedis = jedisPool.getResource();
        jedis.flushAll();   //清空数据,防止冲突
        //定义存放到redis规则
        for (int i=0;i<prizes.size();i++){
            String prize= RedisConst.PRIZEPREX+(i+1);   //i等奖
            int size=prizes.get(i).getPrizeSize();  //数量
            String result=jedis.set(prize,size+"");
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
     * 采用乐观锁,由于本身库存是-1操作,所以可以默认把库存看作是版本号
     * 采用redis回调回来的数据判断是否已经空了，防止扣除
     */
    public void luckDrawByRedis(HttpServletRequest request){
        Jedis jedis=null;
        try {
            String ip=IPUtil.getIpAddr(request);
            jedis = jedisPool.getResource();
            PrizeResult result=queryAllByRedis();
            int sum=result.getFirstSize()+result.getSecondSize()+result.getThirdSize()+result.getThankSize();
            Random random=new Random();
            int luckNum=random.nextInt(sum+1);
            if (luckNum-result.getFirstSize()<=0){  //一等奖
                if(result.getSecondSize()<=0) {     //降低redis压力
                    throw new NullPointerException("一等奖已被抢购完");
                }
                jedis.watch(RedisConst.FIRSTPRIZESIZE);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.decr(RedisConst.FIRSTPRIZESIZE);    //减少first
                List<Object> list=transac.exec();
                if (list!=null) {
                    Long i = (Long) list.get(0);
                    if (i<0){   //利用回调防止多次抢空
                        throw new NullPointerException("一等奖已被抢空");
                    }
                    addResultToRedis(jedis,ip,RedisConst.FIRSTLEVEL);
                }

            }else if (luckNum-(result.getFirstSize()+result.getSecondSize())<=0){
                if(result.getSecondSize()<=0) {     //
                    throw new NullPointerException("二等奖已被抢购完");
                }
                jedis.watch(RedisConst.SECONDPRIZESIZE);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.decr(RedisConst.SECONDPRIZESIZE);    //减少second
                List<Object> list=transac.exec();
                if (list!=null) {
                    Long i = (Long) list.get(0);
                    if (i<0){   //利用回调防止多次抢空
                        throw new NullPointerException("二等奖已被抢空");
                    }
                    addResultToRedis(jedis,ip,RedisConst.SECONDLEVEL);
                }

            }else if (luckNum-(result.getFirstSize()+result.getSecondSize()+result.getThirdSize())<=0){
                if(result.getSecondSize()<=0) {
                    throw new NullPointerException("三等奖已被抢购完");
                }
                jedis.watch(RedisConst.THIRDPRIZESIZE);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.decr(RedisConst.THIRDPRIZESIZE);    //减少first
                List<Object> list=transac.exec();
                if (list!=null) {
                    Long i = (Long) list.get(0);
                    if (i<0){   //利用回调防止多次抢空
                        throw new NullPointerException("三等奖已被抢空");
                    }
                    addResultToRedis(jedis,ip,RedisConst.THIRDLEVEL);
                }

            }else {
                if (result.getThankSize()<=0){
                    throw new NullPointerException("感谢奖已被抢购完");
                }
                jedis.watch(RedisConst.THANKSPRIZESIZE);   //监听一等奖版本号
                Transaction transac=jedis.multi();  //开启事务
                transac.decr(RedisConst.THANKSPRIZESIZE);    //减少first
                List<Object> list=transac.exec();
                if (list!=null) {
                    Long i = (Long) list.get(0);
                    if (i<0){   //利用回调防止多次抢空
                        throw new NullPointerException("感谢奖已被抢空");
                    }
                    addResultToRedis(jedis,ip, RedisConst.THANKSLEVEL);
                }

            }
        }catch (Throwable e){
            logger.error(e.getMessage());
        }finally {
            if (jedis!=null){
                jedis.close();
            }
        }
    }

    /**
     * 1.存入prize_level : {ip..} 用于获取一等奖获取者
     * 2.存放ip : prize_level
     */
    public void addResultToRedis(Jedis jedis,String ip,String prizeLevel){
        jedis.set(ip,prizeLevel);   //方便查询黑色ip
        jedis.sadd(prizeLevel,ip);  //方便查询获奖用户
    }


    /**
     * 查询获奖用户
     */
    public Set<String> getWinPrize(String prizeLevel){
        Jedis jedis=null;
        Set<String> userIps= null;
        try {
            jedis=jedisPool.getResource();
            userIps=jedis.smembers(prizeLevel);
        }catch (Throwable e){
            logger.error("it throw an exception"+e.getMessage());
        }finally {
            if (jedis!=null)
                jedis.close();
        }
        return userIps;
    }

    /**
     * 查询已获奖用户
     */
    public boolean NotInPrize(String ip){
        Jedis jedis=null;
        boolean result=true;
        try {
            jedis=jedisPool.getResource();
            String haved=jedis.get(ip);
            if (haved!=null)
                result=false;
            return result;
        }catch (Throwable e){
            logger.error(e.getMessage());
        }finally {
            if (jedis!=null)
                jedis.close();
        }
        return result;
    }


}
