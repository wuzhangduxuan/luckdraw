import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import service.LuckDrawService;

/**
 * Created by 吴樟 on www.haixiangzhene.xyz
 * 2017/9/28.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = LuckDrawController.class)
@WebAppConfiguration
public class SpringTest {

    @Autowired
    private LuckDrawService luckDrawService;



    @Test
    public void test() throws Exception {

    }



}
