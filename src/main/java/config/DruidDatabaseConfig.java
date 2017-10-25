package config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import controller.SystemConst;
import controller.result.ResultBean;
import org.apache.log4j.Logger;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import service.LuckDrawService;
import util.DateUtil;
import util.IPUtil;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/10/18.
 */
@Configuration
public class DruidDatabaseConfig  extends WebMvcConfigurerAdapter {



    @Value("${spring.datasource.filters}")
    private String filters;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.initialSize}")
    private String initialSize;

    @Value("${spring.datasource.minIdle}")
    private String minIdle;

    @Value("${spring.datasource.maxActive}")
    private String maxActive;

    @Value("${spring.datasource.maxWait}")
    private String maxWait;

    @Autowired
    private LuckDrawService luckDrawService;

    private Logger logger=Logger.getLogger(this.getClass());

    @Bean
    @Primary
    public DataSource druidDataSource(){
        DruidDataSource datasource = new DruidDataSource();
        datasource.setUrl(dbUrl);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setDriverClassName(driverClassName);
        datasource.setInitialSize(Integer.valueOf(initialSize));
        datasource.setMinIdle(Integer.valueOf(minIdle));
        datasource.setMaxActive(Integer.valueOf(maxActive));
        datasource.setMaxWait(Long.valueOf(maxWait));
        try {
            datasource.setFilters(filters);
        } catch (SQLException e) {
            logger.error("druid configuration initialization filter", e);
        }
        return datasource;
    }

    @Bean
    public ServletRegistrationBean druidServlet() {
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
        servletRegistrationBean.setServlet(new StatViewServlet());
        servletRegistrationBean.addUrlMappings("/druid/*");
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("loginUsername", "druid");// 用户名
        initParameters.put("loginPassword", "druid");// 密码
        initParameters.put("resetEnable", "false");// 禁用HTML页面上的“Reset All”功能
        initParameters.put("allow", "127.0.0.1"); // IP白名单 (没有配置或者为空，则允许所有访问)
        servletRegistrationBean.setInitParameters(initParameters);
        return servletRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.bmp,*.png,*.css,*.ico,/druid/*");
        return filterRegistrationBean;
    }

    @Bean(value = "druid-stat-interceptor")
    public DruidStatInterceptor DruidStatInterceptor() {
        DruidStatInterceptor druidStatInterceptor = new DruidStatInterceptor();
        return druidStatInterceptor;
    }



    @Bean
    public BeanNameAutoProxyCreator beanNameAutoProxyCreator() {
        BeanNameAutoProxyCreator beanNameAutoProxyCreator = new BeanNameAutoProxyCreator();
        beanNameAutoProxyCreator.setProxyTargetClass(true);
        // 设置要监控的bean的id
        beanNameAutoProxyCreator.setBeanNames("drawLogMapper","prizeMapper");
        beanNameAutoProxyCreator.setInterceptorNames("druid-stat-interceptor");
        return beanNameAutoProxyCreator;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptorAdapter(){
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                //判断距离开奖还有多少时间
                Date now=new Date();
                Date start=SystemConst.startLuckDraw;
                if (!DateUtil.isExpired(start,now)){    //还未到达预定时间
                    ResultBean<String> resultBean=new ResultBean<>();
                    resultBean.setCode(-2);
                    resultBean.setMessage("暂时还没有开放抽奖");
                    String resultMessage=JSON.toJSONString(resultBean);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(resultMessage);
                    response.getWriter().flush();
                    return false;
                }
                String userIp= IPUtil.getIpAddr(request);
               // boolean result=luckDrawService.isIpDrawed(userIp); mysql版
                //redis版本
                boolean result=luckDrawService.NotInPrize(userIp);
                logger.info("the ip "+userIp+"===="+result);
                return result;
            }
        }).addPathPatterns("/luckDraw");    //redis  //mysql /panic
    }
}
