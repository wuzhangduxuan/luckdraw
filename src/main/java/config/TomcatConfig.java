package config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/11/7.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public EmbeddedServletContainerFactory createEmbeddedServletContainerFactory(){
        TomcatEmbeddedServletContainerFactory tomcatFactory=
                new TomcatEmbeddedServletContainerFactory();
        tomcatFactory.setPort(8081);
        tomcatFactory.addConnectorCustomizers(new MyTomcatConnectorCustomizer());
        return tomcatFactory;
    }

    class MyTomcatConnectorCustomizer implements TomcatConnectorCustomizer{
        @Override
        public void customize(Connector connector) {
            Http11NioProtocol protocol= (Http11NioProtocol) connector.getProtocolHandler();
            protocol.setMaxConnections(600);
            protocol.setMinSpareThreads(100);
            protocol.setMaxThreads(600);
            protocol.setConnectionTimeout(3000);
        }
    }

}
